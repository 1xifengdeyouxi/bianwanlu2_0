package com.swu.bianwanlu2_0.data.local

import android.content.Context
import com.swu.bianwanlu2_0.data.reminder.ReminderCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val ACCOUNT_SESSION_PREFS_NAME = "account_session"
private const val ACCOUNT_KEY_AUTH_CHOICE_HANDLED = "auth_choice_handled"
private const val ACCOUNT_KEY_IS_LOGGED_IN = "is_logged_in"
private const val ACCOUNT_KEY_ACCOUNT = "account"
private const val ACCOUNT_KEY_PASSWORD = "password"
private const val ACCOUNT_KEY_NICKNAME = "nickname"
private const val ACCOUNT_KEY_AVATAR_URI = "avatar_uri"
private const val ACCOUNT_KEY_ACCOUNT_USER_ID = "account_user_id"
private const val GUEST_USER_ID = 1L

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
            !isLoggedIn -> "游客"
            nickname.isNotBlank() -> nickname
            account.isNotBlank() -> account
            else -> "用户"
        }

    val secondaryText: String
        get() = when {
            isLoggedIn && account.isNotBlank() -> account
            isLoggedIn -> "已登录"
            hasLocalAccount -> "本地账号未登录"
            else -> "暂未登录"
        }
}

