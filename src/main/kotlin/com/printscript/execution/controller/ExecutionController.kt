package com.printscript.execution.controller

import com.printscript.execution.dto.FormatReq
import com.printscript.execution.dto.FormatRes
import com.printscript.execution.dto.LintReq
import com.printscript.execution.dto.LintRes
import com.printscript.execution.dto.ParseReq
import com.printscript.execution.dto.ParseRes
import com.printscript.execution.dto.RunReq
import com.printscript.execution.dto.RunRes
import com.printscript.execution.dto.RunTestsReq
import com.printscript.execution.dto.RunTestsRes
import com.printscript.execution.service.ExecutionServiceImpl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class ExecutionController(private val service: ExecutionServiceImpl) {

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

    @PostMapping("/run-tests")
    fun runTests(@RequestBody req: RunTestsReq): RunTestsRes = service.runTests(req)
}
