package com.printscript.execution.dto

import io.printscript.contracts.formatter.FormatterOptionsDto

sealed interface Request {
    val language: String
    val version: String
    val content: String
}

data class RunReq(override val language: String, override val version: String, override val content: String, val inputs: List<String>? = null) : Request

data class LintReq(override val language: String, override val version: String, override val content: String, val configText: String? = null, val configFormat: String? = null) : Request

data class ParseReq(override val language: String, override val version: String, override val content: String) : Request

data class FormatReq(override val language: String, override val version: String, override val content: String, val configText: String? = null, val configFormat: String? = null, val options: FormatterOptionsDto? = null) : Request

data class RunTestsReq(override val language: String, override val version: String, override val content: String, val testCases: List<RunTestCaseDto>, val options: RunTestsOptionsDto? = null) : Request

data class RunSingleTestReq(val language: String, val version: String, val content: String, val inputs: List<String> = emptyList(), val expectedOutputs: List<String> = emptyList(), val options: RunTestsOptionsDto? = null)
