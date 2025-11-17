package com.printscript.execution.domain.language

import com.printscript.execution.domain.diagnostics.ExecException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DefaultLanguageRunnerRegistry(private val runners: List<LanguageRunnerPort>) : LanguageRunnerRegistry {

    private val logger = LoggerFactory.getLogger(DefaultLanguageRunnerRegistry::class.java)

    private val registry: Map<String, LanguageRunnerPort> =
        runners.associateBy { it.language.lowercase() }

    init {
        logger.info("Loaded language runners: ${registry.keys}")
    }

    override fun runnerFor(language: String): LanguageRunnerPort = registry[language.lowercase()]
        ?: throw ExecException(
            msg = "Unsupported language: $language",
        )
}
