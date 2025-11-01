package com.printscript.execution

import com.printscript.execution.config.SecurityConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertTrue

@SpringBootTest(
    classes = [ExecutionApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@TestPropertySource(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://test-auth-issuer.com/",
        "auth0.client-id=test-client-id",
        "auth0.client-secret=test-client-secret",
        "auth0.audience=https://test-audience",
        "auth0.domain=test-domain.auth0.com",
        "snippets.base-url=http://test-snippets:8080",
        "streams.enabled=false",
        "spring.data.redis.host=test-redis",
        "spring.data.redis.port=6379",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
    ],
)
class ExecutionApplicationTests {

    @Test
    fun contextLoads() {
        assertTrue(true)
    }
}
