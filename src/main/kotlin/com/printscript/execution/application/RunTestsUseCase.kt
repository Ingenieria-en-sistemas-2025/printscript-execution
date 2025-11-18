package com.printscript.execution.application

import com.printscript.execution.domain.RunTestsReq
import com.printscript.execution.domain.RunTestsRes
import com.printscript.execution.domain.SingleTestResultDto
import com.printscript.execution.domain.SummaryDto
import com.printscript.execution.domain.diagnostics.ExecException
import com.printscript.execution.domain.language.LanguageRunnerPort
import com.printscript.execution.domain.language.LanguageRunnerRegistry
import io.printscript.contracts.DiagnosticDto
import io.printscript.contracts.run.RunReq
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private const val PASS = "PASS"
private const val FAIL = "FAIL"
private const val ERROR = "ERROR"

@Service
class RunTestsUseCase(private val runners: LanguageRunnerRegistry, private val runUseCase: RunUseCase) {
    private val logger = LoggerFactory.getLogger(RunTestsUseCase::class.java)

    fun runTests(req: RunTestsReq): RunTestsRes {
        logger.info(
            "RunTestsUseCase.runTests: cases={} lang={} version={}",
            req.testCases.size,
            req.language,
            req.version,
        )

        val runner = runners.runnerFor(req.language)
        precheckSyntaxOrNull(runner, req)?.let { allErrors ->
            logger.warn("Test run aborted due to initial syntax check failure.")
            return RunTestsRes(
                summary = SummaryDto(total = allErrors.size, passed = 0),
                results = allErrors,
            )
        }

        val results = req.testCases.indices.map { idx -> runOneTestCase(req, idx) }
        val passed = results.count { it.status == PASS }

        logger.info(
            "RunTestsUseCase.runTests: completed total={} passed={} failedOrError={}",
            results.size,
            passed,
            results.size - passed,
        )

        return RunTestsRes(
            summary = SummaryDto(total = results.size, passed = passed),
            results = results,
        )
    }

    private fun precheckSyntaxOrNull(runner: LanguageRunnerPort, req: RunTestsReq): List<SingleTestResultDto>? {
        val diags: List<DiagnosticDto> = runner.validate(
            language = req.language,
            version = req.version,
            content = req.content,
        )

        if (diags.isEmpty()) return null

        val diag = diags.first()

        return req.testCases.mapIndexed { i, _ ->
            SingleTestResultDto(
                index = i,
                status = ERROR,
                reason = diag.message,
                diagnostic = diag,
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
            val runRes = runUseCase.execute(
                RunReq(
                    language = req.language,
                    version = req.version,
                    content = req.content,
                    inputs = testCase.inputs,
                ),
            )

            val (status, mismatchAt) = evaluateOutputs(
                expected = testCase.expectedOutputs,
                actual = runRes.outputs,
            )

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
                diagnostic = ex.diagnostic?.let { d ->
                    DiagnosticDto(d.code, d.message, d.line, d.column)
                },
            )
        }
    }
}
