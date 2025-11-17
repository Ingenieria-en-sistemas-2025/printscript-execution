package com.printscript.execution.errors

import com.printscript.execution.domain.diagnostics.ApiDiagnostic
import com.printscript.execution.domain.diagnostics.ExecException
import com.printscript.execution.web.RestErrors
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class RestErrorsTest {

    private val handler = RestErrors()

    @Test
    fun `badReq mapea IllegalArgumentException a 400`() {
        val res = handler.badReq(IllegalArgumentException("bad"))
        assertEquals(400, res.statusCode.value())
        assertEquals("bad", res.body?.error)
    }

    @Test
    fun `exec mapea ExecException a 422 con diagnostic`() {
        val diag = ApiDiagnostic("CODE", "msg", 1, 2)
        val ex = ExecException(diagnostic = diag, msg = "boom")

        val res = handler.exec(ex)

        assertEquals(422, res.statusCode.value())
        assertEquals("boom", res.body?.error)
        assertEquals("CODE", res.body?.diagnostic?.code)
        assertEquals(1, res.body?.diagnostic?.line)
        assertEquals(2, res.body?.diagnostic?.column)
    }

    @Test
    fun `boom mapea Exception generica a 500`() {
        val res = handler.boom(Exception("x"))
        assertEquals(500, res.statusCode.value())
        assertEquals("x", res.body?.error)
    }
}
