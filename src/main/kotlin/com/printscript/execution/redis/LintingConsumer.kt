// LintingConsumer.kt
package com.printscript.execution.redis

import com.printscript.execution.dto.LintReq
import com.printscript.execution.service.ExecutionService
import org.austral.ingsis.redis.RedisStreamConsumer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import java.time.Duration

private const val MAX_ATTEMPTS = 3
private const val POLL_TIMEOUT_SECONDS = 10L
private val POLL_TIMEOUT: Duration = Duration.ofSeconds(POLL_TIMEOUT_SECONDS)

@ConditionalOnProperty(prefix = "streams", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@Component
class LintingConsumer(@param:Qualifier("redisTemplateJson") private val redisJson: RedisTemplate<String, Any>, redisStr: RedisTemplate<String, String>, @Value("\${streams.linting.key}") streamKey: String, @Value("\${streams.linting.group}") groupId: String, private val exec: ExecutionService, private val snippets: SnippetsClient, @Value("\${streams.dlq.linting}") private val dlqKey: String) :
    RedisStreamConsumer<SnippetsLintingRulesUpdated>(streamKey, groupId, redisStr) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, SnippetsLintingRulesUpdated>> = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(POLL_TIMEOUT)
        .targetType(SnippetsLintingRulesUpdated::class.java)
        .build()

    override fun onMessage(record: ObjectRecord<String, SnippetsLintingRulesUpdated>) {
        val event = record.value
        try {
            val content = snippets.getContent(event.snippetId)
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

    private fun retryOrDlq(ev: SnippetsLintingRulesUpdated) {
        if (ev.attempt + 1 <= MAX_ATTEMPTS) {
            val next = ev.copy(attempt = ev.attempt + 1)
            redisJson.opsForStream<String, Any>()
                .add(StreamRecords.newRecord().ofObject(next).withStreamKey(streamKey))
            logger.info("Retry scheduled for snippetId={} attempt={}", ev.snippetId, next.attempt)
        } else {
            redisJson.opsForStream<String, Any>()
                .add(StreamRecords.newRecord().ofObject(ev).withStreamKey(dlqKey))
            logger.error("Sent to DLQ={} snippetId={} after attempts={}", dlqKey, ev.snippetId, ev.attempt)
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
}
