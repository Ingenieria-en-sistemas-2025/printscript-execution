package com.printscript.execution.utils

import com.printscript.execution.dto.DiagnosticDto
import org.printscript.analyzer.Diagnostic
import org.printscript.common.LabeledError

fun diagToDiagnosticDto(diagnostic: Diagnostic): DiagnosticDto = DiagnosticDto(
    diagnostic.ruleId,
    diagnostic.message,
    diagnostic.span.start.line,
    diagnostic.span.start.column,
)

fun errorToDto(error: LabeledError, code: String = "PS-SYNTAX"): DiagnosticDto = DiagnosticDto(
    ruleId = code,
    message = error.message,
    line = error.span.start.line,
    col = error.span.start.column,
)
