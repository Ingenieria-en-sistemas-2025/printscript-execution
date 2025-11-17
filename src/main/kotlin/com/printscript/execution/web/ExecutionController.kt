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
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class ExecutionController(private val service: ExecutionService) {

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Void> = ResponseEntity.noContent().build()

    @PostMapping("/parse")
    fun parse(@RequestBody req: ParseReq): ParseRes = service.parse(req)

    @PostMapping("/lint")
    fun lint(@RequestBody req: LintReq): LintRes = service.lint(req)

    @PostMapping("/format")
    fun formatContent(@RequestBody req: FormatReq): FormatRes = service.formatContent(req)

    @PostMapping("/run")
    fun execute(@RequestBody req: RunReq): RunRes = service.execute(req)

    @PostMapping("/run-test")
    fun runTest(@RequestBody req: RunSingleTestReq): RunSingleTestRes = service.runSingleTest(req)
}
