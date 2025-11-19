package com.printscript.execution.domain

import io.printscript.contracts.DiagnosticDto

data class SingleTestResultDto(
    val index: Int,
    val status: String,
    val expected: List<String>? = null,
    val actual: List<String>? = null,
    val mismatchAt: Int? = null,
    val reason: String? = null,
    val diagnostic: DiagnosticDto? = null,
)
