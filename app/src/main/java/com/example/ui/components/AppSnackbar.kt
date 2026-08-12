/*
 * Copyright (c) 2026 Pablo Daniel De Luca
 * Ink 318 Software
 * DNI: 31.649.936
 * Todos los derechos reservados.
 */
package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Reusable Snackbar & Toast Utility for Compose
 */

enum class SnackbarType {
    ERROR,
    WARNING,
    INFO
}

data class SnackbarMessage(
    val message: String,
    val type: SnackbarType = SnackbarType.ERROR,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

class AppSnackbarState(
    val snackbarHostState: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun showError(message: String, actionLabel: String? = "REINTENTAR", onAction: (() -> Unit)? = null) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = SnackbarDuration.Long
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onAction?.invoke()
            }
        }
    }

    fun showInfo(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }
}

@Composable
fun rememberAppSnackbarState(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    scope: CoroutineScope = rememberCoroutineScope()
): AppSnackbarState {
    return remember(snackbarHostState, scope) {
        AppSnackbarState(snackbarHostState, scope)
    }
}

/**
 * Custom Cyberpunk / Futuristic Styled Snackbar Host
 */
@Composable
fun CyberSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val cyanHolo = Color(0xFF00F0FF)
    val magentaHolo = Color(0xFFFF00AA)

    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(16.dp)
    ) { data ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D0D15).copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                .border(1.dp, magentaHolo, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = "Error",
                    tint = magentaHolo
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = data.visuals.message,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                data.visuals.actionLabel?.let { action ->
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { data.performAction() }
                    ) {
                        Text(
                            text = action,
                            color = cyanHolo,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Toast helper utility
 */
object ToastUtils {
    fun showToast(context: Context, message: String, isLong: Boolean = false) {
        Toast.makeText(
            context,
            message,
            if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }
}
