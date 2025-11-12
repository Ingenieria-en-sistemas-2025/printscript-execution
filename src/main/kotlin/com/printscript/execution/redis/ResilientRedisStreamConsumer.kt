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
import reactor.core.Disposable
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
abstract class ResilientRedisStreamConsumer(rawStreamKey: String, rawGroupId: String, private val redis: RedisTemplate<String, String>, private val receiver: StreamReceiver<String, ObjectRecord<String, String>>) {

    // Limpia comillas y whitespace en extremos
    private fun hardClean(s: String) = s
        .trim()
        .replace("\\\"", "") // quita comillas escapadas
        .replace("\"", "") // quita comillas dobles
        .replace("“", "").replace("”", "") // comillas tipográficas
        .replace("'", "") // comillas simples

    protected val streamKey: String = hardClean(rawStreamKey)
    protected val groupId: String = hardClean(rawGroupId)

    private val logger = LoggerFactory.getLogger(javaClass)

    protected abstract fun onMessage(record: ObjectRecord<String, String>)
    private val consumerName: String =
        (System.getenv("HOSTNAME") ?: "ps-execution") + ":" + ProcessHandle.current().pid()

    private lateinit var flow: Flux<ObjectRecord<String, String>>
    private lateinit var subscription: Disposable

    @PostConstruct
    @Suppress("TooGenericExceptionCaught")
    fun subscription() {
        logger.info("Starting consumer: stream='{}' group='{}' consumer='{}'", streamKey, groupId, consumerName)

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

        logger.info("[{} / {}] SUBSCRIBING as {} with offset=lastConsumed", groupId, streamKey, consumerName)
        flow = receiver
            .receiveAutoAck(
                Consumer.from(groupId, consumerName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            )
            .doOnSubscribe { logger.info("[{} / {}] SUBSCRIBED as {}", groupId, streamKey, consumerName) }
            .doOnNext { rec ->
                logger.info("[{} / {}] DELIVER id={} type={}", groupId, streamKey, rec.id.value, rec.value?.javaClass?.name)
            }
            .doOnError { t ->
                logger.error("[{} / {}] STREAM ERROR ({}): {}", groupId, streamKey, t.javaClass.simpleName, t.message, t)
            }
            .retryWhen(
                Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF)
                    .maxBackoff(MAX_BACKOFF)
                    .jitter(JITTER_FACTOR),
            )

        subscription = flow.subscribe(
            { rec -> onMessage(rec) },
            { t -> logger.error("[{}/{}] subscription terminated: {}", groupId, streamKey, t.message, t) },
        )
        logger.info("[{} / {}] SUBSCRIPTION STARTED -> {}", groupId, streamKey, subscription)
    }

    private fun createConsumerGroup(streamKey: String, groupId: String) {
        try {
            redis.opsForStream<String, String>().add(streamKey, mapOf("init" to "1"))
        } catch (_: Exception) { /* no-op si ya existe */ }
        redis.opsForStream<String, String>().createGroup(streamKey, ReadOffset.latest(), groupId)
        logger.info("Created group='{}' on stream='{}'", groupId, streamKey)
    }

    private fun consumerGroupExists(stream: String, group: String): Boolean = try {
        redis.opsForStream<String, String>().groups(stream).any { it.groupName() == group }
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