@Singleton
class AccountSessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val currentUserStore: CurrentUserStore,
    private val userDataIsolationManager: UserDataIsolationManager,
    private val reminderCoordinator: ReminderCoordinator,
    private val searchHistoryStore: SearchHistoryStore,
) {
    private val preferences = context.getSharedPreferences(ACCOUNT_SESSION_PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _session = MutableStateFlow(readSession())

    val session: StateFlow<AccountSession> = _session.asStateFlow()

    init {
        promoteLegacyLoggedInAccountIfNeeded()
    }

    fun markAuthChoiceHandled() {
        updateState {
            putBoolean(ACCOUNT_KEY_AUTH_CHOICE_HANDLED, true)
        }
    }

    fun register(account: String, password: String) {
        val trimmedAccount = account.trim()
        val previousUserId = currentUserStore.peekCurrentUserId()
        val allocation = currentUserStore.ensureAccountUserId()
        updateState {
            putBoolean(ACCOUNT_KEY_AUTH_CHOICE_HANDLED, true)
            putBoolean(ACCOUNT_KEY_IS_LOGGED_IN, true)
            putString(ACCOUNT_KEY_ACCOUNT, trimmedAccount)
            putString(ACCOUNT_KEY_PASSWORD, password)
            if ((preferences.getString(ACCOUNT_KEY_NICKNAME, "") ?: "").isBlank()) {
                putString(ACCOUNT_KEY_NICKNAME, trimmedAccount)
            }
        }
        handleUserStateChanged(previousUserId, allocation.isNewlyAllocated)
    }

    fun matchesCredentials(account: String, password: String): Boolean {
        val currentAccount = preferences.getString(ACCOUNT_KEY_ACCOUNT, "")?.trim().orEmpty()
        val currentPassword = preferences.getString(ACCOUNT_KEY_PASSWORD, "") ?: ""
        return currentAccount.isNotBlank() && currentAccount == account.trim() && currentPassword == password
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        val previousUserId = currentUserStore.peekCurrentUserId()
        val shouldAllocateUserId = isLoggedIn
        val allocation = if (shouldAllocateUserId) currentUserStore.ensureAccountUserId() else null
        updateState {
            putBoolean(ACCOUNT_KEY_AUTH_CHOICE_HANDLED, true)
            putBoolean(ACCOUNT_KEY_IS_LOGGED_IN, isLoggedIn)
            if (!isLoggedIn) {
                remove(ACCOUNT_KEY_NICKNAME)
                remove(ACCOUNT_KEY_AVATAR_URI)
            }
        }
        handleUserStateChanged(previousUserId, allocation?.isNewlyAllocated == true)
    }

    fun updateNickname(nickname: String) {
        updateState {
            putString(ACCOUNT_KEY_NICKNAME, nickname.trim())
        }
    }

    fun updateAccount(account: String) {
        updateState {
            putString(ACCOUNT_KEY_ACCOUNT, account.trim())
        }
    }

    fun updateAvatar(avatarUri: String?) {
        updateState {
            if (avatarUri.isNullOrBlank()) {
                remove(ACCOUNT_KEY_AVATAR_URI)
            } else {
                putString(ACCOUNT_KEY_AVATAR_URI, avatarUri)
            }
        }
    }

    fun clearAccount() {
        val previousUserId = currentUserStore.peekCurrentUserId()
        val accountUserId = currentUserStore.peekAccountUserId()
        updateState {
            putBoolean(ACCOUNT_KEY_AUTH_CHOICE_HANDLED, true)
            putBoolean(ACCOUNT_KEY_IS_LOGGED_IN, false)
            remove(ACCOUNT_KEY_ACCOUNT)
            remove(ACCOUNT_KEY_PASSWORD)
            remove(ACCOUNT_KEY_NICKNAME)
            remove(ACCOUNT_KEY_AVATAR_URI)
            remove(ACCOUNT_KEY_ACCOUNT_USER_ID)
        }
        handleAccountCleared(previousUserId, accountUserId)
    }

    private fun handleAccountCleared(previousUserId: Long, accountUserId: Long?) {
        val currentUserId = currentUserStore.peekCurrentUserId()
        scope.launch {
            if (accountUserId != null && previousUserId == accountUserId) {
                reminderCoordinator.handleCurrentUserChanged(previousUserId, currentUserId)
            }
            if (accountUserId != null) {
                userDataIsolationManager.clearUserData(accountUserId)
                searchHistoryStore.clearHistory(accountUserId)
            }
            if (currentUserId == GUEST_USER_ID) {
                userDataIsolationManager.ensureDefaultGuestCategories()
            }
        }
    }

    private fun handleUserStateChanged(previousUserId: Long, migrateGuestData: Boolean) {
        val currentUserId = currentUserStore.peekCurrentUserId()
        if (previousUserId == currentUserId && !migrateGuestData) return

        scope.launch {
            if (migrateGuestData && currentUserId != GUEST_USER_ID) {
                userDataIsolationManager.migrateGuestDataToUserIfNeeded(currentUserId)
            }
            if (currentUserId == GUEST_USER_ID) {
                userDataIsolationManager.ensureDefaultGuestCategories()
            }
            reminderCoordinator.handleCurrentUserChanged(previousUserId, currentUserId)
        }
    }

    private fun promoteLegacyLoggedInAccountIfNeeded() {
        val session = _session.value
        if (!session.isLoggedIn || currentUserStore.peekAccountUserId() != null) return

        val previousUserId = currentUserStore.peekCurrentUserId()
        val allocation = currentUserStore.ensureAccountUserId()
        _session.value = readSession()
        handleUserStateChanged(previousUserId, allocation.isNewlyAllocated)
    }

    private fun updateState(block: android.content.SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply(block).apply()
        _session.value = readSession()
    }

    private fun readSession(): AccountSession {
        val account = preferences.getString(ACCOUNT_KEY_ACCOUNT, "")?.trim().orEmpty()
        val password = preferences.getString(ACCOUNT_KEY_PASSWORD, "") ?: ""
        val hasLocalAccount = account.isNotBlank() && password.isNotBlank()
        return AccountSession(
            hasSeenAuthChoice = preferences.getBoolean(ACCOUNT_KEY_AUTH_CHOICE_HANDLED, false),
            hasLocalAccount = hasLocalAccount,
            isLoggedIn = hasLocalAccount && preferences.getBoolean(ACCOUNT_KEY_IS_LOGGED_IN, false),
            account = account,
            nickname = preferences.getString(ACCOUNT_KEY_NICKNAME, "")?.trim().orEmpty(),
            avatarUri = preferences.getString(ACCOUNT_KEY_AVATAR_URI, null)?.takeIf { it.isNotBlank() },
        )
    }
}
