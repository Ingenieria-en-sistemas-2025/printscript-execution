package com.printscript.execution.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.printscript.execution.application.ExecutionService
import com.printscript.execution.infrastructure.redis.FormattingConsumer
import com.printscript.execution.infrastructure.snippets.SnippetsClient
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.printscript.contracts.events.FormattingRulesUpdated
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.RedisTemplate
import java.util.UUID
import kotlin.test.Test

class FormattingConsumerTest {

    private val om = jacksonObjectMapper().findAndRegisterModules()

    private fun createConsumer(exec: ExecutionService = mockk(), snippets: SnippetsClient = mockk()): FormattingConsumer {
        val om = om
        val redis = mockk<RedisTemplate<String, String>>()
        return FormattingConsumer(
            rawStreamKey = "ps.formatting",
            rawGroup = "formatting-workers",
            om = om,
            exec = exec,
            snippets = snippets,
            redis = redis,
        )
    }

    @Test
    fun `onMessage ignora mensajes con JSON invalido`() {
        val exec = mockk<ExecutionService>(relaxed = true)
        val snippets = mockk<SnippetsClient>(relaxed = true)
        val consumer = createConsumer(exec, snippets)

        val record = ObjectRecord.create("ps.formatting", "{broken-json")

        consumer.onMessage(record)

        verify { exec wasNot Called }
        verify { snippets wasNot Called }
    }

    @Test
    fun `onMessage procesa evento valido y guarda formatted`() {
        val exec = mockk<ExecutionService>()
        val snippets = mockk<SnippetsClient>()
        val om = om
        val redis = mockk<RedisTemplate<String, String>>()

        val consumer = FormattingConsumer(
            rawStreamKey = "ps.formatting",
            rawGroup = "formatting-workers",
            om = om,
            exec = exec,
            snippets = snippets,
            redis = redis,
        )

        val snippetId = UUID.randomUUID()
        val ev = FormattingRulesUpdated(
            correlationalId = "corr-123",
            snippetId = snippetId,
            language = "printscript",
            version = "1.1",
            configText = null,
            configFormat = null,
        )
        val json = om.writeValueAsString(ev)

        every { snippets.getContent(snippetId) } returns "let a:number=1;"
        every { exec.formatContent(any<FormatReq>()) } returns FormatRes("let a: number = 1;")
        every { snippets.saveFormatted(snippetId, "let a: number = 1;") } just Runs

        val record = ObjectRecord.create("ps.formatting", json)

        consumer.onMessage(record)

        verify(exactly = 1) { snippets.getContent(snippetId) }
        verify(exactly = 1) { exec.formatContent(any<FormatReq>()) }
        verify(exactly = 1) { snippets.saveFormatted(snippetId, "let a: number = 1;") }
    }
}
