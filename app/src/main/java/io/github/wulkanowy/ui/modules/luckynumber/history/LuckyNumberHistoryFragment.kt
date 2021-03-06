package io.github.wulkanowy.ui.modules.luckynumber.history

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.recyclerview.widget.LinearLayoutManager
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import dagger.hilt.android.AndroidEntryPoint
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.LuckyNumber
import io.github.wulkanowy.databinding.FragmentLuckyNumberHistoryBinding
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.ui.widgets.DividerItemDecoration
import io.github.wulkanowy.utils.SchooldaysRangeLimiter
import io.github.wulkanowy.utils.dpToPx
import io.github.wulkanowy.utils.getThemeAttrColor
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class LuckyNumberHistoryFragment :
    BaseFragment<FragmentLuckyNumberHistoryBinding>(R.layout.fragment_lucky_number_history), LuckyNumberHistoryView,
    MainView.TitledView {

    @Inject
    lateinit var presenter: LuckyNumberHistoryPresenter

    @Inject
    lateinit var luckyNumberHistoryAdapter: LuckyNumberHistoryAdapter

    companion object {
        fun newInstance() = LuckyNumberHistoryFragment()
    }

    override val titleStringId: Int
        get() = R.string.lucky_number_history_title

    override val isViewEmpty get() = luckyNumberHistoryAdapter.items.isEmpty()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentLuckyNumberHistoryBinding.bind(view)
        messageContainer = binding.luckyNumberRecycler
        presenter.onAttachView(this)
    }

    override fun initView() {
        with(binding.luckyNumberRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = luckyNumberHistoryAdapter
            addItemDecoration(DividerItemDecoration(context))
        }

        with(binding) {
            luckyNumberSwipe.setOnRefreshListener(presenter::onSwipeRefresh)
            luckyNumberSwipe.setColorSchemeColors(requireContext().getThemeAttrColor(R.attr.colorPrimary))
            luckyNumberSwipe.setProgressBackgroundColorSchemeColor(requireContext().getThemeAttrColor(R.attr.colorSwipeRefresh))
            luckyNumberNavDate.setOnClickListener { presenter.onPickDate() }
            luckyNumberErrorRetry.setOnClickListener { presenter.onRetry() }
            luckyNumberErrorDetails.setOnClickListener { presenter.onDetailsClick() }

            luckyNumberPreviousButton.setOnClickListener { presenter.onPreviousWeek() }
            luckyNumberNextButton.setOnClickListener { presenter.onNextWeek() }

            luckyNumberNavContainer.setElevationCompat(requireContext().dpToPx(8f))
        }
    }

    override fun updateData(data: List<LuckyNumber>) {
        with(luckyNumberHistoryAdapter) {
            items = data
            notifyDataSetChanged()
        }
    }

    override fun clearData() {
        with(luckyNumberHistoryAdapter) {
            items = emptyList()
            notifyDataSetChanged()
        }
    }

    override fun hideRefresh() {
        binding.luckyNumberSwipe.isRefreshing = false
    }

    override fun showEmpty(show: Boolean) {
        binding.luckyNumberEmpty.visibility = if (show) VISIBLE else GONE
    }

    override fun showErrorView(show: Boolean) {
        binding.luckyNumberError.visibility = if (show) VISIBLE else GONE
    }

    override fun setErrorDetails(message: String) {
        binding.luckyNumberErrorMessage.text = message
    }

    override fun updateNavigationWeek(date: String) {
        binding.luckyNumberNavDate.text = date
    }

    override fun showProgress(show: Boolean) {
        binding.luckyNumberProgress.visibility = if (show) VISIBLE else GONE
    }

    override fun enableSwipe(enable: Boolean) {
        binding.luckyNumberSwipe.isEnabled = enable
    }

    override fun showPreButton(show: Boolean) {
        binding.luckyNumberPreviousButton.visibility = if (show) VISIBLE else View.INVISIBLE
    }

    override fun showNextButton(show: Boolean) {
        binding.luckyNumberNextButton.visibility = if (show) VISIBLE else View.INVISIBLE
    }

    override fun showDatePickerDialog(currentDate: LocalDate) {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            presenter.onDateSet(year, month + 1, dayOfMonth)
        }
        val datePickerDialog = DatePickerDialog.newInstance(dateSetListener,
            currentDate.year, currentDate.monthValue - 1, currentDate.dayOfMonth)

        with(datePickerDialog) {
            setDateRangeLimiter(SchooldaysRangeLimiter())
            version = DatePickerDialog.Version.VERSION_2
            scrollOrientation = DatePickerDialog.ScrollOrientation.VERTICAL
            vibrate(false)
            show(this@LuckyNumberHistoryFragment.parentFragmentManager, null)
        }
    }

    override fun showContent(show: Boolean) {
        binding.luckyNumberRecycler.visibility = if (show) VISIBLE else GONE
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }
}
