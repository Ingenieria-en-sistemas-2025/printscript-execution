package com.printscript.execution.auth

import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class Auth0TokenServiceTest {

    @Test
    fun `getAccessToken refresca cuando no hay token y devuelve el nuevo`() {
        val service = Auth0TokenService(
            issuer = "https://example-issuer.com/",
            clientId = "cid",
            clientSecret = "secret",
            audience = "aud",
            restBuilder = RestClient.builder(),
        )

        val restMock = mockk<RestClient>()
        val postSpec = mockk<RestClient.RequestBodyUriSpec>()
        val retrieveSpec = mockk<RestClient.ResponseSpec>()

        every { restMock.post() } returns postSpec
        every { postSpec.uri(any<String>()) } returns postSpec
        every { postSpec.contentType(MediaType.APPLICATION_JSON) } returns postSpec
        every { postSpec.body(any<Map<String, String>>()) } returns postSpec
        every { postSpec.retrieve() } returns retrieveSpec
        every { retrieveSpec.body(Auth0TokenService.TokenResponse::class.java) } returns Auth0TokenService.TokenResponse(
            accessToken = "m2m-token",
            expiresIn = 3600,
            tokenType = "Bearer",
        )

        val field = Auth0TokenService::class.java.getDeclaredField("rest")
        field.isAccessible = true
        field.set(service, restMock)

        val token = service.getAccessToken()
        assertEquals("m2m-token", token)
        val expiresAtField = Auth0TokenService::class.java.getDeclaredField("expiresAt")
        expiresAtField.isAccessible = true
        val expiresAt = expiresAtField.get(service) as Instant
        assert(expiresAt.isAfter(Instant.now()))
    }

    @Test
    fun `getAccessToken no refresca si el token sigue vigente`() {
        val service = Auth0TokenService(
            issuer = "https://example-issuer.com/",
            clientId = "cid",
            clientSecret = "secret",
            audience = "aud",
            restBuilder = RestClient.builder(),
        )

        val restMock = mockk<RestClient>(relaxed = true) // no falla si se llama a metodos no definidos con every

        val restField = Auth0TokenService::class.java.getDeclaredField("rest")
        restField.isAccessible = true
        restField.set(service, restMock)

        // Seteo un token y una expiración en el futuro (más que TOKEN_RENEW_WINDOW_SEC)
        val tokenField = Auth0TokenService::class.java.getDeclaredField("accessToken")
        tokenField.isAccessible = true
        tokenField.set(service, "cached-token")

        val expiresAtField = Auth0TokenService::class.java.getDeclaredField("expiresAt")
        expiresAtField.isAccessible = true
        expiresAtField.set(service, Instant.now().plusSeconds(3600))

        val token = service.getAccessToken()
        assertEquals("cached-token", token)
        // como el token está vigente, nunca debería llamarse rest.post()
        io.mockk.verify(exactly = 0) { restMock.post() }
    }
}
