package io.github.wulkanowy.data.repositories.semester

import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.utils.DispatchersProvider
import io.github.wulkanowy.utils.getCurrentOrLast
import io.github.wulkanowy.utils.isCurrent
import io.github.wulkanowy.utils.uniqueSubtract
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SemesterRepository @Inject constructor(
    private val remote: SemesterRemote,
    private val local: SemesterLocal,
    private val dispatchers: DispatchersProvider
) {

    suspend fun getSemesters(student: Student, forceRefresh: Boolean = false, refreshOnNoCurrent: Boolean = false) = withContext(dispatchers.backgroundThread) {
        local.getSemesters(student).let { semesters ->
            semesters.filter {
                !forceRefresh && when {
                    Sdk.Mode.valueOf(student.loginMode) != Sdk.Mode.API -> semesters.firstOrNull { it.isCurrent }?.diaryId != 0
                    refreshOnNoCurrent -> semesters.any { semester -> semester.isCurrent }
                    else -> true
                }
            }
        }.ifEmpty {
            val new = remote.getSemesters(student)
            if (new.isEmpty()) throw IllegalArgumentException("Empty semester list!")

            val old = local.getSemesters(student)
            local.deleteSemesters(old.uniqueSubtract(new))
            local.saveSemesters(new.uniqueSubtract(old))

            local.getSemesters(student)
        }
    }

    suspend fun getCurrentSemester(student: Student, forceRefresh: Boolean = false) = withContext(dispatchers.backgroundThread) {
        getSemesters(student, forceRefresh).getCurrentOrLast()
    }
}
