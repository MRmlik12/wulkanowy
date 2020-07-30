package io.github.wulkanowy.data.repositories.exam

import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.getStudentEntity
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.sdk.pojo.Exam
import io.github.wulkanowy.utils.init
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDate.of

class ExamRemoteTest {

    @SpyK
    private var mockSdk = Sdk()

    @MockK
    private lateinit var semesterMock: Semester

    private val student = getStudentEntity()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun getExamsTest() {
        every { mockSdk.init(student) } returns mockSdk
        every { mockSdk.switchDiary(1, 2019) } returns mockSdk

        coEvery {
            mockSdk.getExams(
                of(2018, 9, 10),
                of(2018, 9, 15),
                1
            )
        } returns listOf(
            getExam(of(2018, 9, 10)),
            getExam(of(2018, 9, 17))
        )

        every { semesterMock.studentId } returns 1
        every { semesterMock.diaryId } returns 1
        every { semesterMock.schoolYear } returns 2019
        every { semesterMock.semesterId } returns 1

        val exams = runBlocking {
            ExamRemote(mockSdk).getExams(student, semesterMock,
                of(2018, 9, 10),
                of(2018, 9, 15)
            )
        }
        assertEquals(2, exams.size)
    }

    private fun getExam(date: LocalDate): Exam {
        return Exam(
            subject = "",
            group = "",
            type = "",
            description = "",
            teacher = "",
            teacherSymbol = "",
            date = date,
            entryDate = date
        )
    }
}
