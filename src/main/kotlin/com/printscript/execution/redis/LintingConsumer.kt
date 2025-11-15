package com.printscript.execution.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.printscript.execution.service.ExecutionService
import io.printscript.contracts.events.LintingRulesUpdated
import io.printscript.contracts.linting.LintReq
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
private const val LOGS = 500

@Component
@ConditionalOnProperty(prefix = "streams", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class LintingConsumer(@Value("\${streams.linting.key}") rawStreamKey: String, @Value("\${streams.linting.group}") rawGroup: String, private val om: ObjectMapper, private val exec: ExecutionService, private val snippets: SnippetsClient, @Qualifier("stringTemplate") redis: RedisTemplate<String, String>) :
    RedisStreamConsumer<String>(
        streamKey = rawStreamKey.trim().trim('"', '\''),
        groupId = rawGroup.trim().trim('"', '\''),
        redis = redis,
    ) {

    @PostConstruct
    fun started() {
        println("[LintingConsumer] stream=$streamKey group=$groupId READY")
    }

    override fun options(): StreamReceiver.StreamReceiverOptions<String, ObjectRecord<String, String>> = StreamReceiver.StreamReceiverOptions.builder()
        .pollTimeout(Duration.ofSeconds(POLL_TIMEOUT_SECONDS))
        .targetType(String::class.java)
        .build()

    @Suppress("TooGenericExceptionCaught")
    override fun onMessage(record: ObjectRecord<String, String>) {
        val raw = record.value
        println("[lint] raw=${raw.take(LOG_PREVIEW_CHARS)}")

        val ev: LintingRulesUpdated = try {
            om.readValue(raw, LintingRulesUpdated::class.java)
        } catch (e: Exception) {
            println("[lint][DESER ERROR] ${e::class.java.name}: ${e.message}")
            return
        }

        println("[lint] ev.ok id=${ev.snippetId} corr=${ev.correlationalId}")

        try {
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
            println("[lint] saved ${res.violations.size} violations for ${ev.snippetId}")
        } catch (e: org.springframework.web.client.RestClientResponseException) {
            println("[lint][HTTP body] ${e.responseBodyAsString?.take(LOGS)}")
        } catch (e: org.springframework.web.client.ResourceAccessException) {
            println("[lint][NETWORK] ${e::class.java.simpleName}: ${e.message}")
        } catch (e: Exception) {
            println("[lint][UNEXPECTED] ${e::class.java.name}: ${e.message}")
        }
    }
}
