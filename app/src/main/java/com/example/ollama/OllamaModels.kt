// © 2026 Pablo Daniel de Luca - Ink 318 Software. Todos los derechos reservados.
// DNI: 31.649.936
// Este archivo es propiedad exclusiva de Pablo Daniel de Luca / Ink 318 Software.
// Queda prohibida su reproducción, distribución, modificación, venta o uso total o parcial sin autorización expresa y por escrito del titular.

package com.example.ollama

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OllamaGenerateRequest(
    @Json(name = "model") val model: String,
    @Json(name = "prompt") val prompt: String,
    @Json(name = "stream") val stream: Boolean = false,
    @Json(name = "system") val system: String? = null
)

@JsonClass(generateAdapter = true)
data class OllamaGenerateResponse(
    @Json(name = "model") val model: String? = null,
    @Json(name = "response") val response: String? = null,
    @Json(name = "done") val done: Boolean? = true,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class OllamaChatMessage(
    @Json(name = "role") val role: String, // "system", "user", "assistant"
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class OllamaChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<OllamaChatMessage>,
    @Json(name = "stream") val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OllamaChatResponse(
    @Json(name = "model") val model: String? = null,
    @Json(name = "message") val message: OllamaChatMessage? = null,
    @Json(name = "done") val done: Boolean? = true,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class OllamaModelDetails(
    @Json(name = "name") val name: String
)

@JsonClass(generateAdapter = true)
data class OllamaTagsResponse(
    @Json(name = "models") val models: List<OllamaModelDetails>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class OllamaPullRequest(
    @Json(name = "name") val name: String,
    @Json(name = "stream") val stream: Boolean = true
)

@JsonClass(generateAdapter = true)
data class OllamaPullResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "digest") val digest: String? = null,
    @Json(name = "total") val total: Long? = null,
    @Json(name = "completed") val completed: Long? = null,
    @Json(name = "error") val error: String? = null
)

sealed class OllamaResult {
    data class Success(val response: String) : OllamaResult()
    data class Error(val userMessage: String, val technicalError: String? = null) : OllamaResult()
}
