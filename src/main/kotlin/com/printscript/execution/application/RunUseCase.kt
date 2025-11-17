package com.printscript.execution.application

import com.printscript.execution.domain.language.LanguageRunnerRegistry
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import org.springframework.stereotype.Service

@Service
class RunUseCase(private val runners: LanguageRunnerRegistry) {

    fun execute(req: RunReq): RunRes {
        val runner = runners.runnerFor(req.language)
        val res = runner.execute(
            language = req.language,
            version = req.version,
            content = req.content,
            inputs = req.inputs ?: emptyList(),
        )
        return RunRes(outputs = res.outputs)
    }
}
