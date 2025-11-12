package com.printscript.execution.redis

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.data.redis.stream.StreamReceiver
import java.time.Duration

private const val STREAM_POLL_TIMEOUT_SECONDS = 3L

@Profile("!test")
@Configuration
class RedisConfiguration(@Value("\${spring.data.redis.host}") private val host: String, @Value("\${spring.data.redis.port}") private val port: Int) {
    @Bean
    fun stringTemplate(cf: RedisConnectionFactory): RedisTemplate<String, String> = RedisTemplate<String, String>().apply {
        setConnectionFactory(cf)
        keySerializer = StringRedisSerializer()
        valueSerializer = StringRedisSerializer()
        hashKeySerializer = StringRedisSerializer()
        hashValueSerializer = StringRedisSerializer()
        afterPropertiesSet()
    }

    @Bean
    fun streamReceiver(cf: ReactiveRedisConnectionFactory): StreamReceiver<String, ObjectRecord<String, String>> = StreamReceiver.create(
        cf,
        StreamReceiver.StreamReceiverOptions
            .builder()
            .pollTimeout(Duration.ofSeconds(STREAM_POLL_TIMEOUT_SECONDS))
            .targetType(String::class.java)
            .build(),
    )
}
