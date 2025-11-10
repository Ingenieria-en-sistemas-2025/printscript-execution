package com.printscript.execution.redis

import com.printscript.execution.dto.LintReq
import com.printscript.execution.service.ExecutionService
import com.printscript.snippets.redis.events.SnippetsFormattingRulesUpdated
import com.printscript.snippets.redis.events.SnippetsLintingRulesUpdated
import org.austral.ingsis.redis.RedisStreamConsumer
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

@Component
@Profile("!test")
class LintingConsumer(
    @Qualifier("redisTemplateJson")
    private val redisJson: RedisTemplate<String, Any>,
    @Value("\${streams.linting.key}") rawStreamKey: String,
    @Value("\${streams.linting.group}") groupId: String,
    private val exec: ExecutionService,
    private val snippets: SnippetsClient,
    @Value("\${streams.dlq.linting}") private val dlqKey: String,
    private val genericJsonSerializer: GenericJackson2JsonRedisSerializer,
) : ResilientRedisStreamConsumer<SnippetsLintingRulesUpdated>(
    sanitizeKey(rawStreamKey),
    groupId,
    redisJson,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val streamKeyForRetry: String = streamKey

    init {
        logger.info(
            "LintingConsumer streamKey='{}' group='{}' dlq='{}'",
            streamKeyForRetry,
            groupId,
            dlqKey,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, SnippetsLintingRulesUpdated>> {
        val pair = RedisSerializationContext.SerializationPair.fromSerializer(genericJsonSerializer)
        return StreamReceiver.StreamReceiverOptions.builder()
            .pollTimeout(POLL_TIMEOUT)
            .serializer(pair)
            .targetType(SnippetsLintingRulesUpdated::class.java)
            .build() as StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, SnippetsLintingRulesUpdated>>
    }

    override fun onMessage(record: ObjectRecord<String, SnippetsLintingRulesUpdated>) {
        val event = record.value
        try {
            val content =
                snippets.getContent(event.snippetId)
            val res = exec.lint(
                LintReq(
                    language = event.language,
                    version = event.version,
                    content = content,
                    configText = event.configText,
                    configFormat = event.configFormat,
                ),
            )
            snippets.saveLint(event.snippetId, res.violations)
        } catch (e: RestClientResponseException) {
            logger.warn("HTTP error linting snippetId={} status={} body={}", event.snippetId, e.statusCode.value(), e.responseBodyAsString, e)
            retryOrDlq(event)
        } catch (e: ResourceAccessException) {
            logger.warn("Resource access error linting snippetId={}: {}", event.snippetId, e.message, e)
            retryOrDlq(event)
        } catch (e: RestClientException) {
            logger.warn("REST client error linting snippetId={}: {}", event.snippetId, e.message, e)
            retryOrDlq(event)
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid args linting snippetId={}: {}", event.snippetId, e.message, e)
            retryOrDlq(event)
        } catch (e: IllegalStateException) {
            logger.warn("Illegal state linting snippetId={}: {}", event.snippetId, e.message, e)
            retryOrDlq(event)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun retryOrDlq(ev: SnippetsLintingRulesUpdated) {
        try {
            if (ev.attempt + 1 <= MAX_ATTEMPTS) {
                val next = ev.copy(attempt = ev.attempt + 1)
                redisJson.opsForStream<String, String>()
                    .add(
                        StreamRecords
                            .newRecord()
                            .ofObject(next)
                            .withStreamKey(streamKeyForRetry),
                    )
                logger.info(
                    "Retry scheduled for snippetId={} attempt={}",
                    ev.snippetId,
                    next.attempt,
                )
            } else {
                redisJson.opsForStream<String, String>()
                    .add(
                        StreamRecords
                            .newRecord()
                            .ofObject(ev)
                            .withStreamKey(dlqKey),
                    )
                logger.error(
                    "Sent to DLQ={} snippetId={} after attempts={}",
                    dlqKey,
                    ev.snippetId,
                    ev.attempt,
                )

                // Intentamos marcar el lint como fallido
                safeMarkFailed(ev)
            }
        } catch (ex: Exception) {
            logger.error(
                "Unexpected error retrying snippetId={} attempt={} -> {}",
                ev.snippetId,
                ev.attempt,
                ex.message,
                ex,
            )
        }
    }

    private fun safeMarkFailed(ev: SnippetsLintingRulesUpdated) {
        try {
            snippets.markLintFailed(ev.snippetId)
        } catch (e: RestClientResponseException) {
            logger.error(
                "Notify lint failed (HTTP) snippetId={} status={} body={}",
                ev.snippetId,
                e.statusCode.value(),
                e.responseBodyAsString,
                e,
            )
        } catch (e: ResourceAccessException) {
            logger.error(
                "Notify lint failed (resource access) snippetId={}: {}",
                ev.snippetId,
                e.message,
                e,
            )
        } catch (e: RestClientException) {
            logger.error(
                "Notify lint failed (rest client) snippetId={}: {}",
                ev.snippetId,
                e.message,
                e,
            )
        } catch (e: IllegalStateException) {
            logger.error(
                "Notify lint failed (illegal state) snippetId={}: {}",
                ev.snippetId,
                e.message,
                e,
            )
        }
    }
}
