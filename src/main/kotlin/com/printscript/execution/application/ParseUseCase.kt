package com.printscript.execution.application

import com.printscript.execution.domain.language.LanguageRunnerRegistry
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.parse.ParseRes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ParseUseCase(private val runners: LanguageRunnerRegistry) {

    private val logger = LoggerFactory.getLogger(ParseUseCase::class.java)

    fun parse(req: ParseReq): ParseRes {
        val runner = runners.runnerFor(req.language)
        val diags = runner.validate(
            language = req.language,
            version = req.version,
            content = req.content,
        )
        return ParseRes(valid = diags.isEmpty(), diagnostics = diags)
    }
}
