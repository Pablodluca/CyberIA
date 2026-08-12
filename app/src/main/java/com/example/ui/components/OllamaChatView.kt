/*
 * Copyright (c) 2026 Pablo Daniel De Luca
 * Ink 318 Software
 * DNI: 31.649.936
 * Todos los derechos reservados.
 */
package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CyanHolo
import com.example.MagentaHolo
import com.example.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OllamaChatView(
    messages: List<Message>,
    inputText: TextFieldValue,
    onInputTextChange: (TextFieldValue) -> Unit,
    onSendPrompt: () -> Unit,
    isProcessing: Boolean,
    isListening: Boolean,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = modifier.fillMaxSize()) {
        // Chat History List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("chat_history_list"),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(
                    count = messages.size,
                    key = { index -> "${messages[index].timestamp}_$index" }
                ) { index ->
                    val msg = messages[index]
                    ChatMessageBubble(
                        message = msg,
                        onCopyText = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                            ToastUtils.showToast(context, "Texto copiado al portapapeles")
                        }
                    )
                }
            }

            // Top gradient fade
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black,
                            1f to Color.Transparent
                        )
                    )
            )
        }

        // Processing Indicator Banner
        AnimatedVisibility(visible = isProcessing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A14))
                    .border(1.dp, CyanHolo.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = CyanHolo,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OLLAMA PENSANDO...",
                    color = CyanHolo,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Input Field and Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.85f))
                .border(1.dp, CyanHolo.copy(alpha = 0.2f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChange,
                placeholder = {
                    Text(
                        text = if (isListening) "Escuchando voz..." else "Enviar prompt a Ollama...",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFF090910),
                    focusedContainerColor = Color(0xFF090910),
                    unfocusedBorderColor = CyanHolo.copy(alpha = 0.3f),
                    focusedBorderColor = CyanHolo,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = CyanHolo
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("prompt_input_field"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            )

            // Mic Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isListening) MagentaHolo.copy(alpha = 0.2f) else Color(0xFF090910))
                    .border(1.dp, if (isListening) MagentaHolo else CyanHolo.copy(alpha = 0.5f), CircleShape)
                    .clickable { onMicClick() }
                    .testTag("mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Micrófono",
                    tint = if (isListening) MagentaHolo else CyanHolo
                )
            }

            // Send Prompt Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (inputText.text.isNotBlank()) CyanHolo else Color(0xFF090910))
                    .border(1.dp, CyanHolo, CircleShape)
                    .clickable(enabled = inputText.text.isNotBlank() && !isProcessing) {
                        onSendPrompt()
                    }
                    .testTag("send_prompt_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Enviar a Ollama",
                    tint = if (inputText.text.isNotBlank()) Color.Black else CyanHolo.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: Message,
    onCopyText: (String) -> Unit
) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) Color(0xFF161625) else Color(0xFF0A101D)
    val borderColor = if (isUser) MagentaHolo.copy(alpha = 0.4f) else CyanHolo.copy(alpha = 0.4f)
    val textColor = if (isUser) Color.White else Color(0xFFE0F7FF)

    val formattedTime = remember(message.timestamp) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            if (!isUser) {
                Icon(
                    imageVector = Icons.Rounded.SmartToy,
                    contentDescription = "Ollama Bot",
                    tint = CyanHolo,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "CYBERIA [OLLAMA]",
                    color = CyanHolo,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "ENTIDAD PABLO",
                    color = MagentaHolo,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "· $formattedTime",
                color = Color.Gray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .background(bubbleColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 12.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copiar texto",
                        tint = Color.Gray.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onCopyText(message.text) }
                    )
                }
            }
        }
    }
}
