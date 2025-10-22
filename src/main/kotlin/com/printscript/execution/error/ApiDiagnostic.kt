package com.printscript.execution.error

data class ApiDiagnostic(val code: String, val message: String, val line: Int, val column: Int)
