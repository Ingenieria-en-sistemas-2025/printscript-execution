package com.printscript.execution.logs

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlin.test.Test

class RequestLogFilterTest {

    private val filter = RequestLogFilter()

    @Test
    fun `doFilterInternal loguea request y sigue la cadena`() {
        val req = mockk<HttpServletRequest>()
        val res = mockk<HttpServletResponse>()
        val chain = mockk<FilterChain>()

        every { req.method } returns "GET"
        every { req.requestURI } returns "/ping"
        every { req.queryString } returns null
        every { res.status } returns 200
        justRun { chain.doFilter(req, res) }

        filter.doFilterInternal(req, res, chain)
    }
}
