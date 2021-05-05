package io.github.wulkanowy.data.repositories

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.SharedPrefProvider
import io.github.wulkanowy.data.db.dao.MessageAttachmentDao
import io.github.wulkanowy.data.db.dao.MessagesDao
import io.github.wulkanowy.data.db.entities.Message
import io.github.wulkanowy.data.db.entities.Recipient
import io.github.wulkanowy.data.db.entities.Semester
import io.github.wulkanowy.data.db.entities.Student
import io.github.wulkanowy.data.enums.MessageFolder
import io.github.wulkanowy.data.enums.MessageFolder.RECEIVED
import io.github.wulkanowy.data.mappers.mapFromEntities
import io.github.wulkanowy.data.mappers.mapToEntities
import io.github.wulkanowy.sdk.Sdk
import io.github.wulkanowy.sdk.pojo.Folder
import io.github.wulkanowy.sdk.pojo.SentMessage
import io.github.wulkanowy.ui.modules.message.send.RecipientChipItem
import io.github.wulkanowy.utils.AutoRefreshHelper
import io.github.wulkanowy.utils.getRefreshKey
import io.github.wulkanowy.utils.init
import io.github.wulkanowy.utils.networkBoundResource
import io.github.wulkanowy.utils.uniqueSubtract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber
import java.time.LocalDateTime.now
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messagesDb: MessagesDao,
    private val messageAttachmentDao: MessageAttachmentDao,
    private val sdk: Sdk,
    @ApplicationContext private val context: Context,
    private val refreshHelper: AutoRefreshHelper,
    private val sharedPrefProvider: SharedPrefProvider
) {

    private val saveFetchResultMutex = Mutex()

    private val cacheKey = "message"

    @Suppress("UNUSED_PARAMETER")
    fun getMessages(student: Student, semester: Semester, folder: MessageFolder, forceRefresh: Boolean, notify: Boolean = false) = networkBoundResource(
        mutex = saveFetchResultMutex,
        shouldFetch = { it.isEmpty() || forceRefresh || refreshHelper.isShouldBeRefreshed(getRefreshKey(cacheKey, student, folder)) },
        query = { messagesDb.loadAll(student.id.toInt(), folder.id) },
        fetch = { sdk.init(student).getMessages(Folder.valueOf(folder.name), now().minusMonths(3), now()).mapToEntities(student) },
        saveFetchResult = { old, new ->
            messagesDb.deleteAll(old uniqueSubtract new)
            messagesDb.insertAll((new uniqueSubtract old).onEach {
                it.isNotified = !notify
            })

            refreshHelper.updateLastRefreshTimestamp(getRefreshKey(cacheKey, student, folder))
        }
    )

    fun getMessage(student: Student, message: Message, markAsRead: Boolean = false) = networkBoundResource(
        shouldFetch = {
            checkNotNull(it, { "This message no longer exist!" })
            Timber.d("Message content in db empty: ${it.message.content.isEmpty()}")
            it.message.unread || it.message.content.isEmpty()
        },
        query = { messagesDb.loadMessageWithAttachment(student.id.toInt(), message.messageId) },
        fetch = {
            sdk.init(student).getMessageDetails(it!!.message.messageId, message.folderId, markAsRead, message.realId).let { details ->
                details.content to details.attachments.mapToEntities()
            }
        },
        saveFetchResult = { old, (downloadedMessage, attachments) ->
            checkNotNull(old, { "Fetched message no longer exist!" })
            messagesDb.updateAll(listOf(old.message.copy(unread = !markAsRead).apply {
                id = old.message.id
                content = content.ifBlank { downloadedMessage }
            }))
            messageAttachmentDao.insertAttachments(attachments)
            Timber.d("Message ${message.messageId} with blank content: ${old.message.content.isBlank()}, marked as read")
        }
    )

    fun getNotNotifiedMessages(student: Student): Flow<List<Message>> {
        return messagesDb.loadAll(student.id.toInt(), RECEIVED.id).map { it.filter { message -> !message.isNotified && message.unread } }
    }

    suspend fun updateMessages(messages: List<Message>) {
        return messagesDb.updateAll(messages)
    }

    suspend fun sendMessage(student: Student, subject: String, content: String, recipients: List<Recipient>): SentMessage {
        return sdk.init(student).sendMessage(
            subject = subject,
            content = content,
            recipients = recipients.mapFromEntities()
        )
    }

    suspend fun deleteMessage(student: Student, message: Message) {
        val isDeleted = sdk.init(student).deleteMessages(listOf(message.messageId), message.folderId)

        if (message.folderId != MessageFolder.TRASHED.id) {
            if (isDeleted) messagesDb.updateAll(listOf(message.copy(folderId = MessageFolder.TRASHED.id).apply {
                id = message.id
                content = message.content
            }))
        } else messagesDb.deleteAll(listOf(message))
    }

    fun getDraftState(): Boolean {
        return sharedPrefProvider.getBoolean(context.getString(R.string.pref_key_message_send_is_draft), false)
    }

    fun changeDraftState(recipients: List<RecipientChipItem>, subject: String, content: String) {
        if (recipients.isEmpty() && subject.isEmpty() && content.isEmpty()) {
            sharedPrefProvider.putBoolean(context.getString(R.string.pref_key_message_send_is_draft), false)
            return
        }

        sharedPrefProvider.putBoolean(context.getString(R.string.pref_key_message_send_is_draft), true)
    }

    fun setMessageSubject(text: CharSequence?) {
        sharedPrefProvider.putString(context.getString(R.string.pref_key_message_send_subject), text.toString())
    }

    fun getMessageSubject() = sharedPrefProvider.getString(context.getString(R.string.pref_key_message_send_subject), "")

    fun setMessageContent(text: CharSequence?) {
        sharedPrefProvider.putString(context.getString(R.string.pref_key_message_send_content), text.toString())
    }

    fun getMessageContent() = sharedPrefProvider.getString(context.getString(R.string.pref_key_message_send_content), "")

    fun setMessageRecipients(recipients: String) {
        sharedPrefProvider.putString(context.getString(R.string.pref_key_message_send_recipients), recipients)
    }

    fun getMessageRecipients() = sharedPrefProvider.getString(context.getString(R.string.pref_key_message_send_recipients), "")

    fun clearMessagePreferences() {
        sharedPrefProvider.delete(context.getString(R.string.pref_key_message_send_recipients))
        sharedPrefProvider.delete(context.getString(R.string.pref_key_message_send_subject))
        sharedPrefProvider.delete(context.getString(R.string.pref_key_message_send_content))
    }
}
