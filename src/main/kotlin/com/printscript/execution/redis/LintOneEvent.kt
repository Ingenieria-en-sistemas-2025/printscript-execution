package com.printscript.execution.redis

import com.printscript.execution.dto.FormatterOptionsDto

data class LintOneEvent(val correlationalId: String, val snippetId: Long, val language: String, val version: String, val configText: String?, val configFormat: String?, val attempt: Int = 0, val createdAt: Long = System.currentTimeMillis())