package com.swu.bianwanlu2_0.presentation.screens.profile

import androidx.lifecycle.ViewModel
import com.swu.bianwanlu2_0.data.local.AccountSession
import com.swu.bianwanlu2_0.data.local.AccountSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val accountSessionStore: AccountSessionStore,
) : ViewModel() {
    val uiState: StateFlow<AccountSession> = accountSessionStore.session

    fun skipLogin() {
        accountSessionStore.markAuthChoiceHandled()
        accountSessionStore.setLoggedIn(false)
    }

    fun login(account: String, password: String): String? {
        val trimmedAccount = account.trim()
        if (trimmedAccount.isBlank()) return "请输入账号"
        if (password.isBlank()) return "请输入密码"
        if (!uiState.value.hasLocalAccount) return "当前暂无可登录账号，请先注册"
        if (!accountSessionStore.matchesCredentials(trimmedAccount, password)) {
            return "账号或密码不正确"
        }
        accountSessionStore.markAuthChoiceHandled()
        accountSessionStore.setLoggedIn(true)
        return null
    }

    fun register(account: String, password: String): String? {
        val trimmedAccount = account.trim()
        if (trimmedAccount.isBlank()) return "请输入账号"
        if (password.isBlank()) return "请输入密码"
        if (password.length < 6) return "密码长度不能少于6位"
        if (uiState.value.hasLocalAccount) {
            return "已存在本地账号，如需重新注册请先注销账号"
        }
        accountSessionStore.register(trimmedAccount, password)
        return null
    }

    fun updateNickname(nickname: String): String? {
        val trimmedNickname = nickname.trim()
        if (trimmedNickname.isBlank()) return "昵称不能为空"
        accountSessionStore.updateNickname(trimmedNickname)
        return null
    }

    fun updateAccount(account: String): String? {
        val trimmedAccount = account.trim()
        if (!uiState.value.isLoggedIn) return "请先登录或注册"
        if (trimmedAccount.isBlank()) return "账号不能为空"
        accountSessionStore.updateAccount(trimmedAccount)
        return null
    }

    fun updateAvatar(avatarUri: String?) {
        accountSessionStore.updateAvatar(avatarUri)
    }

    fun logout() {
        accountSessionStore.setLoggedIn(false)
    }

    fun cancelAccount() {
        accountSessionStore.clearAccount()
    }
}
