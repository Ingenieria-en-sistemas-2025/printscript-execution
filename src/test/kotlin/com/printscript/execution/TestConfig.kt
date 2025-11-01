package com.printscript.execution

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.web.client.RestTemplate

@TestConfiguration
class TestConfig {

    @Bean
    @Lazy
    fun restTemplate(): RestTemplate = RestTemplate()
}
