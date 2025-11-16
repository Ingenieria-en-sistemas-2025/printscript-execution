package com.printscript.execution.auth

import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class HttpClientConfig {

    companion object {
        private const val CONNECT_TIMEOUT_SECONDS = 5L // maximo para establecer conexion
        private const val READ_TIMEOUT_SECONDS = 30L // máximo para esperar respuesta
    }

    @Bean("m2mRestTemplate")
    fun m2mRestTemplate(tokenService: Auth0TokenService): RestTemplate {
        val authInterceptor = ClientHttpRequestInterceptor { req, body, exec ->
            val token = tokenService.getAccessToken()
            req.headers.setBearerAuth(token)
            exec.execute(req, body) // aca ejecuta la request real
        }

        return RestTemplateBuilder()
            .requestFactorySettings { settings ->
                settings
                    .withConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
                    .withReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
            }
            .additionalInterceptors(authInterceptor)
            .build() // devuelve el rest template listo para usar.
    }
}
