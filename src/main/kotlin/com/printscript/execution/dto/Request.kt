package com.printscript.execution.dto

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
