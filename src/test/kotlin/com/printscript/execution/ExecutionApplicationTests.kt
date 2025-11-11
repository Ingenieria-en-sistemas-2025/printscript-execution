package com.printscript.execution

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertTrue

@SpringBootTest(
    classes = [TestApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = [
        "spring.main.web-application-type=none",
        "streams.enabled=false",
        "spring.main.allow-circular-references=true",
        "auth0.client-id=test-client-id",
        "auth0.client-secret=test-client-secret",
        "auth0.audience=https://test-audience",
        "auth0.domain=test-domain.auth0.com",
        "snippets.base-url=http://test-snippets:8080",
    ],
)
@EnableAutoConfiguration(
    exclude = [
        RedisAutoConfiguration::class,
        RedisReactiveAutoConfiguration::class,
        RedisRepositoriesAutoConfiguration::class,
        SecurityAutoConfiguration::class,
        OAuth2ResourceServerAutoConfiguration::class,
    ],
)
@Import(TestConfig::class)
@ActiveProfiles("test")
class ExecutionApplicationTests {
    @Test fun contextLoads() {
        assertTrue(true)
    }
}
