package com.printscript.execution.error

import org.printscript.common.LabeledError

object ErrorMapping {
    fun toApiDiagnostic(le: LabeledError, code: String = "PS"): ApiDiagnostic = ApiDiagnostic(
        code = code,
        message = le.message,
        line = le.span.start.line,
        column = le.span.start.column,
    )
}
