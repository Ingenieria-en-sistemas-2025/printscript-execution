package com.printscript.execution.error

data class ApiError(val error: String, val diagnostic: ApiDiagnostic? = null)
