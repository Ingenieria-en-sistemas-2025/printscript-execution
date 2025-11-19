package com.printscript.execution.application

import com.printscript.execution.domain.language.LanguageRunnerRegistry
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FormatUseCase(private val runners: LanguageRunnerRegistry) {

    private val logger = LoggerFactory.getLogger(FormatUseCase::class.java)

    fun format(req: FormatReq): FormatRes {
        logger.info(
            "FormatUseCase.format: lang={} version={} contentLen={} hasConfig={}",
            req.language,
            req.version,
            req.content.length,
            !req.configText.isNullOrBlank(),
        )

        val runner = runners.runnerFor(req.language)
        val formatted = runner.format(
            language = req.language,
            version = req.version,
            content = req.content,
            configText = req.configText,
        )

        logger.info(
            "FormatUseCase.format: formattedLen={}",
            formatted.length,
        )

        return FormatRes(formattedContent = formatted)
    }
}
