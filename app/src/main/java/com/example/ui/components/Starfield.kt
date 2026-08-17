// © 2026 Pablo Daniel de Luca - Ink 318 Software. Todos los derechos reservados.
// DNI: 31.649.936
// Este archivo es propiedad exclusiva de Pablo Daniel de Luca / Ink 318 Software.
// Queda prohibida su reproducción, distribución, modificación, venta o uso total o parcial sin autorización expresa y por escrito del titular.

package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Star(
    val x: Float,
    val y: Float,
    val size: Float,
    val isCyan: Boolean,
    val speedX: Float,
    val speedY: Float
)

@Composable
fun StarfieldBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "Starfield")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    val stars = remember {
        List(60) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat() * 2f + 0.5f,
                isCyan = Random.nextFloat() > 0.7f,
                speedX = (Random.nextFloat() - 0.5f) * 0.0005f,
                speedY = -(Random.nextFloat() * 0.002f + 0.001f) // Always float up
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        stars.forEach { star ->
            // Calculate movement based on time
            val currentX = (star.x * width + time * star.speedX * width) % width
            val currentY = (star.y * height + time * star.speedY * height) % height

            // Ensure positive wrap-around
            val finalX = if (currentX < 0) currentX + width else currentX
            val finalY = if (currentY < 0) currentY + height else currentY

            drawCircle(
                color = if (star.isCyan) Color(0xFF00F0FF).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                radius = star.size,
                center = Offset(finalX, finalY)
            )
        }
    }
}
