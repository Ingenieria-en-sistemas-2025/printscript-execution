package com.printscript.execution

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertTrue

@SpringBootTest(properties = ["streams.enabled=false"])
@TestPropertySource(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://test-auth-issuer.com/", // <- Agregado para JWT
        "auth0.client-id=test-client-id",
        "auth0.client-secret=test-client-secret",
        "auth0.audience=https://test-audience",
        "auth0.domain=test-domain.auth0.com",
        "snippets.base-url=http://test-snippets:8080",
    ],
)
class ExecutionApplicationTests {

    @Test
    fun contextLoads() {
        assertTrue(true)
    }
}
