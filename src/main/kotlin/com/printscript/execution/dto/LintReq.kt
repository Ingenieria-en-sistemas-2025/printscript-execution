package com.printscript.execution.dto

data class LintReq(val language: String, val version: String, val content: String, val rules: List<String>? = null)