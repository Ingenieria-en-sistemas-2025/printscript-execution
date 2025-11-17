package com.printscript.execution.redis

import com.printscript.execution.infrastructure.redis.RedisConfiguration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer
import kotlin.test.Test
import kotlin.test.assertNotNull

class RedisConfigurationTest {

    @Test
    fun `connectionFactory y stringTemplate se crean correctamente`() {
        val config = RedisConfiguration(host = "localhost", port = 6379)
        val cf: RedisConnectionFactory = config.connectionFactory()

        val template: RedisTemplate<String, String> = config.stringTemplate(cf)

        assertNotNull(cf)
        assertNotNull(template.connectionFactory)
        assert(template.keySerializer is StringRedisSerializer)
        assert(template.valueSerializer is StringRedisSerializer)
    }
}
