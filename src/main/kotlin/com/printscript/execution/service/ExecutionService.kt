package com.printscript.execution.service

import com.printscript.execution.dto.RunTestsReq
import com.printscript.execution.dto.RunTestsRes
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.parse.ParseRes
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes

interface ExecutionService {
    fun parse(req: ParseReq): ParseRes
    fun lint(req: LintReq): LintRes
    fun execute(req: RunReq): RunRes
    fun formatContent(req: FormatReq): FormatRes
    fun runTests(req: RunTestsReq): RunTestsRes
}
