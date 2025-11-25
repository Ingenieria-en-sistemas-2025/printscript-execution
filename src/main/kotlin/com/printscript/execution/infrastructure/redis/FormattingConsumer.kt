package com.printscript.execution.infrastructure.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.newrelic.api.agent.NewRelic
import com.printscript.execution.application.ExecutionService
import com.printscript.execution.infrastructure.snippets.SnippetsClient
import com.printscript.execution.logs.CorrelationIdFilter.Companion.CORRELATION_ID_KEY
import io.printscript.contracts.events.FormattingRulesUpdated
import io.printscript.contracts.formatter.FormatReq
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
import java.time.Duration

private const val POLL_TIMEOUT_SECONDS = 10L
private const val LOG_PREVIEW_CHARS = 200

@Component
@ConditionalOnProperty(prefix = "streams", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class FormattingConsumer(
    @Value("\${streams.formatting.key}") rawStreamKey: String,
    @Value("\${streams.formatting.group}") rawGroup: String,
    private val om: ObjectMapper,
    private val exec: ExecutionService,
    private val snippets: SnippetsClient,
    @Qualifier("stringTemplate") redis: RedisTemplate<String, String>,
) : RedisStreamConsumer<String>(
    streamKey = rawStreamKey.trim().trim('"', '\''),
    groupId = rawGroup.trim().trim('"', '\''),
    redis = redis,
) {

    private val logger = LoggerFactory.getLogger(FormattingConsumer::class.java)

    @PostConstruct
    fun started() {
        logger.info("FormattingConsumer READY stream={} group={}", streamKey, groupId)
    }

    override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, String>> = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(Duration.ofSeconds(POLL_TIMEOUT_SECONDS))
        .targetType(String::class.java)
        .build()

    @Suppress("TooGenericExceptionCaught")
    public override fun onMessage(record: ObjectRecord<String, String>) {
        val raw = record.value
        logger.info("[format] message received rawPreview={}", raw.take(LOG_PREVIEW_CHARS))

        val ev: FormattingRulesUpdated = try {
            om.readValue(raw, FormattingRulesUpdated::class.java)
        } catch (e: Exception) {
            logger.error("[format][DESER] {}", e.message, e)
            return
        }

        val corrId = ev.correlationalId ?: "format-${ev.snippetId}"
        MDC.put(CORRELATION_ID_KEY, corrId)
        NewRelic.addCustomParameter(CORRELATION_ID_KEY, corrId)

        try {
            logger.info(
                "[format] event ok snippetId={} lang={} version={} corrId={}",
                ev.snippetId,
                ev.language,
                ev.version,
                corrId,
            )
            val content = snippets.getContent(ev.snippetId)
            val res = exec.formatContent(
                FormatReq(
                    language = ev.language,
                    version = ev.version,
                    content = content,
                    configText = ev.configText ?: "{}",
                    configFormat = ev.configFormat ?: "json",
                ),
            )
            snippets.saveFormatted(ev.snippetId, res.formattedContent)

            logger.info(
                "[format] saved snippetId={} formattedLen={}",
                ev.snippetId,
                res.formattedContent.length,
            )
        } catch (e: Exception) {
            logger.error("[format][ERR] {}: {}", e::class.java.simpleName, e.message, e)
        } finally {
            MDC.remove(CORRELATION_ID_KEY)
        }
    }
}
