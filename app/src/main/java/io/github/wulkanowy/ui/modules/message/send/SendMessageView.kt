package io.github.wulkanowy.ui.modules.message.send

import io.github.wulkanowy.data.db.entities.ReportingUnit
import io.github.wulkanowy.ui.base.BaseView

interface SendMessageView : BaseView {
    val isDropdownListVisible: Boolean

    var formRecipientsData: List<RecipientChipItem>

    var formSubjectValue: String

    var formContentValue: String

    val messageRequiredRecipients: String

    val messageContentMinLength: String

    val messageSuccess: String

    fun initView()

    fun setReportingUnit(unit: ReportingUnit)

    fun setRecipients(recipients: List<RecipientChipItem>)

    fun setSelectedRecipients(recipients: List<RecipientChipItem>)

    fun showProgress(show: Boolean)

    fun showContent(show: Boolean)

    fun showEmpty(show: Boolean)

    fun showActionBar(show: Boolean)

    fun setSubject(subject: String)

    fun setContent(content: String)

    fun showSoftInput(show: Boolean)

    fun hideDropdownList()

    fun scrollToRecipients()

    fun popView()

    fun getDraftState(): Boolean

    fun getRecipientsNames(): String

    fun showMessageBackupDialog()

    fun recoverDraft()

    fun clearDraft()
}
