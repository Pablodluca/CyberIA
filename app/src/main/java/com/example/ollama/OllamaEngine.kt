/*
 * Copyright (c) 2026 Pablo Daniel De Luca
 * Ink 318 Software
 * DNI: 31.649.936
 * Todos los derechos reservados.
 */
package com.example.ollama

import com.example.assistant.LocalCommandExecutor
import kotlinx.coroutines.flow.Flow

object OllamaEngine {

    val repository: OllamaRepository = OllamaRepository(
        baseUrl = "http://10.0.2.2:11434/",
        selectedModel = "dosama"
    )

    val serverUrl: String
        get() = repository.baseUrl

    val currentModel: String
        get() = repository.selectedModel

    fun updateConfig(newUrl: String, newModel: String) {
        repository.updateServerConfig(newUrl, newModel)
    }

    suspend fun queryOllama(
        userMessage: String,
        systemPrompt: String = "Eres CyberIA (Ink318 Autónomo 3.18), un asistente virtual soberano e inteligente desarrollado por Pablo Daniel De Luca para Ink 318 Software. Responde de forma clara, directa y eficiente."
    ): OllamaResult {
        return repository.generateSingle(userMessage, systemPrompt)
    }

    /**
     * Real-time streaming tokens flow from local model
     */
    fun queryOllamaStream(
        userMessage: String,
        systemPrompt: String = "Eres CyberIA (Ink318 Autónomo 3.18), un asistente virtual soberano e inteligente desarrollado por Pablo Daniel De Luca para Ink 318 Software. Responde de forma clara, directa y eficiente."
    ): Flow<String> {
        return repository.generateStream(userMessage, systemPrompt)
    }

    suspend fun checkConnection(): OllamaConnectionState {
        return repository.checkConnection()
    }

    /**
     * Download/pull model to run offline (e.g., 'dosama', 'autono318-coder', 'qwen2.5-coder:7b')
     */
    fun pullModelStream(modelName: String): Flow<OllamaPullResponse> {
        return repository.pullModel(modelName)
    }

    /**
     * Process user request:
     * 1. Check if it's an explicit Android OS or hardware action (LocalCommandExecutor)
     * 2. If conversational -> execute via local Ollama engine
     */
    suspend fun processUserRequest(
        inputText: String,
        onSystemCommand: (String) -> Unit = {}
    ): Pair<String, OllamaResult?> {
        val trimmed = inputText.trim()
        if (trimmed.isEmpty()) return Pair("Comando vacío.", null)

        val isSystemAction = isExplicitSystemCommand(trimmed)
        if (isSystemAction) {
            val systemResult = LocalCommandExecutor.execute(trimmed)
            onSystemCommand(systemResult)
            return Pair(systemResult, null)
        }

        val ollamaRes = repository.generateSingle(trimmed)
        return when (ollamaRes) {
            is OllamaResult.Success -> Pair(ollamaRes.response, ollamaRes)
            is OllamaResult.Error -> {
                // Fallback to local offline command executor
                val fallbackAnswer = LocalCommandExecutor.execute(trimmed)
                Pair(fallbackAnswer, ollamaRes)
            }
        }
    }

    fun isExplicitSystemCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower.startsWith("tocar ") ||
                lower.startsWith("touch ") ||
                lower.startsWith("crear ") ||
                lower.startsWith("create ") ||
                lower.startsWith("borrar ") ||
                lower.startsWith("delete ") ||
                lower.startsWith("bash ") ||
                lower.startsWith("sh ") ||
                lower.startsWith("abrir ") ||
                lower.startsWith("open ") ||
                lower == "inicio" || lower == "home" ||
                lower == "atras" || lower == "back" ||
                lower == "recientes" || lower == "recents" ||
                lower == "notificaciones"
    }
}
