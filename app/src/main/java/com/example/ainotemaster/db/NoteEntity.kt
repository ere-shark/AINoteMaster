package com.example.ainotemaster.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val summary: String,
    val questions: String,
    val answers: String,
    val createdAt: Long = System.currentTimeMillis()
)
