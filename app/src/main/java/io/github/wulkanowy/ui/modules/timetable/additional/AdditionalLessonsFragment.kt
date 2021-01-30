package io.github.wulkanowy.ui.modules.timetable.additional

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import dagger.hilt.android.AndroidEntryPoint
import io.github.wulkanowy.R
import io.github.wulkanowy.data.db.entities.TimetableAdditional
import io.github.wulkanowy.databinding.FragmentTimetableAdditionalBinding
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.modules.main.MainView
import io.github.wulkanowy.ui.widgets.DividerItemDecoration
import io.github.wulkanowy.utils.SchooldaysRangeLimiter
import io.github.wulkanowy.utils.dpToPx
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class AdditionalLessonsFragment :
    BaseFragment<FragmentTimetableAdditionalBinding>(R.layout.fragment_timetable_additional),
    AdditionalLessonsView, MainView.TitledView {

    @Inject
    lateinit var presenter: AdditionalLessonsPresenter

    @Inject
    lateinit var additionalLessonsAdapter: AdditionalLessonsAdapter

    companion object {
        private const val SAVED_DATE_KEY = "CURRENT_DATE"

        fun newInstance() = AdditionalLessonsFragment()
    }

    override val titleStringId get() = R.string.additional_lessons_title

    override val isViewEmpty get() = additionalLessonsAdapter.items.isEmpty()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentTimetableAdditionalBinding.bind(view)
        messageContainer = binding.additionalLessonsRecycler
        presenter.onAttachView(this, savedInstanceState?.getLong(SAVED_DATE_KEY))
    }

    override fun initView() {
        with(binding.additionalLessonsRecycler) {
            layoutManager = LinearLayoutManager(context)
            adapter = additionalLessonsAdapter
            addItemDecoration(DividerItemDecoration(context))
        }

        with(binding) {
            additionalLessonsSwipe.setOnRefreshListener(presenter::onSwipeRefresh)
            additionalLessonsErrorRetry.setOnClickListener { presenter.onRetry() }

            additionalLessonsPreviousButton.setOnClickListener { presenter.onPreviousDay() }
            additionalLessonsNavDate.setOnClickListener { presenter.onPickDate() }
            additionalLessonsNextButton.setOnClickListener { presenter.onNextDay() }

            additionalLessonsNavContainer.setElevationCompat(requireContext().dpToPx(8f))
        }
    }

    override fun updateData(data: List<TimetableAdditional>) {
        with(additionalLessonsAdapter) {
            items = data
            notifyDataSetChanged()
        }
    }

    override fun clearData() {
        with(additionalLessonsAdapter) {
            items = emptyList()
            notifyDataSetChanged()
        }
    }

    override fun updateNavigationDay(date: String) {
        binding.additionalLessonsNavDate.text = date
    }

    override fun hideRefresh() {
        binding.additionalLessonsSwipe.isRefreshing = false
    }

    override fun showEmpty(show: Boolean) {
        binding.additionalLessonsEmpty.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showErrorView(show: Boolean) {
        binding.additionalLessonsError.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun setErrorDetails(message: String) {
        binding.additionalLessonsErrorMessage.text = message
    }

    override fun showProgress(show: Boolean) {
        binding.additionalLessonsProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun enableSwipe(enable: Boolean) {
        binding.additionalLessonsSwipe.isEnabled = enable
    }

    override fun showContent(show: Boolean) {
        binding.additionalLessonsRecycler.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showPreButton(show: Boolean) {
        binding.additionalLessonsPreviousButton.visibility = if (show) View.VISIBLE else View.INVISIBLE
    }

    override fun showNextButton(show: Boolean) {
        binding.additionalLessonsNextButton.visibility = if (show) View.VISIBLE else View.INVISIBLE
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
            show(this@AdditionalLessonsFragment.parentFragmentManager, null)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(SAVED_DATE_KEY, presenter.currentDate.toEpochDay())
    }

    override fun onDestroyView() {
        presenter.onDetachView()
        super.onDestroyView()
    }
}
