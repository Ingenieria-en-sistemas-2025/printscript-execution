package com.printscript.execution.redis

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.stream.Consumer
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.ReadOffset
import org.springframework.data.redis.connection.stream.StreamOffset
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import reactor.core.publisher.Flux
import reactor.util.retry.Retry
import java.net.InetAddress
import java.time.Duration

/**
 * Consumidor genérico y resiliente de Redis Streams con:
 * - Autocreación de stream + consumer group si no existen (MKSTREAM implícito).
 * - Logs consistentes (info/warn/error) con detalles.
 * - Backoff infinito con jitter ante errores.
 */
abstract class ResilientRedisStreamConsumer<Value : Any>(rawStreamKey: String, rawGroupId: String, private val redis: RedisTemplate<String, Any>) {

    // Limpia comillas y whitespace en extremos
    private fun clean(s: String) = s.trim().trim('"', '\'')

    protected val streamKey: String = clean(rawStreamKey)
    protected val groupId: String = clean(rawGroupId)

    private val logger = LoggerFactory.getLogger(javaClass)

    protected abstract fun onMessage(record: ObjectRecord<String, Value>)
    protected abstract fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, Value>>

    private lateinit var flow: Flux<ObjectRecord<String, Value>>

    @PostConstruct
    @Suppress("TooGenericExceptionCaught")
    fun subscription() {
        // Log defensivo para ver exactamente qué clave se usa
        logger.info("Starting consumer: stream='{}' group='{}'", streamKey, groupId)

        try {
            if (!consumerGroupExists(streamKey, groupId)) {
                logger.info("Group '{}' not found on stream '{}'. Creating…", groupId, streamKey)
                createConsumerGroup(streamKey, groupId)
            } else {
                logger.info("Group '{}' already exists on stream '{}'", groupId, streamKey)
            }
        } catch (e: Exception) {
            logger.warn("Check/create group failed for stream='{}' group='{}': {}", streamKey, groupId, e.message, e)
            logger.info("Fallback: creating stream/group with MKSTREAM")
            createConsumerGroup(streamKey, groupId)
        }

        val factory = redis.connectionFactory as ReactiveRedisConnectionFactory
        val container = StreamReceiver.create(factory, options())

        flow = container
            .receiveAutoAck(
                Consumer.from(groupId, InetAddress.getLocalHost().hostName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            )
            .doOnSubscribe { logger.info("[{} / {}] SUBSCRIBED", groupId, streamKey) }
            .doOnNext { rec -> logger.info("[{} / {}] DELIVER id={} type={}", groupId, streamKey, rec.id.value, rec.value?.javaClass?.name) }
            .doOnError { t -> logger.error("[{} / {}] STREAM ERROR: {}", groupId, streamKey, t.message, t) }
            .retryWhen(
                Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF).maxBackoff(MAX_BACKOFF).jitter(JITTER_FACTOR),
            )

        flow.subscribe(
            { rec -> onMessage(rec) },
            { t -> logger.error("[{}/{}] subscription terminated: {}", groupId, streamKey, t.message, t) },
        )
    }

    private fun createConsumerGroup(streamKey: String, groupId: String) {
        redis.opsForStream<Any, Any>()
            .createGroup(streamKey, ReadOffset.latest(), groupId) // XGROUP CREATE … $ MKSTREAM
        logger.info("Created group='{}' on stream='{}'", groupId, streamKey)
    }

    private fun consumerGroupExists(stream: String, group: String): Boolean = try {
        redis.opsForStream<Any, Any>().groups(stream).any { it.groupName() == group }
    } catch (_: Exception) {
        false
    }

    private companion object {
        const val MAX_RETRIES: Long = Long.MAX_VALUE
        val INITIAL_BACKOFF: Duration = Duration.ofSeconds(1)
        val MAX_BACKOFF: Duration = Duration.ofSeconds(30)
        const val JITTER_FACTOR: Double = 0.2
    }
}
