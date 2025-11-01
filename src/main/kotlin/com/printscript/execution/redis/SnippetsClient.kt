package com.printscript.execution.redis

import com.printscript.execution.dto.DiagnosticDto
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class SnippetsClient(@Value("\${snippets.base-url}") private val base: String, @Qualifier("m2mRestTemplate") private val rest: RestTemplate) {

    fun getContent(snippetId: UUID): String {
        val res: ResponseEntity<ContentDto> = rest.getForEntity("$base/internal/snippets/$snippetId/content", ContentDto::class.java)
        return res.body?.content
            ?: error("content not found for snippet $snippetId")
    }

    fun saveFormatted(snippetId: UUID, formatted: String) {
        rest.postForEntity(
            "$base/internal/snippets/$snippetId/format",
            mapOf("content" to formatted),
            Void::class.java,
        )
    }

    fun saveLint(snippetId: UUID, violations: List<DiagnosticDto>) {
        rest.postForEntity(
            "$base/internal/snippets/$snippetId/lint",
            violations,
            Void::class.java,
        )
    }
}

data class ContentDto(val content: String)
