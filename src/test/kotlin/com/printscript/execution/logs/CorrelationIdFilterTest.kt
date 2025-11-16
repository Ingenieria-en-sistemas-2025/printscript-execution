package com.printscript.execution.logs

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import kotlin.test.Test
import kotlin.test.assertNull

class CorrelationIdFilterTest {

    private val filter = CorrelationIdFilter()

    @Test
    fun `si viene X-Correlation-Id se reutiliza y se setea en header y MDC`() {
        val req = mockk<HttpServletRequest>() // request http que recibe el filtro
        val res = mockk<HttpServletResponse>() // la respuesta HTTP que el filtro escribe
        val chain = mockk<FilterChain>() // la cadena de filtros que se ejecuta después

        every { req.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER) } returns "corr-123"
        every { res.setHeader(any(), any()) } just Runs
        every { chain.doFilter(req, res) } just Runs

        filter.doFilterInternal(req, res, chain)

        verify { res.setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-123") }
        // Al final del filtro debería limpiarse
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY))
    }

    @Test
    fun `si no viene header se genera uno nuevo y se setea igual`() {
        val req = mockk<HttpServletRequest>()
        val res = mockk<HttpServletResponse>()
        val chain = mockk<FilterChain>()

        every { req.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER) } returns null
        every { res.setHeader(any(), any()) } just Runs
        every { chain.doFilter(req, res) } just Runs

        filter.doFilterInternal(req, res, chain)

        verify {
            res.setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), any())
        }
    }
}
