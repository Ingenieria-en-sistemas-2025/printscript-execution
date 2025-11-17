package com.printscript.execution.domain.language

interface LanguageRunnerRegistry {
    fun runnerFor(language: String): LanguageRunnerPort
}
