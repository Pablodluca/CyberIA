/*
 * Copyright (c) 2026 Pablo Daniel De Luca
 * Ink 318 Software
 * DNI: 31.649.936
 * Todos los derechos reservados.
 */
package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.accompanist.permissions.*
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.assistant.LocalCommandExecutor
import com.example.assistant.FloatingBubbleService
import com.example.ui.components.OllamaChatView
import com.example.ui.components.CyberCore
import com.example.ui.components.StarfieldBackground
import com.example.ui.components.rememberAppSnackbarState
import com.example.ui.components.CyberSnackbarHost
import com.example.ui.theme.*
import com.example.voice.VoiceEngine
import com.example.data.AppDatabase
import com.example.memory.Memory
import com.example.ollama.OllamaEngine
import com.example.ollama.OllamaConnectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

val CyanHolo = Color(0xFF00F0FF)
val MagentaHolo = Color(0xFFFF00AA)

class MainActivity : ComponentActivity() {
    private var voiceEngine: VoiceEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Solicitud de Burbuja / Superposición
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        } else {
            // Iniciar burbuja flotante si ya tiene permiso
            startService(Intent(this, FloatingBubbleService::class.java))
        }

        // 2. Archivos Totales
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {}
            }
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CyberiaApp(
                    initVoiceEngine = { onResult ->
                        if (voiceEngine == null) {
                            voiceEngine = VoiceEngine(this, onResult)
                        }
                        voiceEngine!!
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine?.destroy()
    }
}

