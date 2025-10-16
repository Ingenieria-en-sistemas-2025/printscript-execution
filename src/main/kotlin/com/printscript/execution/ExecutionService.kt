package com.printscript.execution

import com.printscript.execution.dto.RunReq
import com.printscript.execution.dto.RunRes

interface ExecutionService {
//    fun parse(req: ParseReq): ParseRes
//    fun lint(req: LintReq): LintRes
    fun execute(req: RunReq): RunRes
//    fun formatContent(req: FormatReq): FormatRes
}
