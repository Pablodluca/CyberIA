/*
 * Copyright (c) 2026 Pablo Daniel De Luca
 * Ink 318 Software
 * DNI: 31.649.936
 * Todos los derechos reservados.
 */
package com.example.ollama

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

interface OllamaApi {

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
