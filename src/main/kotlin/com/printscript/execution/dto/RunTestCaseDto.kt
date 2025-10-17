package com.printscript.execution.dto

data class RunTestCaseDto(val inputs: List<String> = emptyList(), val expectedOutputs: List<String> = emptyList())
