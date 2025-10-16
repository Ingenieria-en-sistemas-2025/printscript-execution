package com.printscript.execution

import com.printscript.execution.dto.RunReq
import com.printscript.execution.dto.RunRes
import com.printscript.execution.error.ErrorMapping
import com.printscript.execution.error.ExecException
import org.printscript.ast.StatementStream
import org.printscript.ast.Step
import org.printscript.cli.CliSupport
import org.printscript.common.Failure
import org.printscript.common.LabeledError
import org.printscript.common.Success
import org.printscript.common.Version
import org.printscript.interpreter.Interpreter
import org.printscript.runner.LanguageWiringFactory
import org.printscript.runner.ProgramIo
import org.springframework.stereotype.Service

@Service
class ExecutionServiceImpl : ExecutionService {

    // helpers
    private fun parseVersion(version: String): Version = CliSupport.resolveVersion(version)

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

    private fun firstParseError(statements: StatementStream): LabeledError? {
        tailrec fun loop(cur: StatementStream): LabeledError? = when (val step = cur.nextStep()) {
            is Step.Item -> loop(step.next)
            is Step.Error -> step.error
            is Step.Eof -> null
        }
        return loop(statements)
    }

//    override fun parse(req: ParseReq): ParseRes {
//
//
//
//    }
//
//    override fun lint(req: LintReq): LintRes {
//
//    }

    override fun execute(req: RunReq): RunRes {
        requirePrintScript(req.language)

        val interpreterWithStatements = getInterpreterWithStatements(req)
        firstParseError(interpreterWithStatements.statements)?.let { le ->
            throw ExecException(
                diagnostic = ErrorMapping.toApiDiagnostic(le, code = "PS-SYNTAX"),
                msg = le.message,
            )
        }
        val interpreterWithStatements2 = getInterpreterWithStatements(req) // reconstruyo porque fue consumido

        return when (val result = interpreterWithStatements2.interpreter.run(interpreterWithStatements2.statements)) {
            is Success -> {
                RunRes(outputs = result.value.outputs)
            }
            is Failure -> {
                val le: LabeledError = result.error
                throw ExecException(
                    diagnostic = ErrorMapping.toApiDiagnostic(le, "PS-RUN"),
                    msg = le.message,
                )
            }
        }
    }

//    override fun formatContent(req: FormatReq): FormatRes {
//
//    }

    private data class InterpreterWithStatements(val interpreter: Interpreter, val statements: StatementStream)
}
