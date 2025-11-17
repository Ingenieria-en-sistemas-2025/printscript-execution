package com.printscript.execution.application

import com.printscript.execution.domain.language.LanguageRunnerRegistry
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import org.springframework.stereotype.Service

@Service
class LintUseCase(private val runners: LanguageRunnerRegistry) {

    fun lint(req: LintReq): LintRes {
        val runner = runners.runnerFor(req.language)
        val diagnostics = runner.lint(
            language = req.language,
            version = req.version,
            content = req.content,
            configText = req.configText,
        )
        return LintRes(violations = diagnostics)
    }
}
