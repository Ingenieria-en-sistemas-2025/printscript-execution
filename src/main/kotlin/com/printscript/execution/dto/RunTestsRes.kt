package com.printscript.execution.dto

data class RunTestsRes(
    val summary: SummaryDto,
    val results: List<SingleTestResultDto>
)
