package com.printscript.execution.domain.diagnostics

class ExecException(val diagnostic: ApiDiagnostic? = null, cause: Throwable? = null, msg: String? = null) : RuntimeException(msg ?: diagnostic?.message, cause)
