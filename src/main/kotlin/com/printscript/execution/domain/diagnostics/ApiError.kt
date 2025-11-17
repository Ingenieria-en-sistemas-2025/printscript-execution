package com.printscript.execution.domain.diagnostics

data class ApiError(val error: String, val diagnostic: ApiDiagnostic? = null)