data class Message(val text: String, val isUser: Boolean, val timestamp: Long = System.currentTimeMillis())

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CyberiaApp(initVoiceEngine: ((String) -> Unit) -> VoiceEngine) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val memoryDao = database.memoryDao()
    
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf(listOf(
        Message("Entidad Pablo. CyberIA en línea con Motor Ollama / Dosama local [${OllamaEngine.currentModel}]. Totalmente autónomo y fuera de línea.", false)
    )) }
    var isProcessing by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var connectionState by remember { mutableStateOf(OllamaConnectionState.CHECKING) }

    val appSnackbarState = rememberAppSnackbarState()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    @OptIn(ExperimentalPermissionsApi::class)
    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!multiplePermissionsState.allPermissionsGranted) {
            multiplePermissionsState.launchMultiplePermissionRequest()
        }
        connectionState = OllamaEngine.checkConnection()
    }

    fun processMessageInternal(text: String, voiceEngine: VoiceEngine) {
        if (text.isBlank()) return
        isProcessing = true

        coroutineScope.launch {
            // Check for explicit local system hardware/bash actions
            if (OllamaEngine.isExplicitSystemCommand(text)) {
                val sysResult = LocalCommandExecutor.execute(text)
                messages = messages + Message(sysResult, false)
                voiceEngine.speak(sysResult)
                launch(Dispatchers.IO) {
                    memoryDao.insertMemory(Memory(userQuery = text, aiResponse = sysResult))
                }
                isProcessing = false
                launch { listState.animateScrollToItem(messages.size - 1) }
                return@launch
            }

            // Real-time Streaming Response from Local Ollama/Dosama Engine
            var streamingText = ""
            val initialMsgIndex = messages.size
            messages = messages + Message("", false)

            var hasError = false

            OllamaEngine.queryOllamaStream(text).collect { token ->
                if (token.startsWith("[ERROR]")) {
                    hasError = true
                    connectionState = OllamaConnectionState.DISCONNECTED
                    // Fallback to local offline command executor if Ollama server fails
                    val fallbackAnswer = LocalCommandExecutor.execute(text)
                    streamingText = fallbackAnswer
                    val updatedList = messages.toMutableList()
                    if (initialMsgIndex < updatedList.size) {
                        updatedList[initialMsgIndex] = Message(fallbackAnswer, false)
                    }
                    messages = updatedList
                    appSnackbarState.showError(
                        message = token,
                        actionLabel = "CONFIGURAR"
                    ) {
                        showSettingsDialog = true
                    }
                } else {
                    connectionState = OllamaConnectionState.CONNECTED
                    streamingText += token
                    val updatedList = messages.toMutableList()
                    if (initialMsgIndex < updatedList.size) {
                        updatedList[initialMsgIndex] = Message(streamingText, false)
                    }
                    messages = updatedList
                }

                launch { listState.animateScrollToItem(messages.size - 1) }
            }

            if (!hasError && streamingText.isNotBlank()) {
                voiceEngine.speak(streamingText)
                launch(Dispatchers.IO) {
                    memoryDao.insertMemory(Memory(userQuery = text, aiResponse = streamingText))
                }
            }

            isProcessing = false
        }
    }

    lateinit var voiceEngineRef: VoiceEngine

    val voiceEngine = remember {
        val engine = initVoiceEngine { spokenText ->
            isListening = false
            inputText = TextFieldValue(spokenText)
            messages = messages + Message(spokenText, true)
            processMessageInternal(spokenText, voiceEngineRef)
        }
        voiceEngineRef = engine
        engine
    }

    fun handleSend() {
        val text = inputText.text.trim()
        if (text.isEmpty()) return
        messages = messages + Message(text, true)
        inputText = TextFieldValue("")
        
        // Control de perfil de voz interceptado
        if (text.lowercase().contains("voz aguda")) voiceEngine.setVoiceProfile(1.5f, 1.0f)
        if (text.lowercase().contains("voz grave")) voiceEngine.setVoiceProfile(0.5f, 1.0f)
        if (text.lowercase().contains("voz rápida") || text.lowercase().contains("voz rapida")) voiceEngine.setVoiceProfile(1.0f, 1.5f)
        if (text.lowercase().contains("voz normal")) voiceEngine.setVoiceProfile(1.0f, 1.0f)

        processMessageInternal(text, voiceEngine)
    }

    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(isProcessing) {
        if (!isProcessing && messages.size > 1) {
            glowAlpha.animateTo(1f, animationSpec = tween(200))
            glowAlpha.animateTo(0f, animationSpec = tween(1500))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        snackbarHost = {
            CyberSnackbarHost(hostState = appSnackbarState.snackbarHostState)
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .drawWithContent {
                drawContent()
                if (glowAlpha.value > 0f) {
                    drawRect(
                        color = CyanHolo.copy(alpha = glowAlpha.value * 0.4f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 30.dp.toPx())
                    )
                }
            }
        ) {
            // Background Canvas
            StarfieldBackground()

            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Centered with Settings Button)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, CyanHolo.copy(alpha = 0.2f))
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CyberCore(isProcessing = isProcessing || isListening)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CYBERIA · OLLAMA / DOSAMA",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isProcessing) "ENLACE NEURONAL STREAMING..." else if (isListening) "ESCUCHANDO..." else "MODELO LOCAL: ${OllamaEngine.currentModel}",
                            color = if (isProcessing || isListening) MagentaHolo else CyanHolo,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Offline Connection Status Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when (connectionState) {
                                        OllamaConnectionState.CONNECTED -> Color(0xFF0D2818)
                                        OllamaConnectionState.DISCONNECTED -> Color(0xFF280D13)
                                        OllamaConnectionState.CHECKING -> Color(0xFF1B1B2A)
                                    }
                                )
                                .border(
                                    width = 1.dp,
                                    color = when (connectionState) {
                                        OllamaConnectionState.CONNECTED -> Color(0xFF00FF66)
                                        OllamaConnectionState.DISCONNECTED -> Color(0xFFFF3366)
                                        OllamaConnectionState.CHECKING -> Color.Yellow
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    coroutineScope.launch {
                                        connectionState = OllamaConnectionState.CHECKING
                                        connectionState = OllamaEngine.checkConnection()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionState) {
                                            OllamaConnectionState.CONNECTED -> Color(0xFF00FF66)
                                            OllamaConnectionState.DISCONNECTED -> Color(0xFFFF3366)
                                            OllamaConnectionState.CHECKING -> Color.Yellow
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (connectionState) {
                                    OllamaConnectionState.CONNECTED -> "100% OFFLINE · OLLAMA LOCAL EN LÍNEA"
                                    OllamaConnectionState.DISCONNECTED -> "100% OFFLINE · MOTOR DESCONECTADO (M. DIRECTO)"
                                    OllamaConnectionState.CHECKING -> "VERIFICANDO CONEXIÓN LOCAL..."
                                },
                                color = when (connectionState) {
                                    OllamaConnectionState.CONNECTED -> Color(0xFF00FF66)
                                    OllamaConnectionState.DISCONNECTED -> Color(0xFFFF3366)
                                    OllamaConnectionState.CHECKING -> Color.Yellow
                                },
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Settings Gear Icon
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Configurar Ollama Local",
                            tint = CyanHolo
                        )
                    }
                }

                // Conversational Chat Component
                OllamaChatView(
                    messages = messages,
                    inputText = inputText,
                    onInputTextChange = { inputText = it },
                    onSendPrompt = { handleSend() },
                    isProcessing = isProcessing,
                    isListening = isListening,
                    onMicClick = {
                        if (isListening) {
                            voiceEngine.stopListening()
                            isListening = false
                        } else {
                            val audioPermissionGranted = multiplePermissionsState.permissions.find { 
                                it.permission == Manifest.permission.RECORD_AUDIO 
                            }?.status?.isGranted == true

                            if (audioPermissionGranted || ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                voiceEngine.startListening()
                                isListening = true
                            } else {
                                multiplePermissionsState.launchMultiplePermissionRequest()
                            }
                        }
                    },
                    listState = listState,
                    modifier = Modifier.weight(1f)
                )

                // Footer
                Text(
                    text = "© 2026 PABLO DANIEL DE LUCA · DNI 31.649.936 · INK 318 SOFTWARE",
                    color = CyanHolo.copy(alpha = 0.5f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // Dialog for Local Ollama / Dosama Configuration and Model Pull
    if (showSettingsDialog) {
        var tempUrl by remember { mutableStateOf(OllamaEngine.serverUrl) }
        var tempModel by remember { mutableStateOf(OllamaEngine.currentModel) }
        var isPullingModel by remember { mutableStateOf(false) }
        var pullStatusText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!isPullingModel) showSettingsDialog = false },
            containerColor = Color(0xFF0F0F1A),
            title = {
                Text(
                    text = "MOTOR LOCAL Y MODELO DOSAMA",
                    color = CyanHolo,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "CyberIA opera 100% offline sin dependencias externas. Puedes conectar con tu motor local en Termux, PC o emulador:",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        label = { Text("URL Servidor Local", color = CyanHolo) },
                        placeholder = { Text("http://10.0.2.2:11434") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanHolo,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tempModel,
                        onValueChange = { tempModel = it },
                        label = { Text("Nombre Modelo Local", color = CyanHolo) },
                        placeholder = { Text("dosama") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanHolo,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Button to Pull/Download Dosama Model directly to local Ollama instance
                    Button(
                        onClick = {
                            isPullingModel = true
                            pullStatusText = "Iniciando descarga de $tempModel..."
                            coroutineScope.launch {
                                OllamaEngine.pullModelStream(tempModel).collect { statusObj ->
                                    if (!statusObj.error.isNullOrBlank()) {
                                        pullStatusText = "Error: ${statusObj.error}"
                                        isPullingModel = false
                                    } else {
                                        val total = statusObj.total ?: 0L
                                        val completed = statusObj.completed ?: 0L
                                        if (total > 0) {
                                            val pct = (completed * 100) / total
                                            pullStatusText = "${statusObj.status ?: "Bajando"} ($pct%)"
                                        } else {
                                            pullStatusText = statusObj.status ?: "Procesando..."
                                        }
                                        if (statusObj.status == "success") {
                                            pullStatusText = "¡Modelo $tempModel instalado correctamente offline!"
                                            isPullingModel = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isPullingModel,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF151525),
                            contentColor = CyanHolo
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyanHolo, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = "Descargar Modelo Dosama",
                                tint = CyanHolo
                            )
                            Text(
                                text = if (isPullingModel) "BAJANDO MODELO..." else "DESCARGAR MODELO LOCAL ($tempModel)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (pullStatusText.isNotBlank()) {
                        Text(
                            text = pullStatusText,
                            color = MagentaHolo,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "Modos de red local sin internet:\n• Dispositivo / Termux: http://localhost:11434/\n• Emulador Android: http://10.0.2.2:11434/\n• Host de Red LAN: http://192.168.1.X:11434/",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        OllamaEngine.updateConfig(tempUrl, tempModel)
                        showSettingsDialog = false
                        appSnackbarState.showInfo("Configuración local de Ollama guardada.")
                        coroutineScope.launch {
                            connectionState = OllamaConnectionState.CHECKING
                            connectionState = OllamaEngine.checkConnection()
                        }
                    }
                ) {
                    Text("GUARDAR", color = CyanHolo, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isPullingModel) showSettingsDialog = false }) {
                    Text("CANCELAR", color = Color.Gray)
                }
            }
        )
    }
}
