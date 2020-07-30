package io.github.wulkanowy.data.repositories.completedlessons

import io.github.wulkanowy.data.db.dao.CompletedLessonsDao
import io.github.wulkanowy.data.db.entities.CompletedLesson
import io.github.wulkanowy.data.db.entities.Semester
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompletedLessonsLocal @Inject constructor(private val completedLessonsDb: CompletedLessonsDao) {

    suspend fun saveCompletedLessons(completedLessons: List<CompletedLesson>) {
        completedLessonsDb.insertAll(completedLessons)
    }

    suspend fun deleteCompleteLessons(completedLessons: List<CompletedLesson>) {
        completedLessonsDb.deleteAll(completedLessons)
    }

    fun getCompletedLessons(semester: Semester, start: LocalDate, end: LocalDate): Flow<List<CompletedLesson>> {
        return completedLessonsDb.loadAll(semester.diaryId, semester.studentId, start, end)
    }
}
