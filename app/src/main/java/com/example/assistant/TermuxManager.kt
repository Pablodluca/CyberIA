// © 2026 Pablo Daniel de Luca - Ink 318 Software. Todos los derechos reservados.
// DNI: 31.649.936
// Este archivo es propiedad exclusiva de Pablo Daniel de Luca / Ink 318 Software.
// Queda prohibida su reproducción, distribución, modificación, venta o uso total o parcial sin autorización expresa y por escrito del titular.

package com.example.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/**
 * Ink 318 Software - Gestor de ejecución en segundo plano para Termux y Servidor SSH local.
 */
object TermuxManager {

    const val TERMUX_PACKAGE = "com.termux"
    const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_RUN_IN_BACKGROUND = "com.termux.RUN_COMMAND_RUN_IN_BACKGROUND"
    const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Inicia Termux o envía comando para levantar SSH daemon (sshd) y Ollama en segundo plano.
     */
    fun startTermuxBackground(context: Context): String {
        return try {
            // Intentar primero lanzar comando en segundo plano si tiene permisos de Termux RUN_COMMAND
            val runIntent = Intent(ACTION_RUN_COMMAND).apply {
                setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
                putExtra(EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
                putExtra(EXTRA_ARGUMENTS, arrayOf("-c", "sshd; ollama serve > /dev/null 2>&1 &"))
                putExtra(EXTRA_RUN_IN_BACKGROUND, true)
                putExtra(EXTRA_SESSION_ACTION, "0")
            }

            try {
                context.startService(runIntent)
                "Servicio Termux/SSH ejecutado en segundo plano."
            } catch (e: Exception) {
                // Si falla el servicio directo, intentar abrir la app de Termux
                val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    "Termux iniciado en el sistema para habilitar entorno SSH y comandos."
                } else {
                    "Termux no detectado en el dispositivo. Si lo instalas, CyberIA levantará automáticamente SSH y el entorno local."
                }
            }
        } catch (e: Exception) {
            Log.e("TermuxManager", "Error al inicializar Termux: ${e.message}")
            "Error al iniciar Termux: ${e.message}"
        }
    }

    /**
     * Ejecuta un comando arbitrario dentro del entorno Termux vía SSH o Intent
     */
    fun runTermuxCommand(context: Context, command: String, inBackground: Boolean = true): String {
        return try {
            val intent = Intent(ACTION_RUN_COMMAND).apply {
                setClassName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
                putExtra(EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
                putExtra(EXTRA_ARGUMENTS, arrayOf("-c", command))
                putExtra(EXTRA_RUN_IN_BACKGROUND, inBackground)
            }
            context.startService(intent)
            "Comando enviado a Termux: $command"
        } catch (e: Exception) {
            "Fallo al enviar comando a Termux: ${e.message}"
        }
    }
}
