package com.printscript.execution.domain

data class RunTestCaseDto(val inputs: List<String> = emptyList(), val expectedOutputs: List<String> = emptyList())
