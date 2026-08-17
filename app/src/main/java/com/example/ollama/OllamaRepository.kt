// © 2026 Pablo Daniel de Luca - Ink 318 Software. Todos los derechos reservados.
// DNI: 31.649.936
// Este archivo es propiedad exclusiva de Pablo Daniel de Luca / Ink 318 Software.
// Queda prohibida su reproducción, distribución, modificación, venta o uso total o parcial sin autorización expresa y por escrito del titular.

package com.example.ollama

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

enum class OllamaConnectionState {
    CONNECTED,
    DISCONNECTED,
    CHECKING
}

/**
 * Local Repository to communicate with the offline Ollama / Dosama engine.
 * Supports token-by-token streaming responses via Kotlin Flow and model installation.
 */
class OllamaRepository(
    var baseUrl: String = "http://10.0.2.2:11434/",
    var selectedModel: String = "dosama"
) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val generateAdapter = moshi.adapter(OllamaGenerateResponse::class.java)
    private val pullAdapter = moshi.adapter(OllamaPullResponse::class.java)

    private var service: OllamaService = buildService(baseUrl)

    fun updateServerConfig(url: String, modelName: String) {
        var formattedUrl = url.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "http://$formattedUrl"
        }
        if (!formattedUrl.endsWith("/")) {
            formattedUrl = "$formattedUrl/"
        }
        baseUrl = formattedUrl
        if (modelName.isNotBlank()) {
            selectedModel = modelName.trim()
        }
        service = buildService(baseUrl)
    }

    private fun buildService(url: String): OllamaService {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OllamaService::class.java)
    }

    /**
     * Streams response tokens from local model line-by-line (NDJSON format)
     */
    fun generateStream(
        prompt: String,
        systemPrompt: String = "Eres CyberIA (Ink318 Autónomo 3.18), un asistente virtual soberano totalmente local, privado y fuera de línea."
    ): Flow<String> = flow {
        try {
            val req = OllamaGenerateRequest(
                model = selectedModel,
                prompt = prompt,
                system = systemPrompt,
                stream = true
            )

            val response = service.generateStream(req)
            if (!response.isSuccessful || response.body() == null) {
                val errCode = response.code()
                emit("Error local Ollama (HTTP $errCode). Verifica que el modelo '$selectedModel' esté activo.")
                return@flow
            }

            val body: ResponseBody = response.body()!!
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String? = reader.readLine()

            while (line != null) {
                if (line.isNotBlank()) {
                    try {
                        val parsed = generateAdapter.fromJson(line)
                        parsed?.response?.let { token ->
                            emit(token)
                        }
                    } catch (e: Exception) {
                        // Skip malformed chunk
                    }
                }
                line = reader.readLine()
            }
        } catch (e: ConnectException) {
            emit("[ERROR] No se pudo conectar al motor local Ollama ($baseUrl). Asegúrate de tener Ollama corriendo localmente sin internet.")
        } catch (e: SocketTimeoutException) {
            emit("[ERROR] Tiempo de espera agotado al consultar el modelo local $selectedModel.")
        } catch (e: Exception) {
            emit("[ERROR LOCAL] ${e.localizedMessage ?: "Fallo de conexión local"}")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Non-streaming single call fallback
     */
    suspend fun generateSingle(
        prompt: String,
        systemPrompt: String = "Eres CyberIA local offline."
    ): OllamaResult {
        return try {
            val req = OllamaGenerateRequest(
                model = selectedModel,
                prompt = prompt,
                system = systemPrompt,
                stream = false
            )
            val resp = service.generate(req)
            if (resp.isSuccessful && resp.body() != null) {
                val text = resp.body()?.response
                if (!text.isNullOrBlank()) {
                    OllamaResult.Success(text.trim())
                } else {
                    OllamaResult.Error("Respuesta vacía del modelo local.")
                }
            } else {
                OllamaResult.Error("Error ${resp.code()} al conectar con Ollama local.")
            }
        } catch (e: Exception) {
            OllamaResult.Error("Fallo de conexión local: ${e.message}")
        }
    }

    /**
     * Downloads/pulls a local model (e.g. 'dosama' or 'autono318-coder') directly from local storage/server
     */
    fun pullModel(modelName: String): Flow<OllamaPullResponse> = flow {
        try {
            val req = OllamaPullRequest(name = modelName, stream = true)
            val resp = service.pullModelStream(req)
            if (!resp.isSuccessful || resp.body() == null) {
                emit(OllamaPullResponse(error = "HTTP ${resp.code()}: No se pudo descargar el modelo $modelName"))
                return@flow
            }

            val body = resp.body()!!
            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            var line: String? = reader.readLine()

            while (line != null) {
                if (line.isNotBlank()) {
                    try {
                        val statusObj = pullAdapter.fromJson(line)
                        if (statusObj != null) {
                            emit(statusObj)
                        }
                    } catch (e: Exception) {
                        // ignore chunk error
                    }
                }
                line = reader.readLine()
            }
        } catch (e: Exception) {
            emit(OllamaPullResponse(error = "Fallo al bajar modelo local: ${e.localizedMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Checks if the local Ollama daemon is reachable
     */
    suspend fun checkConnection(): OllamaConnectionState {
        return try {
            val resp = service.getTags()
            if (resp.isSuccessful) OllamaConnectionState.CONNECTED else OllamaConnectionState.DISCONNECTED
        } catch (e: Exception) {
            OllamaConnectionState.DISCONNECTED
        }
    }

    /**
     * Gets available local models installed in the local Ollama instance
     */
    suspend fun getInstalledModels(): List<String> {
        return try {
            val resp = service.getTags()
            if (resp.isSuccessful) {
                resp.body()?.models?.map { it.name } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
