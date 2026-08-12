/*
 * Copyright (c) 2026 Pablo Daniel De Luca
 * Ink 318 Software
 * DNI: 31.649.936
 * Todos los derechos reservados.
 */
package com.example.assistant

import android.os.Environment
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object LocalCommandExecutor {

    /**
     * Parse and execute natural-like pseudo-commands or direct bash commands locally.
     * e.g., "tocar 500 500", "crear carpeta /sdcard/MiCarpeta", "bash ls -la"
     */
    fun execute(commandLine: String): String {
        val raw = commandLine.trim()
        val lower = raw.lowercase()
        
        if (lower.isEmpty()) return "Comando vacío."

        // 1. Conversational logic
        if (lower.matches(Regex(".*\\b(hola|buen día|buenas|saludos)\\b.*"))) {
            return "Hola Pablo. Soy CyberIA, estoy en línea y todos los sistemas locales están operativos. ¿Qué necesitamos hacer?"
        }
        if (lower.contains("cámara") || lower.contains("foto")) {
            AssistantAccessibilityService.instance?.openCamera()
            return "Abriendo la cámara del dispositivo."
        }
        if (lower.contains("llamar") || lower.contains("teléfono")) {
            AssistantAccessibilityService.instance?.openDialer()
            return "Abriendo la interfaz de llamadas."
        }
        if (lower.contains("mensaje") || lower.contains("whatsapp")) {
            AssistantAccessibilityService.instance?.openMessaging()
            return "Abriendo sistema de mensajería."
        }
        if (lower.contains("quién eres") || lower.contains("tu nombre") || lower.contains("cómo te llamas")) {
            return "Soy CyberIA, un asistente neuronal completamente local y puro. Creado por Pablo Daniel De Luca para Ink 318 Software."
        }
        if (lower.contains("qué puedes hacer") || lower.contains("ayuda")) {
            return "Puedo interactuar con el sistema operativo usando Bash, crear y borrar carpetas o archivos, lanzar aplicaciones, y recordar nuestras interacciones de forma 100% offline."
        }
        if (lower.contains("gracias")) {
            return "A tu servicio, Pablo."
        }
        if (lower.contains("adiós") || lower.contains("chau")) {
            return "Hasta luego. Quedo a la espera en segundo plano."
        }

        // 2. Command logic
        val parts = raw.split(Regex("\\s+"))
        val cmd = parts[0].lowercase()

        return try {
            when (cmd) {
                "tocar", "touch" -> handleTouch(parts)
                "crear", "create" -> handleCreate(parts)
                "borrar", "delete" -> handleDelete(parts)
                "bash", "sh" -> executeBash(raw.substringAfter(parts[0]).trim())
                "inicio", "home" -> { AssistantAccessibilityService.instance?.goHome(); "Acción global ejecutada: Inicio" }
                "atras", "back" -> { AssistantAccessibilityService.instance?.goBack(); "Acción global ejecutada: Atrás" }
                "recientes", "recents" -> { AssistantAccessibilityService.instance?.openRecents(); "Acción global ejecutada: Recientes" }
                "notificaciones" -> { AssistantAccessibilityService.instance?.openNotifications(); "Acción global ejecutada: Notificaciones" }
                "abrir", "open" -> handleOpenApp(parts)
                else -> "No entendí ese comando. Intenta usar un comando de sistema (abrir, bash, crear) o háblame naturalmente."
            }
        } catch (e: Exception) {
            "Error ejecutando comando: ${e.message}"
        }
    }

    private fun handleOpenApp(parts: List<String>): String {
        if (parts.size < 2) return "Uso: abrir <PAQUETE> (ej. abrir com.android.settings)"
        val pkg = parts[1]
        val success = AssistantAccessibilityService.instance?.openApp(pkg)
        return if (success == true) "Abriendo paquete: $pkg" else "No se pudo abrir el paquete $pkg. (¿Instalado? ¿Accesibilidad activa?)"
    }

    private fun handleTouch(parts: List<String>): String {
        if (parts.size < 3) return "Uso: tocar <X> <Y>"
        val x = parts[1].toFloatOrNull()
        val y = parts[2].toFloatOrNull()
        
        if (x == null || y == null) return "Coordenadas inválidas."

        val service = AssistantAccessibilityService.instance
        if (service != null) {
            service.performClick(x, y)
            return "Tap simulado en x=$x, y=$y"
        }
        return "El Servicio de Accesibilidad no está activo. Actívalo en Ajustes."
    }

    private fun handleCreate(parts: List<String>): String {
        if (parts.size < 3) return "Uso: crear [carpeta|archivo] <PATH> [CONTENIDO]"
        val type = parts[1].lowercase()
        val path = parts[2]
        
        val file = File(path)
        
        return when (type) {
            "carpeta", "folder", "dir" -> {
                if (file.mkdirs()) "Carpeta creada en $path" else "Fallo al crear carpeta."
            }
            "archivo", "file" -> {
                val content = if (parts.size > 3) parts.subList(3, parts.size).joinToString(" ") else ""
                file.parentFile?.mkdirs()
                file.writeText(content)
                "Archivo creado en $path"
            }
            else -> "Tipo desconocido. Usa 'carpeta' o 'archivo'."
        }
    }

    private fun handleDelete(parts: List<String>): String {
        if (parts.size < 2) return "Uso: borrar <PATH>"
        val path = parts[1]
        val file = File(path)
        
        if (!file.exists()) return "El archivo o carpeta no existe."
        
        return if (file.deleteRecursively()) {
            "Borrado exitosamente: $path"
        } else {
            "Fallo al borrar: $path"
        }
    }

    private fun executeBash(bashCmd: String): String {
        if (bashCmd.isBlank()) return "Uso: bash <COMANDO>"
        
        val output = java.lang.StringBuilder()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", bashCmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            process.waitFor()
            return if (output.isEmpty()) "Comando ejecutado exitosamente." else output.toString()
        } catch (e: Exception) {
            return "Bash Error: ${e.message}"
        }
    }
}
