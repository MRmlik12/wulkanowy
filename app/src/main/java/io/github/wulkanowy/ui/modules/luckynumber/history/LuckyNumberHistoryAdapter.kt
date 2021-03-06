package io.github.wulkanowy.ui.modules.luckynumber.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.wulkanowy.data.db.entities.LuckyNumber
import io.github.wulkanowy.databinding.ItemLuckyNumberHistoryBinding
import io.github.wulkanowy.utils.toFormattedString
import io.github.wulkanowy.utils.weekDayName
import javax.inject.Inject

class LuckyNumberHistoryAdapter @Inject constructor() :
    RecyclerView.Adapter<LuckyNumberHistoryAdapter.ItemViewHolder>() {

    var items = emptyList<LuckyNumber>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ItemViewHolder(
        ItemLuckyNumberHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            luckyNumberWeekName.text = item.date.weekDayName
            luckyNumberDate.text = item.date.toFormattedString("dd.MM")
            luckyNumber.text = item.luckyNumber.toString()
        }
    }

    override fun getItemCount() = items.size

    class ItemViewHolder(val binding: ItemLuckyNumberHistoryBinding) : RecyclerView.ViewHolder(binding.root)
}