package com.printscript.execution.config

import com.printscript.execution.auth.Auth0TokenService
import com.printscript.execution.auth.HttpClientConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.support.HttpRequestWrapper
import org.springframework.mock.http.client.MockClientHttpRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpClientConfigTest {

    @Test
    fun `m2mRestTemplate agrega Authorization Bearer con token de Auth0TokenService`() {
        val tokenService = mockk<Auth0TokenService>()
        every { tokenService.getAccessToken() } returns "test-token"

        val config = HttpClientConfig()
        val template = config.m2mRestTemplate(tokenService)

        // un solo interceptor: el authInterceptor
        val interceptor: ClientHttpRequestInterceptor = template.interceptors.single()

        val underlying = MockClientHttpRequest(HttpMethod.GET, "/test")
        val wrapper = HttpRequestWrapper(underlying)
        val body = ByteArray(0)

        var executed = false
        val execution = ClientHttpRequestExecution { req, reqBody ->
            executed = true
            mockk<ClientHttpResponse>(relaxed = true)
        }

        interceptor.intercept(wrapper, body, execution)

        verify(exactly = 1) { tokenService.getAccessToken() }

        val authHeader = underlying.headers.getFirst(HttpHeaders.AUTHORIZATION)
        assertEquals("Bearer test-token", authHeader)

        assertTrue(executed)
    }
}
