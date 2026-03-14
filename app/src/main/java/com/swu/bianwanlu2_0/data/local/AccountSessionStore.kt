package com.swu.bianwanlu2_0.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccountSession(
    val hasSeenAuthChoice: Boolean = false,
    val hasLocalAccount: Boolean = false,
    val isLoggedIn: Boolean = false,
    val account: String = "",
    val nickname: String = "",
    val avatarUri: String? = null,
) {
    val displayName: String
        get() = when {
            !isLoggedIn -> "点击登录"
            nickname.isNotBlank() -> nickname
            account.isNotBlank() -> account
            else -> "点击登录"
        }

    val secondaryText: String
        get() = when {
            isLoggedIn && account.isNotBlank() -> account
            isLoggedIn -> "已登录"
            hasLocalAccount -> "请先登录后使用相关功能"
            else -> "请先登录或注册"
        }
}

@Singleton
class AccountSessionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(readSession())

    val session: StateFlow<AccountSession> = _session.asStateFlow()

    fun markAuthChoiceHandled() {
        updateState {
            putBoolean(KEY_AUTH_CHOICE_HANDLED, true)
        }
    }

    fun register(account: String, password: String) {
        val trimmedAccount = account.trim()
        updateState {
            putBoolean(KEY_AUTH_CHOICE_HANDLED, true)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_ACCOUNT, trimmedAccount)
            putString(KEY_PASSWORD, password)
            if ((preferences.getString(KEY_NICKNAME, "") ?: "").isBlank()) {
                putString(KEY_NICKNAME, trimmedAccount)
            }
        }
    }

    fun matchesCredentials(account: String, password: String): Boolean {
        val currentAccount = preferences.getString(KEY_ACCOUNT, "")?.trim().orEmpty()
        val currentPassword = preferences.getString(KEY_PASSWORD, "") ?: ""
        return currentAccount.isNotBlank() && currentAccount == account.trim() && currentPassword == password
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        updateState {
            putBoolean(KEY_AUTH_CHOICE_HANDLED, true)
            putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            if (!isLoggedIn) {
                remove(KEY_NICKNAME)
                remove(KEY_AVATAR_URI)
            }
        }
    }

    fun updateNickname(nickname: String) {
        updateState {
            putString(KEY_NICKNAME, nickname.trim())
        }
    }

    fun updateAccount(account: String) {
        updateState {
            putString(KEY_ACCOUNT, account.trim())
        }
    }

    fun updateAvatar(avatarUri: String?) {
        updateState {
            if (avatarUri.isNullOrBlank()) {
                remove(KEY_AVATAR_URI)
            } else {
                putString(KEY_AVATAR_URI, avatarUri)
            }
        }
    }

    fun clearAccount() {
        updateState {
            putBoolean(KEY_AUTH_CHOICE_HANDLED, true)
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_ACCOUNT)
            remove(KEY_PASSWORD)
            remove(KEY_NICKNAME)
            remove(KEY_AVATAR_URI)
        }
    }

    private fun updateState(block: android.content.SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply(block).apply()
        _session.value = readSession()
    }

    private fun readSession(): AccountSession {
        val account = preferences.getString(KEY_ACCOUNT, "")?.trim().orEmpty()
        val password = preferences.getString(KEY_PASSWORD, "") ?: ""
        val hasLocalAccount = account.isNotBlank() && password.isNotBlank()
        return AccountSession(
            hasSeenAuthChoice = preferences.getBoolean(KEY_AUTH_CHOICE_HANDLED, false),
            hasLocalAccount = hasLocalAccount,
            isLoggedIn = hasLocalAccount && preferences.getBoolean(KEY_IS_LOGGED_IN, false),
            account = account,
            nickname = preferences.getString(KEY_NICKNAME, "")?.trim().orEmpty(),
            avatarUri = preferences.getString(KEY_AVATAR_URI, null)?.takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val PREFS_NAME = "account_session"
        const val KEY_AUTH_CHOICE_HANDLED = "auth_choice_handled"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_ACCOUNT = "account"
        const val KEY_PASSWORD = "password"
        const val KEY_NICKNAME = "nickname"
        const val KEY_AVATAR_URI = "avatar_uri"
    }
}
