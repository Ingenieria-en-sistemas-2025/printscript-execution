package com.printscript.execution.dto

data class RunReq(val language: String, val version: String, val content: String, val inputs: List<String>? = null)
