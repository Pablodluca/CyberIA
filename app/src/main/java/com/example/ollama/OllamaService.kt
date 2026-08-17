// © 2026 Pablo Daniel de Luca - Ink 318 Software. Todos los derechos reservados.
// DNI: 31.649.936
// Este archivo es propiedad exclusiva de Pablo Daniel de Luca / Ink 318 Software.
// Queda prohibida su reproducción, distribución, modificación, venta o uso total o parcial sin autorización expresa y por escrito del titular.

package com.example.ollama

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

/**
 * Ink 318 Software - Interfaz de Servicio Retrofit para comunicación con Ollama API
 */
interface OllamaService {

    @POST("api/generate")
    suspend fun generate(
        @Body request: OllamaGenerateRequest
    ): Response<OllamaGenerateResponse>

    @Streaming
    @POST("api/generate")
    suspend fun generateStream(
        @Body request: OllamaGenerateRequest
    ): Response<ResponseBody>

    @POST("api/chat")
    suspend fun chat(
        @Body request: OllamaChatRequest
    ): Response<OllamaChatResponse>

    @Streaming
    @POST("api/chat")
    suspend fun chatStream(
        @Body request: OllamaChatRequest
    ): Response<ResponseBody>

    @Streaming
    @POST("api/pull")
    suspend fun pullModelStream(
        @Body request: OllamaPullRequest
    ): Response<ResponseBody>

    @GET("api/tags")
    suspend fun getTags(): Response<OllamaTagsResponse>
}
