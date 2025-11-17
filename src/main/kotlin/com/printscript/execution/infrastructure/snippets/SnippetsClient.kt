package com.printscript.execution.infrastructure.snippets

import io.printscript.contracts.DiagnosticDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientResponseException
import org.springframework.web.client.RestTemplate
import java.util.UUID

@Component
class SnippetsClient(@Value("\${snippets.base-url}") private val base: String, @Qualifier("m2mRestTemplate") private val rest: RestTemplate) {

    private val logger = LoggerFactory.getLogger(SnippetsClient::class.java)

    init {
        require(base.isNotBlank()) { "snippets.base-url is missing/blank" }
        logger.info("SnippetsClient base={}", base)
    }

    fun getContent(snippetId: UUID): String {
        val res = rest.getForEntity("$base/internal/snippets/$snippetId/content", ContentDto::class.java)
        return res.body?.content ?: error("content not found for snippet $snippetId")
    }

    fun saveFormatted(snippetId: UUID, formatted: String) {
        logger.info("Saving snippet $snippetId to $formatted")
        rest.postForEntity(
            "$base/internal/snippets/$snippetId/format",
            mapOf("content" to formatted),
            Void::class.java,
        )
    }

    fun saveLint(snippetId: UUID, violations: List<DiagnosticDto>) {
        logger.info("Calling /internal/snippets/{}/lint with {} violations", snippetId, violations.size)
        logger.info("Saving snippet $snippetId to $violations")
        try {
            val res = rest.postForEntity("$base/internal/snippets/$snippetId/lint", violations, Void::class.java)
            logger.info("saveLint status={} for {}", res.statusCode.value(), snippetId)
        } catch (e: RestClientResponseException) {
            logger.error("saveLint HTTP {} body={} for {}", e.statusCode.value(), e.responseBodyAsString, snippetId, e)
            throw e
        }
    }
}

data class ContentDto(val content: String)
