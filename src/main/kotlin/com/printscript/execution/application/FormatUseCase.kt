package com.printscript.execution.application

import com.printscript.execution.domain.language.LanguageRunnerRegistry
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import org.springframework.stereotype.Service

@Service
class FormatUseCase(private val runners: LanguageRunnerRegistry) {

    fun format(req: FormatReq): FormatRes {
        val runner = runners.runnerFor(req.language)
        val formatted = runner.format(
            language = req.language,
            version = req.version,
            content = req.content,
            configText = req.configText,
        )
        return FormatRes(formattedContent = formatted)
    }
}
