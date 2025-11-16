package com.printscript.execution.snippets

import com.printscript.execution.redis.ContentDto
import com.printscript.execution.redis.SnippetsClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.printscript.contracts.DiagnosticDto
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class SnippetsClientTest {

    private val baseUrl = "http://snippets-service"

    @Test
    fun `getContent devuelve el content del dto`() {
        val rest = mockk<RestTemplate>()
        val snippetId = UUID.randomUUID()
        val dto = ContentDto("let a: number = 1;")

        every {
            rest.getForEntity("$baseUrl/internal/snippets/$snippetId/content", ContentDto::class.java)
        } returns ResponseEntity.ok(dto)

        val client = SnippetsClient(baseUrl, rest)

        val content = client.getContent(snippetId)

        assertEquals("let a: number = 1;", content)
    }

    @Test
    fun `getContent tira error si el body viene null`() {
        val rest = mockk<RestTemplate>()
        val snippetId = UUID.randomUUID()

        every {
            rest.getForEntity("$baseUrl/internal/snippets/$snippetId/content", ContentDto::class.java)
        } returns ResponseEntity.ok(null)

        val client = SnippetsClient(baseUrl, rest)

        assertThrows<IllegalStateException> {
            client.getContent(snippetId)
        }
    }

    @Test
    fun `saveFormatted llama al endpoint de format`() {
        val rest = mockk<RestTemplate>(relaxed = true)
        val client = SnippetsClient(baseUrl, rest)
        val snippetId = UUID.randomUUID()

        client.saveFormatted(snippetId, "formatted-code")

        verify {
            rest.postForEntity( // si no se llamó a ese postForEntity, el test falla.
                "$baseUrl/internal/snippets/$snippetId/format",
                mapOf("content" to "formatted-code"),
                Void::class.java,
            )
        }
    }

    @Test
    fun `saveLint loguea y relanza RestClientResponseException`() {
        val rest = mockk<RestTemplate>()
        val client = SnippetsClient(baseUrl, rest)
        val snippetId = UUID.randomUUID()
        val violations = listOf(
            DiagnosticDto("RULE", "msg", 1, 1),
        )

        val ex = RestClientResponseException(
            "Boom",
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            null,
            "body".toByteArray(StandardCharsets.UTF_8),
            StandardCharsets.UTF_8,
        )

        every {
            rest.postForEntity("$baseUrl/internal/snippets/$snippetId/lint", violations, Void::class.java)
        } throws ex

        assertThrows<RestClientResponseException> {
            client.saveLint(snippetId, violations)
        } // verifica que save lint propague el error
    }
}
