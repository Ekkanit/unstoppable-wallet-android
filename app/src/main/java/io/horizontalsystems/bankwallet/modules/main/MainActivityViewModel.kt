package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.UserManager
import io.horizontalsystems.core.IKeyStoreManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.security.KeyStoreValidationResult
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val userManager: UserManager,
    private val accountManager: IAccountManager,
    private val pinComponent: IPinComponent,
    private val systemInfoManager: ISystemInfoManager,
    private val keyStoreManager: IKeyStoreManager,
    private val localStorage: ILocalStorage
) : ViewModel() {

    val navigateToMainLiveData = MutableLiveData(false)

    init {
        viewModelScope.launch {
            userManager.currentUserLevelFlow.collect {
                navigateToMainLiveData.postValue(true)
            }
        }
    }

    fun getPage() = when {
        systemInfoManager.isSystemLockOff -> Page.NoSystemLock
        else -> when (keyStoreManager.validateKeyStore()) {
            KeyStoreValidationResult.UserNotAuthenticated -> Page.UserAuthentication
            KeyStoreValidationResult.KeyIsInvalid -> Page.KeyInvalidated
            KeyStoreValidationResult.KeyIsValid -> when {
                accountManager.isAccountsEmpty && !localStorage.mainShowedOnce -> Page.Welcome
                pinComponent.isLocked -> Page.Unlock
                else -> Page.Main
            }
        }
    }

    fun onNavigatedToMain() {
        navigateToMainLiveData.postValue(false)
    }

    enum class Page {
        Welcome,
        Main,
        Unlock,
        NoSystemLock,
        KeyInvalidated,
        UserAuthentication
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainActivityViewModel(App.userManager, App.accountManager, App.pinComponent, App.systemInfoManager, App.keyStoreManager, App.localStorage) as T
        }
    }
}
