package com.printscript.execution.application

import com.printscript.execution.domain.RunTestCaseDto
import com.printscript.execution.domain.RunTestsReq
import com.printscript.execution.domain.RunTestsRes
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.parse.ParseRes
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import io.printscript.contracts.tests.RunSingleTestReq
import io.printscript.contracts.tests.RunSingleTestRes
import org.springframework.stereotype.Service

@Service
class ExecutionServiceImpl(private val parseUseCase: ParseUseCase, private val lintUseCase: LintUseCase, private val formatUseCase: FormatUseCase, private val runUseCase: RunUseCase, private val runTestsUseCase: RunTestsUseCase) : ExecutionService {

    override fun parse(req: ParseReq): ParseRes = parseUseCase.parse(req)

    override fun lint(req: LintReq): LintRes = lintUseCase.lint(req)

    override fun execute(req: RunReq): RunRes = runUseCase.execute(req)

    override fun formatContent(req: FormatReq): FormatRes = formatUseCase.format(req)

    override fun runTests(req: RunTestsReq): RunTestsRes = runTestsUseCase.runTests(req)

    override fun runSingleTest(req: RunSingleTestReq): RunSingleTestRes {
        val wrapper = RunTestsReq(
            language = req.language,
            version = req.version,
            content = req.content,
            testCases = listOf(RunTestCaseDto(req.inputs, req.expectedOutputs)),
            options = req.options,
        )
        val res = runTestsUseCase.runTests(wrapper).results.first() // siempre 1
        return RunSingleTestRes(
            status = res.status,
            actual = res.actual,
            mismatchAt = res.mismatchAt,
            diagnostic = res.diagnostic,
        )
    }
}
