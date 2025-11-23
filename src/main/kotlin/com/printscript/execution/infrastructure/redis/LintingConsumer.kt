package com.printscript.execution.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.newrelic.api.agent.NewRelic
import com.printscript.execution.application.ExecutionService
import com.printscript.execution.infrastructure.snippets.SnippetsClient
import com.printscript.execution.logs.CorrelationIdFilter.Companion.CORRELATION_ID_KEY
import io.printscript.contracts.events.LintingRulesUpdated
import io.printscript.contracts.linting.LintReq
import jakarta.annotation.PostConstruct
import org.austral.ingsis.redis.RedisStreamConsumer
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import java.time.Duration

private const val POLL_TIMEOUT_SECONDS = 10L
private const val LOG_PREVIEW_CHARS = 200
private const val LOGS = 500

@Component
@ConditionalOnProperty(prefix = "streams", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class LintingConsumer(
    @Value("\${streams.linting.key}") rawStreamKey: String,
    @Value("\${streams.linting.group}") rawGroup: String,
    private val om: ObjectMapper,
    private val exec: ExecutionService,
    private val snippets: SnippetsClient,
    @Qualifier("stringTemplate") redis: RedisTemplate<String, String>,
) : RedisStreamConsumer<String>(
    streamKey = rawStreamKey.trim().trim('"', '\''),
    groupId = rawGroup.trim().trim('"', '\''),
    redis = redis,
) {

    private val logger = LoggerFactory.getLogger(LintingConsumer::class.java)

    @PostConstruct
    fun started() {
        logger.info("LintingConsumer READY stream={} group={}", streamKey, groupId)
    }

    override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, String>> = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(Duration.ofSeconds(POLL_TIMEOUT_SECONDS))
        .targetType(String::class.java)
        .build()

    @Suppress("TooGenericExceptionCaught")
    public override fun onMessage(record: ObjectRecord<String, String>) {
        val raw = record.value
        logger.info("[lint] message received rawPreview={}", raw.take(LOG_PREVIEW_CHARS))

        val ev: LintingRulesUpdated = try {
            om.readValue(raw, LintingRulesUpdated::class.java)
        } catch (e: Exception) {
            logger.error("[lint][DESER ERROR] {}: {}", e::class.java.name, e.message, e)
            return
        }

        val corrId = ev.correlationalId ?: "lint-${ev.snippetId}"
        MDC.put(CORRELATION_ID_KEY, corrId)
        NewRelic.addCustomParameter(CORRELATION_ID_KEY, corrId)

        try {
            logger.info(
                "[lint] event ok snippetId={} corrId={} lang={} version={}",
                ev.snippetId,
                ev.correlationalId,
                ev.language,
                ev.version,
            )
            val content = snippets.getContent(ev.snippetId)

            val res = exec.lint(
                LintReq(
                    language = ev.language,
                    version = ev.version,
                    content = content,
                    configText = ev.configText,
                    configFormat = ev.configFormat,
                ),
            )

            snippets.saveLint(ev.snippetId, res.violations)
            logger.info(
                "[lint] saved violations={} snippetId={}",
                res.violations.size,
                ev.snippetId,
            )
        } catch (e: RestClientResponseException) {
            logger.error(
                "[lint][HTTP] status={} bodyPreview={} snippetId={}",
                e.statusCode.value(),
                e.responseBodyAsString?.take(LOGS),
                ev.snippetId,
                e,
            )
        } catch (e: ResourceAccessException) {
            logger.error("[lint][NETWORK] {}: {}", e::class.java.simpleName, e.message, e)
        } catch (e: Exception) {
            logger.error("[lint][UNEXPECTED] {}: {}", e::class.java.name, e.message, e)
        } finally {
            MDC.remove(CORRELATION_ID_KEY)
        }
    }
}
