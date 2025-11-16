package com.printscript.execution.utils

import org.junit.jupiter.api.Assertions.assertTrue
import org.printscript.analyzer.Diagnostic
import org.printscript.analyzer.Severity
import org.printscript.common.Position
import org.printscript.common.Span
import kotlin.test.Test
import kotlin.test.assertEquals

class DiagnosticCollectorTest {

    private fun diag(severity: Severity) = Diagnostic(
        ruleId = "R1",
        message = "msg",
        span = Span(
            start = Position(1, 1),
            end = Position(1, 2),
        ),
        severity = severity,
    )

    @Test
    fun `collector acumula diagnostics`() {
        val collector = DiagnosticCollector()
        val d1 = diag(Severity.ERROR)
        val d2 = diag(Severity.WARNING)

        collector.report(d1)
        collector.report(d2)

        val diags = collector.diagnostics
        assertEquals(2, diags.size)
        assertEquals(d1, diags[0])
        assertEquals(d2, diags[1])
    }

    @Test
    fun `hasErrors detecta si hay al menos un ERROR`() {
        val list = listOf(diag(Severity.WARNING), diag(Severity.ERROR))

        assertTrue(list.hasErrors())
    }

    @Test
    fun `onlyWarnings filtra solo WARNINGS`() {
        val list = listOf(
            diag(Severity.WARNING),
            diag(Severity.ERROR),
            diag(Severity.WARNING),
        )

        val warnings = list.onlyWarnings()

        assertEquals(2, warnings.size)
        assertTrue(warnings.all { it.severity == Severity.WARNING })
    }
}
