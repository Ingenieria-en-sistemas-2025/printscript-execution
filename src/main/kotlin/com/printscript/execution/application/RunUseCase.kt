package com.printscript.execution.application

import com.printscript.execution.domain.language.LanguageRunnerRegistry
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RunUseCase(private val runners: LanguageRunnerRegistry) {

    private val logger = LoggerFactory.getLogger(RunUseCase::class.java)

    fun execute(req: RunReq): RunRes {
        logger.info(
            "RunUseCase.execute: lang={} version={} contentLen={} inputs={}",
            req.language,
            req.version,
            req.content.length,
            req.inputs?.size ?: 0,
        )

        val runner = runners.runnerFor(req.language)
        val res = runner.execute(
            language = req.language,
            version = req.version,
            content = req.content,
            inputs = req.inputs ?: emptyList(),
        )

        logger.info(
            "RunUseCase.execute: finished outputs={}",
            res.outputs.size,
        )
        return RunRes(outputs = res.outputs)
    }
}
