package com.printscript.execution.domain

data class RunTestsRes(val summary: SummaryDto, val results: List<SingleTestResultDto>)
