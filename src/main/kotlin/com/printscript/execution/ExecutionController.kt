package com.printscript.execution

import com.printscript.execution.dto.RunReq
import com.printscript.execution.dto.RunRes
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

//    @PostMapping("/parse")
//    fun parse(@RequestBody req: ParseReq) : ParseRes = service.parse(req)
//
//    @PostMapping("/lint")
//    fun lint(@RequestBody req: LintReq) : LintRes = service.lint(req)

//    @PostMapping("/format")
//    fun formatContent(@RequestBody req: FormatReq) : FormatRes = service.formatContent(req)

    @PostMapping("/run")
    fun execute(@RequestBody req: RunReq): RunRes = service.execute(req)
}
