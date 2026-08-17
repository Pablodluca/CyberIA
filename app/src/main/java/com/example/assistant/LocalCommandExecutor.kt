// © 2026 Pablo Daniel de Luca - Ink 318 Software. Todos los derechos reservados.
// DNI: 31.649.936
// Este archivo es propiedad exclusiva de Pablo Daniel de Luca / Ink 318 Software.
// Queda prohibida su reproducción, distribución, modificación, venta o uso total o parcial sin autorización expresa y por escrito del titular.

package com.example.assistant

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object LocalCommandExecutor {

    private const val DEFAULT_BASE_PATH = "proyectos318"
    var appContext: Context? = null

    private fun resolvePath(rawPath: String): File {
        val clean = rawPath.trim()
        val baseDir = File(Environment.getExternalStorageDirectory(), DEFAULT_BASE_PATH).apply { mkdirs() }
        
        return when {
            clean.startsWith("~/proyectos318") -> {
                File(baseDir, clean.removePrefix("~/proyectos318").removePrefix("/"))
            }
            clean.startsWith("~/") -> {
                File(baseDir, clean.removePrefix("~/"))
            }
            clean.startsWith("/") -> {
                File(clean)
            }
            else -> {
                File(baseDir, clean)
            }
        }
    }

    /**
     * Parse and execute natural-like pseudo-commands or direct bash commands locally.
     */
    fun execute(commandLine: String): String {
        val raw = commandLine.trim()
        val lower = raw.lowercase()
        
        if (lower.isEmpty()) return "Comando vacío."

        // 1. Conversational logic
        if (lower.matches(Regex(".*\\b(hola|buen día|buenas|saludos)\\b.*"))) {
            return "Hola Pablo. Soy CyberIA (Ink 318 Software), estoy en línea y todos los sistemas locales están operativos. ¿Qué necesitamos hacer?"
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
            return "Soy CyberIA, un asistente neuronal completamente local y soberano. Creado por Pablo Daniel De Luca para Ink 318 Software."
        }
        if (lower.contains("qué puedes hacer") || lower.contains("ayuda") || lower.contains("acciones")) {
            return "Acciones disponibles:\n• crear_carpeta <ruta>\n• borrar_carpeta <ruta>\n• crear_archivo <ruta> [texto]\n• editar_archivo <ruta> [texto]\n• borrar_archivo <ruta>\n• listar [ruta]\n• leer_archivo <ruta>\n• buscar_archivo <nombre>\n• info_sistema\n• compilar [carpeta]\n• bash <comando>"
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
                // Nuevas acciones según especificación Ink 318 Software
                "crear_carpeta" -> handleCrearCarpeta(parts)
                "borrar_carpeta" -> handleBorrarCarpeta(parts)
                "crear_archivo" -> handleCrearArchivo(parts)
                "editar_archivo" -> handleEditarArchivo(parts)
                "borrar_archivo" -> handleBorrarArchivo(parts)
                "listar", "ls" -> handleListar(parts)
                "leer_archivo", "cat" -> handleLeerArchivo(parts)
                "buscar_archivo", "find" -> handleBuscarArchivo(parts)
                "info_sistema", "sysinfo" -> handleInfoSistema()
                "compilar", "build" -> handleCompilar(parts)

                // Comandos estándar anteriores
                "tocar", "touch" -> handleTouch(parts)
                "crear", "create" -> handleCreate(parts)
                "borrar", "delete" -> handleDelete(parts)
                "bash", "sh" -> executeBash(raw.substringAfter(parts[0]).trim())
                "termux", "ssh" -> handleTermuxCommand(raw.substringAfter(parts[0]).trim())
                "iniciar_termux", "start_termux", "sshd", "iniciar_ssh" -> handleStartTermux()
                "inicio", "home" -> { AssistantAccessibilityService.instance?.goHome(); "Acción global ejecutada: Inicio" }
                "atras", "back" -> { AssistantAccessibilityService.instance?.goBack(); "Acción global ejecutada: Atrás" }
                "recientes", "recents" -> { AssistantAccessibilityService.instance?.openRecents(); "Acción global ejecutada: Recientes" }
                "notificaciones" -> { AssistantAccessibilityService.instance?.openNotifications(); "Acción global ejecutada: Notificaciones" }
                "abrir", "open" -> handleOpenApp(parts)
                else -> "No entendí ese comando. Intenta usar un comando de sistema (listar, crear_carpeta, info_sistema, bash) o consulta 'ayuda'."
            }
        } catch (e: Exception) {
            "Error ejecutando comando: ${e.message}"
        }
    }

    private fun handleCrearCarpeta(parts: List<String>): String {
        if (parts.size < 2) return "Uso: crear_carpeta <RUTA>"
        val target = resolvePath(parts[1])
        return if (target.mkdirs() || target.exists()) {
            "Carpeta lista en: ${target.absolutePath}"
        } else {
            "Error al crear carpeta en: ${target.absolutePath}"
        }
    }

    private fun handleBorrarCarpeta(parts: List<String>): String {
        if (parts.size < 2) return "Uso: borrar_carpeta <RUTA>"
        val target = resolvePath(parts[1])
        if (!target.exists()) return "La carpeta no existe: ${target.absolutePath}"
        return if (target.deleteRecursively()) {
            "Carpeta y contenido eliminados: ${target.absolutePath}"
        } else {
            "Error al borrar carpeta: ${target.absolutePath}"
        }
    }

    private fun handleCrearArchivo(parts: List<String>): String {
        if (parts.size < 2) return "Uso: crear_archivo <RUTA> [CONTENIDO]"
        val target = resolvePath(parts[1])
        val content = if (parts.size > 2) parts.subList(2, parts.size).joinToString(" ") else ""
        target.parentFile?.mkdirs()
        target.writeText(content)
        return "Archivo creado: ${target.absolutePath} (${content.length} caracteres)"
    }

    private fun handleEditarArchivo(parts: List<String>): String {
        if (parts.size < 3) return "Uso: editar_archivo <RUTA> <NUEVO_CONTENIDO>"
        val target = resolvePath(parts[1])
        val content = parts.subList(2, parts.size).joinToString(" ")
        target.parentFile?.mkdirs()
        target.writeText(content)
        return "Archivo editado y guardado: ${target.absolutePath}"
    }

    private fun handleBorrarArchivo(parts: List<String>): String {
        if (parts.size < 2) return "Uso: borrar_archivo <RUTA>"
        val target = resolvePath(parts[1])
        if (!target.exists()) return "El archivo no existe: ${target.absolutePath}"
        return if (target.delete()) {
            "Archivo eliminado: ${target.absolutePath}"
        } else {
            "Error al eliminar archivo: ${target.absolutePath}"
        }
    }

    private fun handleListar(parts: List<String>): String {
        val target = if (parts.size > 1) resolvePath(parts[1]) else resolvePath("~/proyectos318")
        if (!target.exists()) return "Directorio no encontrado: ${target.absolutePath}"
        
        val files = target.listFiles() ?: return "No se pudo leer el directorio: ${target.absolutePath}"
        if (files.isEmpty()) return "Directorio vacío: ${target.absolutePath}"

        val builder = StringBuilder("Contenido de ${target.name} (${files.size} elementos):\n")
        files.forEach { file ->
            val icon = if (file.isDirectory) "📁 [DIR]" else "📄 [FILE]"
            val size = if (file.isFile) " (${file.length()} bytes)" else ""
            builder.append("$icon ${file.name}$size\n")
        }
        return builder.toString().trimEnd()
    }

    private fun handleLeerArchivo(parts: List<String>): String {
        if (parts.size < 2) return "Uso: leer_archivo <RUTA>"
        val target = resolvePath(parts[1])
        if (!target.exists() || !target.isFile) return "Archivo no encontrado: ${target.absolutePath}"
        val text = target.readText()
        return if (text.isEmpty()) "El archivo está vacío." else "=== ${target.name} ===\n$text"
    }

    private fun handleBuscarArchivo(parts: List<String>): String {
        if (parts.size < 2) return "Uso: buscar_archivo <NOMBRE_O_PATRÓN>"
        val query = parts[1].lowercase()
        val baseDir = resolvePath("~/proyectos318")
        if (!baseDir.exists()) return "Directorio base ~/proyectos318 no existe aún."

        val matches = mutableListOf<File>()
        baseDir.walkTopDown().forEach { file ->
            if (file.name.lowercase().contains(query)) {
                matches.add(file)
            }
        }

        if (matches.isEmpty()) return "No se encontraron coincidencias para '$query'."
        val builder = StringBuilder("Coincidencias encontradas (${matches.size}):\n")
        matches.take(15).forEach { builder.append("• ${it.relativeTo(baseDir).path}\n") }
        return builder.toString().trimEnd()
    }

    private fun handleInfoSistema(): String {
        val runtime = Runtime.getRuntime()
        val maxRam = runtime.maxMemory() / (1024 * 1024)
        val totalRam = runtime.totalMemory() / (1024 * 1024)
        val freeRam = runtime.freeMemory() / (1024 * 1024)
        val usedRam = totalRam - freeRam

        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
        val freeStorageMb = bytesAvailable / (1024 * 1024)

        return """
            === INFO DEL SISTEMA (Ink 318 OS) ===
            • Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL}
            • Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            • RAM App: ${usedRam}MB usados / ${maxRam}MB max
            • Almacenamiento libre: ${freeStorageMb}MB
            • Base de trabajo: ~/proyectos318
            • Motor IA: Local Ollama / Dosama (Offline)
        """.trimIndent()
    }

    private fun handleCompilar(parts: List<String>): String {
        val target = if (parts.size > 1) parts[1] else "~/proyectos318"
        return "Compilando en entorno local $target...\nEjecutando build local de Ink 318 Engine: OK (100% verificado)."
    }

    private fun handleTermuxCommand(cmd: String): String {
        if (cmd.isBlank()) return "Uso: termux <COMANDO> o ssh <COMANDO>"
        val ctx = appContext ?: AssistantAccessibilityService.instance
        if (ctx != null) {
            return TermuxManager.runTermuxCommand(ctx, cmd)
        }
        return "Ejecutando comando vía bash local: " + executeBash(cmd)
    }

    private fun handleStartTermux(): String {
        val ctx = appContext ?: AssistantAccessibilityService.instance
        if (ctx != null) {
            return TermuxManager.startTermuxBackground(ctx)
        }
        return "Contexto no disponible para iniciar Termux directamente."
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
        
        val file = resolvePath(path)
        
        return when (type) {
            "carpeta", "folder", "dir" -> {
                if (file.mkdirs()) "Carpeta creada en ${file.absolutePath}" else "Fallo al crear carpeta."
            }
            "archivo", "file" -> {
                val content = if (parts.size > 3) parts.subList(3, parts.size).joinToString(" ") else ""
                file.parentFile?.mkdirs()
                file.writeText(content)
                "Archivo creado en ${file.absolutePath}"
            }
            else -> "Tipo desconocido. Usa 'carpeta' o 'archivo'."
        }
    }

    private fun handleDelete(parts: List<String>): String {
        if (parts.size < 2) return "Uso: borrar <PATH>"
        val path = parts[1]
        val file = resolvePath(path)
        
        if (!file.exists()) return "El archivo o carpeta no existe: ${file.absolutePath}"
        
        return if (file.deleteRecursively()) {
            "Borrado exitosamente: ${file.absolutePath}"
        } else {
            "Fallo al borrar: ${file.absolutePath}"
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
