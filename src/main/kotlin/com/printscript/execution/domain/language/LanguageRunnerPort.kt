package com.printscript.execution.domain.language

import io.printscript.contracts.DiagnosticDto
import io.printscript.contracts.run.RunRes

interface LanguageRunnerPort {

    val language: String

    fun validate(language: String, version: String, content: String): List<DiagnosticDto>

    fun lint(language: String, version: String, content: String, configText: String?): List<DiagnosticDto>

    fun format(
        language: String,
        version: String,
        content: String,
        configText: String?, // JSON/YAML,..
    ): String

    fun execute(language: String, version: String, content: String, inputs: List<String>): RunRes
}
