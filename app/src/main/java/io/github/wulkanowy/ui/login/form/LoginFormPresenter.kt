package io.github.wulkanowy.ui.login.form

import io.github.wulkanowy.data.repositories.SessionRepository
import io.github.wulkanowy.ui.base.BasePresenter
import io.github.wulkanowy.ui.login.LoginErrorHandler
import io.github.wulkanowy.utils.schedulers.SchedulersManager
import javax.inject.Inject

class LoginFormPresenter @Inject constructor(
        private val schedulers: SchedulersManager,
        private val errorHandler: LoginErrorHandler,
        private val sessionRepository: SessionRepository)
    : BasePresenter<LoginFormView>(errorHandler) {

    private var wasEmpty = false

    override fun onAttachView(view: LoginFormView) {
        super.onAttachView(view)
        view.initInputs()
    }

    fun attemptLogin(email: String, password: String, symbol: String, endpoint: String) {
        if (!validateCredentials(email, password, symbol)) return
        disposable.add(sessionRepository.getConnectedStudents(email, password, symbol, endpoint)
                .observeOn(schedulers.mainThread())
                .subscribeOn(schedulers.backgroundThread())
                .doOnSubscribe {
                    view?.run {
                        hideSoftKeyboard()
                        showLoginProgress(true)
                        errorHandler.doOnBadCredentials = {
                            setErrorPassIncorrect()
                            showSoftKeyboard()
                        }
                    }
                    sessionRepository.clearCache()
                }
                .doFinally { view?.showLoginProgress(false) }
                .subscribe({
                    view?.run {
                        if (it.isEmpty() && !wasEmpty) {
                            showSymbolInput()
                            wasEmpty = true
                        } else if (it.isEmpty() && wasEmpty) {
                            showSymbolInput()
                            setErrorSymbolIncorrect()
                        } else {
                            switchNextView()
                        }
                    }
                }, { errorHandler.proceed(it) }))
    }

    private fun validateCredentials(login: String, password: String, symbol: String): Boolean {
        var isCorrect = true

        if (login.isEmpty()) {
            view?.setErrorNicknameRequired()
            isCorrect = false
        }

        if (password.isEmpty()) {
            view?.setErrorPassRequired(focus = isCorrect)
            isCorrect = false
        }

        if (symbol.isEmpty() && wasEmpty) {
            view?.setErrorSymbolRequire()
            isCorrect = false
        }

        if (password.length < 6 && password.isNotEmpty()) {
            view?.setErrorPassInvalid(focus = isCorrect)
            isCorrect = false
        }
        return isCorrect
    }
}
