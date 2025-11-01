package com.printscript.execution.config

import com.printscript.execution.auth.Auth0TokenService // Asume esta ruta
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
class AppConfig(
    // Inyectamos el servicio M2M que obtiene y renueva el token
    private val auth0TokenService: Auth0TokenService,
) {
    // usado por Auth0TokenService para conseguir el token, pq no se tine que autenticar a si mismo
    // no necesita un token de autenticacion, pq lo esta pidiendo a /oauth/token
    @Bean("plainRestClient")
    fun plainRestClient(): RestClient = RestClient.create()

    @Bean("secureRestClient")
    fun secureRestClient(): RestClient = RestClient.builder()
        .requestInterceptor { request, body, execution ->
            // Obtiene el token
            val accessToken = auth0TokenService.getAccessToken()

            // Añadir el header de Autorizacion (Bearer Token)
            request.headers.add("Authorization", "Bearer $accessToken")

            // Ejecuta la peticion
            execution.execute(request, body)
        }
        .build()
}
