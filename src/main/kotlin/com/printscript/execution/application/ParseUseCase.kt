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
        logger.info(
            "ParseUseCase.parse: lang={} version={} contentLen={}",
            req.language,
            req.version,
            req.content.length,
        )

        val runner = runners.runnerFor(req.language)
        val diags = runner.validate(
            language = req.language,
            version = req.version,
            content = req.content,
        )

        logger.info(
            "ParseUseCase.parse: diagnostics={}",
            diags.size,
        )

        return ParseRes(valid = diags.isEmpty(), diagnostics = diags)
    }
}
