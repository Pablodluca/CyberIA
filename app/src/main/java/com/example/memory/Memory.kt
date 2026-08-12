/*
 * Copyright (c) 2026 Pablo Daniel De Luca
 * Ink 318 Software
 * DNI: 31.649.936
 * Todos los derechos reservados.
 */
package com.example.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userQuery: String,
    val aiResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)
