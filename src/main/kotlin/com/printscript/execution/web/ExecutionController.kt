package com.printscript.execution.web

import com.printscript.execution.application.ExecutionService
import io.printscript.contracts.formatter.FormatReq
import io.printscript.contracts.formatter.FormatRes
import io.printscript.contracts.linting.LintReq
import io.printscript.contracts.linting.LintRes
import io.printscript.contracts.parse.ParseReq
import io.printscript.contracts.parse.ParseRes
import io.printscript.contracts.run.RunReq
import io.printscript.contracts.run.RunRes
import io.printscript.contracts.tests.RunSingleTestReq
import io.printscript.contracts.tests.RunSingleTestRes
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class ExecutionController(private val service: ExecutionService) {

    private val logger = LoggerFactory.getLogger(ExecutionController::class.java)

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Void> {
        logger.info("Ping received")
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/parse")
    fun parse(@RequestBody req: ParseReq): ParseRes {
        logger.info(
            "POST /parse lang={} version={} contentLen={}",
            req.language,
            req.version,
            req.content.length,
        )
        val res = service.parse(req)
        logger.info("POST /parse result valid={} diagnostics={}", res.valid, res.diagnostics.size)
        return res
    }

    @PostMapping("/lint")
    fun lint(@RequestBody req: LintReq): LintRes {
        logger.info(
            "POST /lint lang={} version={} contentLen={}",
            req.language,
            req.version,
            req.content.length,
        )
        val res = service.lint(req)
        logger.info("POST /lint violations={}", res.violations.size)
        return res
    }

    @PostMapping("/format")
    fun formatContent(@RequestBody req: FormatReq): FormatRes {
        logger.info(
            "POST /format lang={} version={} contentLen={} hasConfig={}",
            req.language,
            req.version,
            req.content.length,
            !req.configText.isNullOrBlank(),
        )
        val res = service.formatContent(req)
        logger.info("POST /format formattedLen={}", res.formattedContent.length)
        return res
    }

    @PostMapping("/run")
    fun execute(@RequestBody req: RunReq): RunRes {
        logger.info(
            "POST /run lang={} version={} contentLen={} inputs={}",
            req.language,
            req.version,
            req.content.length,
            req.inputs?.size ?: 0,
        )
        val res = service.execute(req)
        logger.info("POST /run outputs={}", res.outputs.size)
        return res
    }

    @PostMapping("/run-test")
    fun runTest(@RequestBody req: RunSingleTestReq): RunSingleTestRes {
        logger.info(
            "POST /run-test lang={} version={} contentLen={} inputs={} expectedOutputs={}",
            req.language,
            req.version,
            req.content.length,
            req.inputs.size,
            req.expectedOutputs.size,
        )
        val res = service.runSingleTest(req)
        logger.info(
            "POST /run-test status={} mismatchAt={} actualSize={}",
            res.status,
            res.mismatchAt,
            res.actual?.size ?: 0,
        )
        return res
    }
}
