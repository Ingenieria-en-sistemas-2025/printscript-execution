package com.printscript.execution.redis

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfiguration(@Value("\${spring.data.redis.host}") private val host: String, @Value("\${spring.data.redis.port}") private val port: Int) {

    @Bean
    fun connectionFactory() = LettuceConnectionFactory(RedisStandaloneConfiguration(host, port))

    @Bean("redisTemplateString")
    @Primary
    fun redisTemplate(cf: LettuceConnectionFactory): RedisTemplate<String, String> = RedisTemplate<String, String>().apply {
        setConnectionFactory(cf)
        afterPropertiesSet()
    }

    @Bean("redisTemplateJson")
    fun redisTemplateJson(cf: RedisConnectionFactory): RedisTemplate<String, Any> = RedisTemplate<String, Any>().apply {
        setConnectionFactory(cf)

        val json = GenericJackson2JsonRedisSerializer()
        keySerializer = StringRedisSerializer()
        hashKeySerializer = StringRedisSerializer()
        valueSerializer = json
        hashValueSerializer = json
        afterPropertiesSet()
    }
}
