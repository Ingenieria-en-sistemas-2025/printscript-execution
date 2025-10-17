package com.printscript.execution.utils

import org.printscript.analyzer.Diagnostic
import org.printscript.analyzer.DiagnosticEmitter
import org.printscript.analyzer.Severity


class DiagnosticCollector : DiagnosticEmitter {
    private val list = mutableListOf<Diagnostic>()
    val diagnostics: List<Diagnostic> get() = list.toList()

    override fun report(diagnostic: Diagnostic) {
        list += diagnostic
    }
}

internal fun List<Diagnostic>.hasErrors(): Boolean =
    any { it.severity == Severity.ERROR }

internal fun List<Diagnostic>.onlyWarnings(): List<Diagnostic> =
    filter { it.severity == Severity.WARNING }