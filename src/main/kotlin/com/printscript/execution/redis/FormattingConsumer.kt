package com.printscript.execution.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.printscript.execution.dto.FormatReq
import com.printscript.execution.service.ExecutionService
import com.printscript.snippets.redis.events.SnippetsFormattingRulesUpdated
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.time.Duration

private const val MAX_ATTEMPTS = 3
private const val POLL_TIMEOUT_SECONDS = 10L
private val POLL_TIMEOUT: Duration = Duration.ofSeconds(POLL_TIMEOUT_SECONDS)

private fun sanitizeKey(raw: String) = raw.trim().trim('"', '\'')

@Profile("!test")
@ConditionalOnProperty(prefix = "streams", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@Component
class FormattingConsumer(
    @Qualifier("redisTemplateJson")
    private val redisJson: RedisTemplate<String, Any>,
    @Value("\${streams.formatting.key}") rawStreamKey: String,
    @Value("\${streams.formatting.group}") groupId: String,
    private val exec: ExecutionService,
    private val snippets: SnippetsClient,
    @Value("\${streams.dlq.formatting}") private val dlqKey: String,
) : ResilientRedisStreamConsumer<SnippetsFormattingRulesUpdated>(sanitizeKey(rawStreamKey), groupId, redisJson) {

    private val streamKeyForRetry: String = streamKey
    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        logger.info(
            "STREAM RAW='{}'  CLEAN='{}'  RAW_BYTES={}",
            rawStreamKey,
            streamKey,
            rawStreamKey.toCharArray().joinToString(",") { it.code.toString() },
        )
    }

    override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, SnippetsFormattingRulesUpdated>> = StreamReceiver.StreamReceiverOptions
        .builder()
        .pollTimeout(POLL_TIMEOUT)
        .targetType(SnippetsFormattingRulesUpdated::class.java)
        .build()

    override fun onMessage(record: ObjectRecord<String, SnippetsFormattingRulesUpdated>) {
        val event = record.value
        try {
            val content = snippets.getContent(event.snippetId)
            val res = exec.formatContent(
                FormatReq(
                    language = event.language,
                    version = event.version,
                    content = content,
                    configText = event.configText,
                    configFormat = event.configFormat,
                    options = event.options,
                ),
            )
            snippets.saveFormatted(event.snippetId, res.formattedContent)
        } catch (e: RestClientResponseException) {
            logger.warn("HTTP error formatting snippetId={} status={} body={}", event.snippetId, e.statusCode.value(), e.responseBodyAsString, e)
            retryOrDlq(event)
        } catch (e: ResourceAccessException) {
            logger.warn("Resource access error formatting snippetId={}: {}", event.snippetId, e.message, e)
            retryOrDlq(event)
        } catch (e: RestClientException) {
            logger.warn("REST client error formatting snippetId={}: {}", event.snippetId, e.message, e)
            retryOrDlq(event)
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid args formatting snippetId={}: {}", event.snippetId, e.message, e)
            retryOrDlq(event)
        } catch (e: IllegalStateException) {
            logger.warn("Illegal state formatting snippetId={}: {}", event.snippetId, e.message, e)
            retryOrDlq(event)
        }
    }

    private fun retryOrDlq(ev: SnippetsFormattingRulesUpdated) {
        if (ev.attempt + 1 <= MAX_ATTEMPTS) {
            val next = ev.copy(attempt = ev.attempt + 1)
            redisJson.opsForStream<String, Any>()
                .add(StreamRecords.newRecord().ofObject(next).withStreamKey(streamKeyForRetry))
            logger.info("Retry scheduled for snippetId={} attempt={}", ev.snippetId, next.attempt)
        } else {
            redisJson.opsForStream<String, Any>()
                .add(StreamRecords.newRecord().ofObject(ev).withStreamKey(dlqKey))
            logger.error("Sent to DLQ={} snippetId={} after attempts={}", dlqKey, ev.snippetId, ev.attempt)
        }
    }
}
