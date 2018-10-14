package io.github.wulkanowy.ui.main.grade.summary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.common.SmoothScrollLinearLayoutManager
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import io.github.wulkanowy.R
import io.github.wulkanowy.ui.base.BaseFragment
import io.github.wulkanowy.ui.main.grade.GradeFragment
import io.github.wulkanowy.ui.main.grade.GradeView
import kotlinx.android.synthetic.main.fragment_grade_summary.*
import javax.inject.Inject

class GradeSummaryFragment : BaseFragment(), GradeSummaryView, GradeView.GradeChildView {

    @Inject
    lateinit var presenter: GradeSummaryPresenter

    @Inject
    lateinit var gradeSummaryAdapter: FlexibleAdapter<AbstractFlexibleItem<*>>

    companion object {
        fun newInstance() = GradeSummaryFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_grade_summary, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        messageContainer = gradeSummaryRecycler
        presenter.onAttachView(this)
    }

    override fun initView() {
        gradeSummaryAdapter.setDisplayHeadersAtStartUp(true)

        gradeSummaryRecycler.run {
            layoutManager = SmoothScrollLinearLayoutManager(context)
            adapter = gradeSummaryAdapter
        }
        gradeSummarySwipe.setOnRefreshListener { presenter.onSwipeRefresh() }
    }

    override fun updateDataSet(data: List<GradeSummaryItem>, header: GradeSummaryScrollableHeader) {
        gradeSummaryAdapter.apply {
            updateDataSet(data, true)
            removeAllScrollableHeaders()
            addScrollableHeader(header)
        }
    }

    override fun clearView() {
        gradeSummaryAdapter.clear()
    }

    override fun resetView() {
        gradeSummaryAdapter.smoothScrollToPosition(0)
    }

    override fun isViewEmpty() = gradeSummaryAdapter.isEmpty

    override fun showContent(show: Boolean) {
        gradeSummaryRecycler.visibility = if (show) VISIBLE else INVISIBLE
    }

    override fun showEmpty(show: Boolean) {
        gradeSummaryEmpty.visibility = if (show) VISIBLE else INVISIBLE
    }

    override fun showProgress(show: Boolean) {
        gradeSummaryProgress.visibility = if (show) VISIBLE else GONE
    }

    override fun showRefresh(show: Boolean) {
        gradeSummarySwipe.isRefreshing = show
    }

    override fun onParentLoadData(semesterId: String, forceRefresh: Boolean) {
        presenter.onParentViewLoadData(semesterId, forceRefresh)
    }

    override fun onParentReselected() {
        presenter.onParentViewReselected()
    }

    override fun onParentChangeSemester() {
        presenter.onParentViewChangeSemester()
    }

    override fun notifyParentDataLoaded(semesterId: String) {
        (parentFragment as? GradeFragment)?.onChildFragmentLoaded(semesterId)
    }

    override fun notifyParentRefresh() {
        (parentFragment as? GradeFragment)?.onChildRefresh()
    }

    override fun predictedString() = getString(R.string.grade_summary_predicted_grade)

    override fun finalString() = getString(R.string.grade_summary_final_grade)

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDetachView()
    }
}
