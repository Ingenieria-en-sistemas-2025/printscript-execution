package com.printscript.execution.application

import com.printscript.execution.domain.language.LanguageRunnerRegistry
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class LintUseCase(private val runners: LanguageRunnerRegistry) {

    private val logger = LoggerFactory.getLogger(LintUseCase::class.java)

    fun lint(req: LintReq): LintRes {
        logger.info(
            "LintUseCase.format: lang={} version={} contentLen={} hasConfig={}",
            req.language,
            req.version,
            req.content.length,
            !req.configText.isNullOrBlank(),
        )

        val runner = runners.runnerFor(req.language)
        val diagnostics = runner.lint(
            language = req.language,
            version = req.version,
            content = req.content,
            configText = req.configText,
        )

        logger.info(
            "LintUseCase.diagnositcs: diagnosticLength={}",
            diagnostics.size,
        )
        return LintRes(violations = diagnostics)
    }
}
