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

abstract class ResilientRedisStreamConsumer<Value : Any>(protected val streamKey: String, protected val groupId: String, private val redis: RedisTemplate<String, Any>) {
    private val logger = LoggerFactory.getLogger(javaClass)

    protected abstract fun onMessage(record: ObjectRecord<String, Value>)
    protected abstract fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, Value>>

    private lateinit var flow: Flux<ObjectRecord<String, Value>>

    @PostConstruct
    @Suppress("TooGenericExceptionCaught")
    fun subscription() {
        val opts = options()

        try {
            val exists = consumerGroupExists(streamKey, groupId)
            if (!exists) {
                println("Consumer group $groupId for stream $streamKey doesn't exist. Creating...")
                createConsumerGroup(streamKey, groupId)
            } else {
                println("Consumer group $groupId for stream $streamKey exists!")
            }
        } catch (e: Exception) {
            println("Exception: $e")
            println("Stream $streamKey doesn't exist. Creating stream $streamKey and group $groupId")
            redis.opsForStream<Any, Any>().createGroup(streamKey, groupId)
        }

        val factory = redis.connectionFactory as ReactiveRedisConnectionFactory
        val container = StreamReceiver.create(factory, opts)

        flow = container
            .receiveAutoAck(
                Consumer.from(groupId, InetAddress.getLocalHost().hostName),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()),
            )
            .doOnSubscribe { logger.info("[{} / {}] SUBSCRIBED", groupId, streamKey) }
            .doOnNext { rec -> logger.info("[{} / {}] DELIVER id={} type={}", groupId, streamKey, rec.id.value, rec.value?.javaClass?.name) }
            .doOnError { t ->
                logger.error("[{} / {}] STREAM ERROR: {}", groupId, streamKey, t.message, t) // mensaje + stacktrace
                t.printStackTrace()
            }
            .retryWhen(
                Retry.backoff(MAX_RETRIES, INITIAL_BACKOFF).maxBackoff(MAX_BACKOFF).jitter(JITTER_FACTOR),
            )

        flow.subscribe(
            { rec -> this.onMessage(rec) },
            { t -> System.err.println("[$groupId/$streamKey] subscription terminated: ${t.message}") },
        )
    }

    private fun createConsumerGroup(streamKey: String, groupId: String): String = redis.opsForStream<Any, Any>().createGroup(streamKey, groupId)

    private fun consumerGroupExists(stream: String, group: String): Boolean = redis.opsForStream<Any, Any>().groups(stream).any { it.groupName() == group }

    private companion object {
        const val MAX_RETRIES: Long = Long.MAX_VALUE
        val INITIAL_BACKOFF: Duration = Duration.ofSeconds(1)
        val MAX_BACKOFF: Duration = Duration.ofSeconds(30)
        const val JITTER_FACTOR: Double = 0.2
    }
}
