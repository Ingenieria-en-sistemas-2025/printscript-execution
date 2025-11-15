package com.printscript.execution.service

import com.printscript.execution.dto.RunTestsReq
import com.printscript.execution.dto.RunTestsRes
import com.printscript.execution.dto.SingleTestResultDto
import com.printscript.execution.dto.SummaryDto
import com.printscript.execution.error.ApiDiagnostic
import com.printscript.execution.error.ErrorMapping
import com.printscript.execution.error.ExecException
import com.printscript.execution.utils.AnalyzerConfigResolver.resolveAnalyzerConfig
import com.printscript.execution.utils.DiagnosticCollector
import com.printscript.execution.utils.FormatterOptionsResolver
import com.printscript.execution.utils.QueueInputProvider
import com.printscript.execution.utils.diagToDiagnosticDto
import com.printscript.execution.utils.errorToDto
import io.printscript.contracts.DiagnosticDto
import io.printscript.contracts.Request
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.parse.ParseRes
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import org.printscript.ast.StatementStream
import org.printscript.ast.Step
import org.printscript.cli.CliSupport
import org.printscript.common.Failure
import org.printscript.common.LabeledError
import org.printscript.common.Success
import org.printscript.common.Version
import org.printscript.formatter.factories.GlobalFormatterFactory
import org.printscript.interpreter.Interpreter
import org.printscript.lexer.config.LexerFactory
import org.printscript.runner.LanguageWiring
import org.printscript.runner.LanguageWiringFactory
import org.printscript.runner.ProgramIo
import org.printscript.runner.helpers.VersionMapper
import org.printscript.token.TokenStream
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private const val PASS = "PASS"
private const val FAIL = "FAIL"
private const val ERROR = "ERROR"
private const val MAX_STEPS = 10000

@Service
class ExecutionServiceImpl : ExecutionService {

    private val logger = LoggerFactory.getLogger(ExecutionServiceImpl::class.java)

    // helpers
    private fun parseVersion(version: String): Version = VersionMapper.parse(version)

    private fun requirePrintScript(language: String) {
        require(language.equals("printscript", ignoreCase = true)) {
            "Only PrintScript is supported for now"
        }
    }

    private fun getInterpreterWithStatements(req: RunReq): InterpreterWithStatements {
        val version = parseVersion(req.version)
        val input = QueueInputProvider(req.inputs ?: emptyList())
        val wiring = LanguageWiringFactory.forVersion(version)
        val io = ProgramIo(req.content, inputProviderOverride = input)
        val reader = io.openReader()
        val tokenStream = wiring.tokenStreamFromReader(reader)
        val statements = wiring.statementStreamFromTokens(tokenStream)
        val interpreter = wiring.interpreterFor(input)
        return InterpreterWithStatements(interpreter, statements)
    }

    private fun getWiringInfo(req: Request): WiringInfo {
        val version = parseVersion(req.version)
        val wiring = LanguageWiringFactory.forVersion(version)
        val io = ProgramIo(req.content)
        val reader = io.openReader()
        val tokenStream = wiring.tokenStreamFromReader(reader)
        val statements = wiring.statementStreamFromTokens(tokenStream)
        return WiringInfo(version, wiring, tokenStream, statements)
    }

    private fun firstParseError(statements: StatementStream): LabeledError? {
        tailrec fun loop(cur: StatementStream): LabeledError? = when (val step = cur.nextStep()) {
            is Step.Item -> loop(step.next)
            is Step.Error -> step.error
            is Step.Eof -> null
        }
        return loop(statements)
    }

    override fun parse(req: ParseReq): ParseRes {
        logger.info("Request received for parsing content (Language: ${req.language}, Version: ${req.version})")
        requirePrintScript(req.language)
        val wiringInfo = getWiringInfo(req)
        val diags = mutableListOf<DiagnosticDto>()
        tailrec fun loop(cur: StatementStream, steps: Int = 0) {
            if (steps > MAX_STEPS) {
                throw ExecException(
                    diagnostic = ApiDiagnostic("PS-PARSE", "Parser no progresa (posible loop de recuperación)", 1, 1),
                    msg = "Parser no progresa",
                )
            }
            when (val step = cur.nextStep()) {
                is Step.Item -> loop(step.next, steps + 1)
                is Step.Error -> {
                    diags += errorToDto(step.error)
                    loop(step.next, steps + 1)
                }
                is Step.Eof -> return
            }
        }
        loop(wiringInfo.statementStream)
        val valid = diags.isEmpty()
        logger.info("Parsing complete. Valid: $valid, Diagnostics count: ${diags.size}")
        return ParseRes(valid = diags.isEmpty(), diagnostics = diags)
    }

    override fun lint(req: LintReq): LintRes {
        logger.info("Request received for linting content (Language: ${req.language}, Version: ${req.version})")
        requirePrintScript(req.language)
        val wiringInfo = getWiringInfo(req)
        val cfg = resolveAnalyzerConfig(req)
        logger.debug("Lint configuration resolved: $cfg")

        val collector = DiagnosticCollector()
        return when (val lint = wiringInfo.wiring.analyzer.analyze(wiringInfo.statementStream, cfg, collector)) {
            is Success -> {
                logger.info("Lint successful. Violations found: ${collector.diagnostics.size}")
                LintRes(violations = collector.diagnostics.map { diagToDiagnosticDto(it) })
            }
            is Failure -> {
                logger.error("Lint failed: ${lint.error.message}")
                throw ExecException(
                    diagnostic = ErrorMapping.toApiDiagnostic(lint.error, "PS-LINT"),
                    msg = lint.error.message,
                )
            }
        }
    }

