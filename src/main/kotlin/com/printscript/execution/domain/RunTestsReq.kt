package com.printscript.execution.domain

import io.printscript.contracts.Request
import io.printscript.contracts.tests.RunTestsOptionsDto

data class RunTestsReq(override val language: String, override val version: String, override val content: String, val testCases: List<RunTestCaseDto>, val options: RunTestsOptionsDto? = null) :
    Request
