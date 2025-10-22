package com.printscript.execution.service

import com.printscript.execution.dto.FormatReq
import com.printscript.execution.dto.FormatRes
import com.printscript.execution.dto.LintReq
import com.printscript.execution.dto.LintRes
import com.printscript.execution.dto.ParseReq
import com.printscript.execution.dto.ParseRes
import com.printscript.execution.dto.RunReq
import com.printscript.execution.dto.RunRes
import com.printscript.execution.dto.RunTestsReq
import com.printscript.execution.dto.RunTestsRes

interface ExecutionService {
    fun parse(req: ParseReq): ParseRes
    fun lint(req: LintReq): LintRes
    fun execute(req: RunReq): RunRes
    fun formatContent(req: FormatReq): FormatRes
    fun runTests(req: RunTestsReq): RunTestsRes
}
