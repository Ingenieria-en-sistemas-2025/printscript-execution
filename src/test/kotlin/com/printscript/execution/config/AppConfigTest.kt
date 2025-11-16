package com.printscript.execution.config

import com.printscript.execution.auth.Auth0TokenService
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertNotNull

class AppConfigTest {

    @Test
    fun `plainRestClient se crea correctamente`() {
        val dummyTokenService = mockk<Auth0TokenService>()
        val config = AppConfig(dummyTokenService)

        val client = config.plainRestClient()

        assertNotNull(client)
    }

    @Test
    fun `secureRestClient se crea correctamente`() {
        val tokenService = mockk<Auth0TokenService>()
        every { tokenService.getAccessToken() } returns "token"

        val config = AppConfig(tokenService)

        val client = config.secureRestClient()

        assertNotNull(client)
    }
}
