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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import com.google.accompanist.permissions.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.CyberCore
import com.example.ui.components.StarfieldBackground
import com.example.ui.theme.*
import com.example.voice.VoiceEngine
import com.example.data.AppDatabase
import com.example.memory.Memory
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

        // 2. Archivos Totales (Opcional pero solicitado)
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
        Message("Entidad Pablo. CyberIA en línea, enlazada localmente. Sistemas neuronales operativos.", false)
    )) }
    var isProcessing by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

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
    }

    val voiceEngine = remember {
        initVoiceEngine { spokenText ->
            isListening = false
            inputText = TextFieldValue(spokenText)
            // Automate send on voice recognize
            messages = messages + Message(spokenText, true)
            isProcessing = true
            val response = LocalCommandExecutor.execute(spokenText)
            messages = messages + Message(response, false)
            
            coroutineScope.launch(Dispatchers.IO) {
                memoryDao.insertMemory(Memory(userQuery = spokenText, aiResponse = response))
            }
            
            isProcessing = false
        }
    }

    fun handleSend() {
        val text = inputText.text.trim()
        if (text.isEmpty()) return
        messages = messages + Message(text, true)
        inputText = TextFieldValue("")
        isProcessing = true
        
        // Control de perfil de voz interceptado
        if (text.lowercase().contains("voz aguda")) voiceEngine.setVoiceProfile(1.5f, 1.0f)
        if (text.lowercase().contains("voz grave")) voiceEngine.setVoiceProfile(0.5f, 1.0f)
        if (text.lowercase().contains("voz rápida") || text.lowercase().contains("voz rapida")) voiceEngine.setVoiceProfile(1.0f, 1.5f)
        if (text.lowercase().contains("voz normal")) voiceEngine.setVoiceProfile(1.0f, 1.0f)

        val response = LocalCommandExecutor.execute(text)
        messages = messages + Message(response, false)
        voiceEngine.speak(response)
        
        coroutineScope.launch(Dispatchers.IO) {
            memoryDao.insertMemory(Memory(userQuery = text, aiResponse = response))
        }
        
        isProcessing = false
        
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
        }
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
        containerColor = Color.Black
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
                // Header (Centered)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.dp, CyanHolo.copy(alpha = 0.2f))
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CyberCore(isProcessing = isProcessing || isListening)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CYBERIA",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 4.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isProcessing) "PROCESANDO..." else if (isListening) "ESCUCHANDO..." else "NEURAL LINK ACTIVE",
                            color = if (isProcessing || isListening) MagentaHolo else CyanHolo,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }
                }

                // Chat / Messages
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(messages.size) { index ->
                            val msg = messages[index]
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (msg.isUser) Color.White else CyanHolo,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                )
                            }
                        }
                    }
                    
                    // Gradient overlay to fade out messages at the top without offscreen rendering
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black,
                                    1f to Color.Transparent
                                )
                            )
                    )
                }

                // Input Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.7f))
                        .border(1.dp, CyanHolo.copy(alpha = 0.15f))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Transmitir pensamiento...", color = Color.White.copy(alpha = 0.2f), fontSize = 14.sp, fontFamily = FontFamily.Monospace) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = CyanHolo
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, CyanHolo, RoundedCornerShape(8.dp)),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    )

                    // Mic Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, if (isListening) MagentaHolo else CyanHolo, CircleShape)
                            .clickable {
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
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Mic",
                            tint = if (isListening) MagentaHolo else CyanHolo
                        )
                    }

                    // Send Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, CyanHolo, CircleShape)
                            .clickable { handleSend() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = CyanHolo,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

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
}
