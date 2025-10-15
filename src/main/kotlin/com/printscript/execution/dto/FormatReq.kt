package com.printscript.execution.dto

data class FormatReq(val language: String, val version: String, val content: String, val rules: List<String>? = null, val indentSpaces: Int? = null) // ver de mappear a formatted options
