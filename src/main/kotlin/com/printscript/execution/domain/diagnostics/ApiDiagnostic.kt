package com.printscript.execution.domain.diagnostics

data class ApiDiagnostic(val code: String, val message: String, val line: Int, val column: Int)
