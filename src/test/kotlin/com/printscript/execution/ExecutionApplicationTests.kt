package com.printscript.execution

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import kotlin.test.assertTrue

@SpringBootTest(
    classes = [ExecutionApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
)
@Import(TestConfig::class)
@TestPropertySource(
    properties = [
        "spring.autoconfigure.exclude=" +
            "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
            "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration",

        "streams.enabled=false",

        "auth0.client-id=test-client-id",
        "auth0.client-secret=test-client-secret",
        "auth0.audience=https://test-audience",
        "auth0.domain=test-domain.auth0.com",
        "snippets.base-url=http://test-snippets:8080",

        "spring.main.allow-circular-references=true",
    ],
)
@ActiveProfiles("test")
class ExecutionApplicationTests {
    @Test fun contextLoads() {
        kotlin.test.assertTrue(true)
    }
}
