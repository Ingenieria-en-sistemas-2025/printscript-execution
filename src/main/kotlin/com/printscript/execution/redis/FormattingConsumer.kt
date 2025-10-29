package com.printscript.execution.redis

import com.printscript.execution.dto.FormatReq
import com.printscript.execution.service.ExecutionService
import org.austral.ingsis.redis.RedisStreamConsumer
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.connection.stream.StreamRecords
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.stream.StreamReceiver
import org.springframework.stereotype.Component
import java.time.Duration

private const val MAX_ATTEMPTS = 3
@Component
class FormattingConsumer(
    private val redisJson: RedisTemplate<String, Any>,          // para publicar
    redisStr: RedisTemplate<String, String>,                  // para pasar a super
    @Value("\${streams.formatting.key}") streamKey: String,
    @Value("\${streams.formatting.group}") groupId: String,
    private val exec: ExecutionService,
    private val snippets: SnippetsClient,
    @Value("\${streams.dlq.formatting}") private val dlqKey: String
    ): RedisStreamConsumer<SnippetFormattingRulesUpdated>(streamKey, groupId, redisStr) {

    override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, SnippetFormattingRulesUpdated>> =
        StreamReceiver.StreamReceiverOptions.builder()
            .pollTimeout(Duration.ofSeconds(10))
            .targetType(SnippetFormattingRulesUpdated::class.java)
            .build()

    override fun onMessage(record: ObjectRecord<String, SnippetFormattingRulesUpdated>) {
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
                    options = event.options
                )
            )
            snippets.saveFormatted(event.snippetId, res.formattedContent)
        } catch (e: Exception) {
            retryOrDlq(event)
        }
    }

    private fun retryOrDlq(ev: SnippetFormattingRulesUpdated) {
        if (ev.attempt + 1 <= MAX_ATTEMPTS) {
            val next = ev.copy(attempt = ev.attempt + 1)
            redisJson.opsForStream<String, Any>()
                .add(StreamRecords.newRecord().ofObject(next).withStreamKey(streamKey))
        } else {
            redisJson.opsForStream<String, Any>()
                .add(StreamRecords.newRecord().ofObject(ev).withStreamKey(dlqKey))
        }
    }
}