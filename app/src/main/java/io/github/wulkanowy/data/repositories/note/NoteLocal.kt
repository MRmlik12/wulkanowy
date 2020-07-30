package io.github.wulkanowy.data.repositories.note

import io.github.wulkanowy.data.db.dao.NoteDao
import io.github.wulkanowy.data.db.entities.Note
import io.github.wulkanowy.data.db.entities.Student
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteLocal @Inject constructor(private val noteDb: NoteDao) {

    suspend fun saveNotes(notes: List<Note>) {
        noteDb.insertAll(notes)
    }

    suspend fun updateNotes(notes: List<Note>) {
        noteDb.updateAll(notes)
    }

    suspend fun deleteNotes(notes: List<Note>) {
        noteDb.deleteAll(notes)
    }

    fun getNotes(student: Student): Flow<List<Note>> {
        return noteDb.loadAll(student.studentId)
    }
}
