package com.example.ainotemaster.db

import androidx.room.*

@Dao
interface NoteDao {

    @Insert
    fun insert(note: NoteEntity): Long

    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllOrderByDateDesc(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getById(id: Long): NoteEntity?

    @Query("DELETE FROM notes WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM notes")
    fun deleteAll()
}
