package com.printscript.execution.domain.diagnostics

import io.printscript.contracts.DiagnosticDto
import org.printscript.analyzer.Diagnostic

fun diagToDiagnosticDto(diagnostic: Diagnostic): DiagnosticDto = DiagnosticDto(
    diagnostic.ruleId,
    diagnostic.message,
    diagnostic.span.start.line,
    diagnostic.span.start.column,
)
