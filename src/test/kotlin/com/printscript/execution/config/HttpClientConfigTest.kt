package com.printscript.execution.config

import com.printscript.execution.auth.Auth0TokenService
import com.printscript.execution.auth.HttpClientConfig
import com.printscript.execution.logs.CorrelationIdFilter.Companion.CORRELATION_ID_HEADER
import com.printscript.execution.logs.CorrelationIdFilter.Companion.CORRELATION_ID_KEY
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.support.HttpRequestWrapper
import org.springframework.mock.http.client.MockClientHttpRequest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpClientConfigTest {

    @BeforeTest
    fun setup() {
        MDC.put(CORRELATION_ID_KEY, "corr-test-123")
    }

    @AfterTest
    fun clean() {
        MDC.clear()
    }

    @Test
    fun `m2mRestTemplate agrega Authorization y X-Correlation-Id`() {
        val tokenService = mockk<Auth0TokenService>()
        every { tokenService.getAccessToken() } returns "test-token"

        val config = HttpClientConfig()
        val template = config.m2mRestTemplate(tokenService)

        assertEquals(2, template.interceptors.size)

        val underlying = MockClientHttpRequest(HttpMethod.POST, "/internal/test")
        val wrapper = HttpRequestWrapper(underlying)
        val body = ByteArray(0)

        var executed = false
        val execution = ClientHttpRequestExecution { req, reqBody ->
            executed = true
            mockk<ClientHttpResponse>(relaxed = true)
        }

        template.interceptors.forEach { interceptor ->
            interceptor.intercept(wrapper, body, execution)
        }

        verify(exactly = 1) { tokenService.getAccessToken() }

        val authHeader = underlying.headers.getFirst(HttpHeaders.AUTHORIZATION)
        assertEquals("Bearer test-token", authHeader)

        val corrHeader = underlying.headers.getFirst(CORRELATION_ID_HEADER)
        assertEquals("corr-test-123", corrHeader)

        assertTrue(executed)
    }
}
