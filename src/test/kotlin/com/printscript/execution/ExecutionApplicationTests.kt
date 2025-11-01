package com.printscript.execution

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertTrue

@SpringBootTest(
    classes = [ExecutionApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@TestPropertySource(
    properties = [
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "auth0.client-id=test-client-id",
        "auth0.client-secret=test-client-secret",
        "auth0.audience=https://test-audience",
        "auth0.domain=test-domain.auth0.com",
        "snippets.base-url=http://test-snippets:8080",
        "streams.enabled=false",
        "spring.data.redis.host=test-redis",
        "spring.data.redis.port=6379",
    ],
)
class ExecutionApplicationTests {
    @Test
    fun contextLoads() {
        assertTrue(true)
    }
}
