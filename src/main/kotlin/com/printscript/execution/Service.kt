package com.printscript.execution

import com.printscript.execution.dto.FormatReq
import com.printscript.execution.dto.FormatRes
import com.printscript.execution.dto.LintReq
import com.printscript.execution.dto.LintRes
import com.printscript.execution.dto.ParseReq
import com.printscript.execution.dto.ParseRes
import com.printscript.execution.dto.RunReq
import com.printscript.execution.dto.RunRes

interface Service {
    fun parse(req: ParseReq): ParseRes
    fun lint(req: LintReq): LintRes
    fun execute(req: RunReq): RunRes
    fun formatContent(req: FormatReq): FormatRes
}
