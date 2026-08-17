// © 2026 Pablo Daniel de Luca - Ink 318 Software. Todos los derechos reservados.
// DNI: 31.649.936
// Este archivo es propiedad exclusiva de Pablo Daniel de Luca / Ink 318 Software.
// Queda prohibida su reproducción, distribución, modificación, venta o uso total o parcial sin autorización expresa y por escrito del titular.

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CyberCore(isProcessing: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "CorePulse")
    
    val coreScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isProcessing) 1.35f else 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProcessing) 700 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CoreScale"
    )

    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isProcessing) 1.6f else 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isProcessing) 1200 else 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloScale"
    )

    val coreColor = if (isProcessing) Color(0xFFFF00AA) else Color(0xFF00F0FF)

    Box(
        modifier = Modifier.size(60.dp),
        contentAlignment = Alignment.Center
    ) {
        // Halo Ring
        Box(
            modifier = Modifier
                .size(40.dp)
                .scale(haloScale)
                .border(1.dp, coreColor.copy(alpha = 0.4f), CircleShape)
        )
        // Core Sphere
        Box(
            modifier = Modifier
                .size(14.dp)
                .scale(coreScale)
                .clip(CircleShape)
                .background(coreColor)
        )
    }
}
