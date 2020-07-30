package io.github.wulkanowy.ui.modules.grade.summary

import io.github.wulkanowy.data.Status
import io.github.wulkanowy.data.db.entities.GradeSummary
import io.github.wulkanowy.data.repositories.student.StudentRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.base.ErrorHandler
import io.github.wulkanowy.ui.modules.grade.GradeAverageProvider
import io.github.wulkanowy.ui.modules.grade.GradeDetailsWithAverage
import io.github.wulkanowy.utils.FirebaseAnalyticsHelper
import io.github.wulkanowy.utils.afterLoading
import io.github.wulkanowy.utils.flowWithResourceIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

class GradeSummaryPresenter @Inject constructor(
    errorHandler: ErrorHandler,
    studentRepository: StudentRepository,
    private val averageProvider: GradeAverageProvider,
    private val analytics: FirebaseAnalyticsHelper
) : BasePresenter<GradeSummaryView>(errorHandler, studentRepository) {

    private lateinit var lastError: Throwable

    override fun onAttachView(view: GradeSummaryView) {
        super.onAttachView(view)
        view.initView()
        errorHandler.showErrorMessage = ::showErrorViewOnError
    }

    fun onParentViewLoadData(semesterId: Int, forceRefresh: Boolean) {
        Timber.i("Loading grade summary data started")

        loadData(semesterId, forceRefresh)
        if (!forceRefresh) view?.showErrorView(false)
    }

    private fun loadData(semesterId: Int, forceRefresh: Boolean) {
        flowWithResourceIn {
            val student = studentRepository.getCurrentStudent()
            averageProvider.getGradesDetailsWithAverage(student, semesterId, forceRefresh)
        }.onEach {
            when (it.status) {
                Status.LOADING -> Timber.i("Loading grade summary started")
                Status.SUCCESS -> {
                    Timber.i("Loading grade summary result: Success")
                    view?.run {
                        showEmpty(it.data!!.isEmpty())
                        showContent(it.data.isNotEmpty())
                        showErrorView(false)
                        updateData(createGradeSummaryItems(it.data))
                    }
                    analytics.logEvent(
                        "load_data",
                        "type" to "grade_summary",
                        "items" to it.data!!.size
                    )
                }
                Status.ERROR -> {
                    Timber.i("Loading grade summary result: An exception occurred")
                    errorHandler.dispatch(it.error!!)
                }
            }
        }.afterLoading {
            view?.run {
                showRefresh(false)
                showProgress(false)
                enableSwipe(true)
                notifyParentDataLoaded(semesterId)
            }
        }.launch()
    }

    private fun showErrorViewOnError(message: String, error: Throwable) {
        view?.run {
            if (isViewEmpty) {
                lastError = error
                setErrorDetails(message)
                showErrorView(true)
                showEmpty(false)
            } else showError(message, error)
        }
    }

    fun onSwipeRefresh() {
        Timber.i("Force refreshing the grade summary")
        view?.notifyParentRefresh()
    }

    fun onRetry() {
        view?.run {
            showErrorView(false)
            showProgress(true)
        }
        view?.notifyParentRefresh()
    }

    fun onDetailsClick() {
        view?.showErrorDetailsDialog(lastError)
    }

    fun onParentViewReselected() {
        view?.run {
            if (!isViewEmpty) resetView()
        }
    }

    fun onParentViewChangeSemester() {
        view?.run {
            showProgress(true)
            enableSwipe(false)
            showRefresh(false)
            showContent(false)
            showEmpty(false)
            clearView()
        }
        cancelJobs("load")
    }

    private fun createGradeSummaryItems(items: List<GradeDetailsWithAverage>): List<GradeSummary> {
        return items
            .filter { !checkEmpty(it) }
            .sortedBy { it.subject }
            .map { it.summary.copy(average = it.average) }
    }

    private fun checkEmpty(gradeSummary: GradeDetailsWithAverage): Boolean {
        return gradeSummary.run {
            summary.finalGrade.isBlank()
                && summary.predictedGrade.isBlank()
                && average == .0
                && points.isBlank()
        }
    }
}
