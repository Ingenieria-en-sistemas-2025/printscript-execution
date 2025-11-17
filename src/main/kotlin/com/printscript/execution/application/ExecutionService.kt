package com.printscript.execution.application

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

interface ExecutionService {
    fun parse(req: ParseReq): ParseRes
    fun lint(req: LintReq): LintRes
    fun execute(req: RunReq): RunRes
    fun formatContent(req: FormatReq): FormatRes
    fun runTests(req: RunTestsReq): RunTestsRes
    fun runSingleTest(req: RunSingleTestReq): RunSingleTestRes
}
