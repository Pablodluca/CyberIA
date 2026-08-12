/*
 * Copyright (c) 2026 Pablo Daniel De Luca
 * Ink 318 Software
 * DNI: 31.649.936
 * Todos los derechos reservados.
 */
package com.example.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory)

    @Query("DELETE FROM memories")
    suspend fun clearMemories()
}