    override fun execute(req: RunReq): RunRes {
        logger.info("Request received for code execution (Version: ${req.version}, Inputs: ${req.inputs?.size ?: 0})")
        requirePrintScript(req.language)

        val interpreterWithStatements = getInterpreterWithStatements(req)
        firstParseError(interpreterWithStatements.statements)?.let { le ->
            logger.error("Execution failed due to syntax error: ${le.message}")
            throw ExecException(
                diagnostic = ErrorMapping.toApiDiagnostic(le, code = "PS-SYNTAX"),
                msg = le.message,
            )
        }
        val interpreterWithStatements2 = getInterpreterWithStatements(req) // reconstruyo porque fue consumido

        return when (val result = interpreterWithStatements2.interpreter.run(interpreterWithStatements2.statements)) {
            is Success -> {
                logger.info("Code execution finished successfully.")
                RunRes(outputs = result.value.outputs)
            }
            is Failure -> {
                val le: LabeledError = result.error
                logger.error("Runtime execution failed: ${le.message}")
                throw ExecException(
                    diagnostic = ErrorMapping.toApiDiagnostic(le, "PS-RUN"),
                    msg = le.message,
                )
            }
        }
    }

    override fun formatContent(req: FormatReq): FormatRes { // ver de modularizar aca
        logger.info("Request received for formatting (Version: ${req.version})")
        requirePrintScript(req.language)
        val version = parseVersion(req.version)
        val options = FormatterOptionsResolver.resolve(req)
        val lexerFactory = LexerFactory()
        val tokenStream: TokenStream = lexerFactory.tokenStream(version, req.content, true) // emit trivia
        val formatter = GlobalFormatterFactory.forVersion(version, options)
        val out = StringBuilder()

        if (formatter == null) {
            logger.warn("Formatter not available for version ${req.version}. Returning original content.")
            out.append(req.content)
            return FormatRes(out.toString())
        }

        return when (val fmt = formatter.format(tokenStream, out)) {
            is Success -> {
                logger.info("Formatting complete.")
                FormatRes(out.toString())
            }
            is Failure -> {
                val le = fmt.error
                logger.error("Formatting failed: ${le.message}")
                throw ExecException(
                    diagnostic = ErrorMapping.toApiDiagnostic(le, "PS-FORMAT"),
                    msg = le.message,
                )
            }
        }
    }

    private fun precheckSyntaxOrNull(req: RunTestsReq): List<SingleTestResultDto>? {
        val wiringInfo = getWiringInfo(req)
        val syntax = firstParseError(wiringInfo.statementStream) ?: return null

        val diag = ErrorMapping.toApiDiagnostic(syntax, "PS-SYNTAX")
        val err = DiagnosticDto(diag.code, diag.message, diag.line, diag.column)
        return req.testCases.mapIndexed { i, _ ->
            SingleTestResultDto(
                index = i,
                status = ERROR,
                reason = syntax.message,
                diagnostic = err,
            )
        }
    }

    private fun evaluateOutputs(expected: List<String>, actual: List<String>): Pair<String, Int?> {
        val n = minOf(expected.size, actual.size)
        for (i in 0 until n) {
            if (expected[i] != actual[i]) return FAIL to i
        }
        return if (expected.size == actual.size) PASS to null else FAIL to n
    }

    private fun runOneTestCase(req: RunTestsReq, index: Int): SingleTestResultDto {
        val testCase = req.testCases[index]
        return try {
            val runRes = execute(
                RunReq(
                    language = req.language,
                    version = req.version,
                    content = req.content,
                    inputs = testCase.inputs,
                ),
            )
            val (status, mismatchAt) = evaluateOutputs(testCase.expectedOutputs, runRes.outputs)
            SingleTestResultDto(
                index = index,
                status = status,
                expected = testCase.expectedOutputs,
                actual = runRes.outputs,
                mismatchAt = mismatchAt,
            )
        } catch (ex: ExecException) {
            SingleTestResultDto(
                index = index,
                status = ERROR,
                reason = ex.message,
                diagnostic = ex.diagnostic?.let { d -> DiagnosticDto(d.code, d.message, d.line, d.column) },
            )
        }
    }

    override fun runTests(req: RunTestsReq): RunTestsRes {
        logger.info("Request received for running ${req.testCases.size} tests (Version: ${req.version})")
        requirePrintScript(req.language)
        precheckSyntaxOrNull(req)?.let { allErrors ->
            logger.warn("Test run aborted due to initial syntax check failure.")
            return RunTestsRes(
                summary = SummaryDto(total = allErrors.size, passed = 0),
                results = allErrors,
            )
        }
        val results = (req.testCases.indices).map { idx -> runOneTestCase(req, idx) }
        val passed = results.count { it.status == PASS }
        logger.info("Test run complete. Total: ${results.size}, Passed: $passed, Failed/Error: ${results.size - passed}")
        return RunTestsRes(
            summary = SummaryDto(total = results.size, passed = passed),
            results = results,
        )
    }

    private data class InterpreterWithStatements(val interpreter: Interpreter, val statements: StatementStream)
    private data class WiringInfo(val version: Version, val wiring: LanguageWiring, val tokenStream: TokenStream, val statementStream: StatementStream)
}
