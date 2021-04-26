package io.github.wulkanowy.ui.modules.message.send

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import io.github.wulkanowy.R
import timber.log.Timber

class SendMessageDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity.let {
            val builder = AlertDialog.Builder(it)
                .setTitle(R.string.message_title)
                .setMessage(resources.getString(R.string.message_restore_dialog, (activity as SendMessageActivity).getRecipientsNames()))
                .setPositiveButton("Yes") { _, _ ->
                    (activity as SendMessageActivity).recoverDraft()
                    Timber.i("Continue work on draft")
                }
                .setNegativeButton("No") { _, _ ->
                    (activity as SendMessageActivity).clearDraft()
                    Timber.i("Draft cleared!")
                }
            builder.create()
        }
    }
}