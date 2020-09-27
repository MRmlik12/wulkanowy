package io.github.wulkanowy.data.repositories.message

import io.github.wulkanowy.data.Status
import io.github.wulkanowy.data.db.entities.MessageWithAttachment
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.getMessageEntity
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

class MessageRepositoryTest {

    @MockK
    lateinit var local: MessageLocal

    @MockK
    lateinit var remote: MessageRemote

    @MockK
    lateinit var student: Student

    private lateinit var repo: MessageRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { student.userName } returns "Jan"
        repo = MessageRepository(local, remote)
    }

    @Test
    fun `throw error when message is not in the db`() {
        val testMessage = getMessageEntity(1, "", false)
        coEvery { local.getMessageWithAttachment(student, testMessage) } throws NullPointerException("No message in database")

        val message = runCatching { runBlocking { repo.getMessage(student, testMessage).toList()[1] } }
        assertEquals(NullPointerException::class.java, message.exceptionOrNull()?.javaClass)
    }

    @Test
    fun `get message when content already in db`() {
        val testMessage = getMessageEntity(123, "Test", false)
        val messageWithAttachment = MessageWithAttachment(testMessage, emptyList())

        coEvery { local.getMessageWithAttachment(student, testMessage) } returns flowOf(messageWithAttachment)

        val message = runBlocking { repo.getMessage(student, testMessage).toList() }

        assertEquals(Status.SUCCESS, message[1].status)
        assertEquals("Test", message[1].data!!.message.content)
    }

    @Test
    fun `get message when content in db is empty`() {
        val testMessage = getMessageEntity(123, "", true)
        val testMessageWithContent = testMessage.copy().apply { content = "Test" }

        val mWa = MessageWithAttachment(testMessage, emptyList())
        val mWaWithContent = MessageWithAttachment(testMessageWithContent, emptyList())

        coEvery { local.getMessageWithAttachment(student, testMessage) } returnsMany listOf(flowOf(mWa), flowOf(mWaWithContent))
        coEvery { remote.getMessagesContentDetails(student, any(), any()) } returns ("Test" to emptyList())
        coEvery { local.updateMessages(any()) } just Runs
        coEvery { local.saveMessageAttachments(any()) } just Runs

        val message = runBlocking { repo.getMessage(student, testMessage).toList() }

        assertEquals(Status.SUCCESS, message[2].status)
        assertEquals("Test", message[2].data!!.message.content)
        coVerify { local.updateMessages(listOf(testMessageWithContent)) }
    }

    @Test
    fun `get message when content in db is empty and there is no internet connection`() {
        val testMessage = getMessageEntity(123, "", false)
        val messageWithAttachment = MessageWithAttachment(testMessage, emptyList())

        coEvery { local.getMessageWithAttachment(student, testMessage) } throws UnknownHostException()

        val message = runCatching { runBlocking { repo.getMessage(student, testMessage).toList()[1] } }
        assertEquals(UnknownHostException::class.java, message.exceptionOrNull()?.javaClass)
    }

    @Test
    fun `get message when content in db is empty, unread and there is no internet connection`() {
        val testMessage = getMessageEntity(123, "", true)
        val messageWithAttachment = MessageWithAttachment(testMessage, emptyList())

        coEvery { local.getMessageWithAttachment(student, testMessage) } throws UnknownHostException()

        val message = runCatching { runBlocking { repo.getMessage(student, testMessage).toList()[1] } }
        assertEquals(UnknownHostException::class.java, message.exceptionOrNull()?.javaClass)
    }
}
