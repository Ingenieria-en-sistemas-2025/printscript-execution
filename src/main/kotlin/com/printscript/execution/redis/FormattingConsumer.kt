package com.printscript.execution.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.printscript.execution.dto.FormatReq
import com.printscript.execution.service.ExecutionService
import com.printscript.snippets.redis.events.SnippetsFormattingRulesUpdated
import jakarta.annotation.PostConstruct
import org.austral.ingsis.redis.RedisStreamConsumer
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
class FormattingConsumer(@Value("\${streams.formatting.key}") rawStreamKey: String, @Value("\${streams.formatting.group}") rawGroup: String, private val om: ObjectMapper, private val exec: ExecutionService, private val snippets: SnippetsClient, @Qualifier("stringTemplate") redis: RedisTemplate<String, String>) :
    RedisStreamConsumer<String>(
        streamKey = rawStreamKey.trim().trim('"', '\''),
        groupId = rawGroup.trim().trim('"', '\''),
        redis = redis,
    ) {

    @PostConstruct
    fun started() {
        println("[Formatter consumer] stream=$streamKey group=$groupId READY")
    }

    override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, String>> = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(Duration.ofSeconds(POLL_TIMEOUT_SECONDS))
        .targetType(String::class.java)
        .build()

    override fun onMessage(record: ObjectRecord<String, String>) {
        val raw = record.value
        println("[lint] raw=${raw.take(LOG_PREVIEW_CHARS)}")

        val ev = om.readValue(raw, SnippetsFormattingRulesUpdated::class.java)
        val content = snippets.getContent(ev.snippetId)

        val res = exec.formatContent(
            FormatReq(
                language = ev.language,
                version = ev.version,
                content = content,
                configText = ev.configText,
                configFormat = ev.configFormat,
                options = ev.options,
            ),
        )
        snippets.saveFormatted(ev.snippetId, res.formattedContent)
    }
}
