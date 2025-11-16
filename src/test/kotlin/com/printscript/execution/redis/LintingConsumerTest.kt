package com.printscript.execution.redis

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.printscript.execution.service.ExecutionService
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.printscript.contracts.DiagnosticDto
import io.printscript.contracts.events.LintingRulesUpdated
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import org.springframework.data.redis.connection.stream.ObjectRecord
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import java.nio.charset.Charset
import java.util.UUID
import kotlin.test.Test

class LintingConsumerTest {

    private fun createConsumer(exec: ExecutionService = mockk(), snippets: SnippetsClient = mockk()): LintingConsumer {
        val om = ObjectMapper().registerKotlinModule()
        val redis = mockk<RedisTemplate<String, String>>()
        return LintingConsumer(
            rawStreamKey = "ps.linting",
            rawGroup = "linting-workers",
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

        val record = ObjectRecord.create("ps.linting", "{not-json")

        consumer.onMessage(record)

        verify { exec wasNot Called }
        verify { snippets wasNot Called }
    }

    @Test
    fun `onMessage procesa evento valido y guarda lint`() {
        val exec = mockk<ExecutionService>()
        val snippets = mockk<SnippetsClient>()

        val consumer = createConsumer(exec, snippets)
        val om = ObjectMapper().registerKotlinModule()

        val snippetId = UUID.randomUUID()

        val ev = LintingRulesUpdated(
            correlationalId = "corr-1",
            snippetId = snippetId,
            language = "printscript",
            version = "1.1",
            configText = null,
            configFormat = null,
        )
        val json = om.writeValueAsString(ev)

        every { snippets.getContent(snippetId) } returns "let a: number = 1;"
        every { exec.lint(any<LintReq>()) } returns LintRes(violations = listOf(DiagnosticDto("R1", "msg", 1, 1)))
        every { snippets.saveLint(snippetId, any()) } just Runs

        val record = ObjectRecord.create("ps.linting", json)

        consumer.onMessage(record)

        verify(exactly = 1) { snippets.getContent(snippetId) }
        verify(exactly = 1) { exec.lint(any<LintReq>()) }
        verify(exactly = 1) { snippets.saveLint(snippetId, match { it.size == 1 }) }
    }

    @Test
    fun `onMessage maneja RestClientResponseException sin explotar`() {
        val exec = mockk<ExecutionService>()
        val snippets = mockk<SnippetsClient>()
        val consumer = createConsumer(exec, snippets)
        val om = ObjectMapper().registerKotlinModule()
        val snippetId = UUID.randomUUID()

        val ev = LintingRulesUpdated(
            correlationalId = "corr-2",
            snippetId = snippetId,
            language = "printscript",
            version = "1.1",
            configText = null,
            configFormat = null,
        )
        val json = om.writeValueAsString(ev)

        every { snippets.getContent(snippetId) } returns "let a: number = 1;"

        val ex = object : RestClientResponseException(
            "bad request",
            400,
            "Bad Request",
            null,
            ByteArray(0),
            Charset.defaultCharset(),
        ) {}

        every { exec.lint(any<LintReq>()) } throws ex

        val record = ObjectRecord.create("ps.linting", json)

        consumer.onMessage(record)
    }

    @Test
    fun `onMessage maneja ResourceAccessException sin explotar`() {
        val exec = mockk<ExecutionService>()
        val snippets = mockk<SnippetsClient>()
        val consumer = createConsumer(exec, snippets)
        val om = ObjectMapper().registerKotlinModule()
        val snippetId = UUID.randomUUID()

        val ev = LintingRulesUpdated(
            correlationalId = "corr-3",
            snippetId = snippetId,
            language = "printscript",
            version = "1.1",
            configText = null,
            configFormat = null,
        )
        val json = om.writeValueAsString(ev)

        every { snippets.getContent(snippetId) } returns "let a: number = 1;"
        every { exec.lint(any<LintReq>()) } throws ResourceAccessException("timeout")

        val record = ObjectRecord.create("ps.linting", json)

        consumer.onMessage(record)
    }

    @Test
    fun `onMessage maneja excepcion inesperada sin explotar`() {
        val exec = mockk<ExecutionService>()
        val snippets = mockk<SnippetsClient>()
        val consumer = createConsumer(exec, snippets)
        val om = ObjectMapper().registerKotlinModule()
        val snippetId = UUID.randomUUID()

        val ev = LintingRulesUpdated(
            correlationalId = "corr-4",
            snippetId = snippetId,
            language = "printscript",
            version = "1.1",
            configText = null,
            configFormat = null,
        )
        val json = om.writeValueAsString(ev)

        every { snippets.getContent(snippetId) } returns "let a: number = 1;"
        every { exec.lint(any<LintReq>()) } throws RuntimeException("boom")

        val record = ObjectRecord.create("ps.linting", json)

        consumer.onMessage(record)
    }
}
