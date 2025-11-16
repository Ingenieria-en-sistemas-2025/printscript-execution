package com.printscript.execution.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SecurityConfigTest {

    private val issuer = "https://example-issuer.com/"
    private val audience = "my-api"

    private val config = SecurityConfig(issuer = issuer, audience = audience)

    @Test
    fun `tokenValidator acepta jwt con issuer y audience correctos`() {
        val validator: OAuth2TokenValidator<Jwt> = config.tokenValidator()

        val now = Instant.now()
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("iss", issuer)
            .claim("aud", listOf(audience))
            .claim("iat", now)
            .claim("exp", now.plusSeconds(300))
            .build()

        val result = validator.validate(jwt)

        assertFalse(result.hasErrors())
    }

    @Test
    fun `tokenValidator rechaza jwt con audience incorrecta`() {
        val validator: OAuth2TokenValidator<Jwt> = config.tokenValidator()

        val now = Instant.now()
        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("iss", issuer)
            .claim("aud", listOf("other-api"))
            .claim("iat", now)
            .claim("exp", now.plusSeconds(300))
            .build()

        val result = validator.validate(jwt)

        assertTrue(result.hasErrors())
    }

    @Test
    fun `permissionsConverter agrega authorities SCOPE_ por permissions`() {
        val method = SecurityConfig::class.java.getDeclaredMethod("permissionsConverter")
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val converter = method.invoke(config) as Converter<Jwt, AbstractAuthenticationToken>

        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "user-123")
            .claim("permissions", listOf("snippets:read", "snippets:write"))
            .build()

        val auth = converter.convert(jwt)!!

        val authNames = auth.authorities.map { it.authority }.toSet()
        assertTrue(authNames.contains("SCOPE_snippets:read"))
        assertTrue(authNames.contains("SCOPE_snippets:write"))
    }

    @Test
    fun `permissionsConverter sin permissions no agrega scopes extra`() {
        val method = SecurityConfig::class.java.getDeclaredMethod("permissionsConverter")
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val converter = method.invoke(config) as Converter<Jwt, AbstractAuthenticationToken>

        val jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("sub", "user-456")
            // sin permissions
            .build()

        val auth = converter.convert(jwt)!!

        assertTrue(auth.authorities.isEmpty())
    }
}
