package com.printscript.execution.utils

import io.printscript.contracts.DiagnosticDto
import org.printscript.analyzer.Diagnostic
import org.printscript.analyzer.Severity
import org.printscript.common.Position
import org.printscript.common.Span
import kotlin.test.Test
import kotlin.test.assertEquals

class DiagnosticMappingUtilsTest {

    private fun span() = Span(
        start = Position(2, 4),
        end = Position(2, 10),
    )

    @Test
    fun `diagToDiagnosticDto copia bien los campos`() {
        val d = Diagnostic(
            ruleId = "R1",
            message = "msg",
            span = span(),
            severity = Severity.ERROR,
        )

        val dto: DiagnosticDto = diagToDiagnosticDto(d)

        assertEquals("R1", dto.ruleId)
        assertEquals("msg", dto.message)
        assertEquals(2, dto.line)
        assertEquals(4, dto.col)
    }
}
