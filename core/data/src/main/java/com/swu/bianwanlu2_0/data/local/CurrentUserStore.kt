package com.swu.bianwanlu2_0.data.local

import android.content.Context
import android.content.SharedPreferences
import com.swu.bianwanlu2_0.utils.ACCOUNT_KEY_ACCOUNT
import com.swu.bianwanlu2_0.utils.ACCOUNT_KEY_ACCOUNT_USER_ID
import com.swu.bianwanlu2_0.utils.ACCOUNT_KEY_IS_LOGGED_IN
import com.swu.bianwanlu2_0.utils.ACCOUNT_KEY_NEXT_USER_ID
import com.swu.bianwanlu2_0.utils.ACCOUNT_KEY_PASSWORD
import com.swu.bianwanlu2_0.utils.ACCOUNT_SESSION_PREFS_NAME
import com.swu.bianwanlu2_0.utils.FIRST_REGISTERED_USER_ID
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserIdAllocation(
    val userId: Long,
    val isNewlyAllocated: Boolean,
)

@Singleton
class CurrentUserStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val preferences = context.getSharedPreferences(ACCOUNT_SESSION_PREFS_NAME, Context.MODE_PRIVATE)
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key in observedKeys) {
            _currentUserId.value = readCurrentUserId()
        }
    }
    private val _currentUserId = MutableStateFlow(readCurrentUserId())

    val currentUserId: StateFlow<Long> = _currentUserId.asStateFlow()

    init {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun peekCurrentUserId(): Long = _currentUserId.value

    fun peekAccountUserId(): Long? {
        return if (preferences.contains(ACCOUNT_KEY_ACCOUNT_USER_ID)) {
            preferences.getLong(ACCOUNT_KEY_ACCOUNT_USER_ID, -1L).takeIf { it >= FIRST_REGISTERED_USER_ID }
        } else {
            null
        }
    }

    fun ensureAccountUserId(): UserIdAllocation {
        peekAccountUserId()?.let { return UserIdAllocation(it, isNewlyAllocated = false) }

        val nextUserId = preferences
            .getLong(ACCOUNT_KEY_NEXT_USER_ID, FIRST_REGISTERED_USER_ID)
            .coerceAtLeast(FIRST_REGISTERED_USER_ID)

        preferences.edit()
            .putLong(ACCOUNT_KEY_ACCOUNT_USER_ID, nextUserId)
            .putLong(ACCOUNT_KEY_NEXT_USER_ID, nextUserId + 1L)
            .apply()

        _currentUserId.value = readCurrentUserId()
        return UserIdAllocation(nextUserId, isNewlyAllocated = true)
    }

    fun clearAccountUserId() {
        preferences.edit().remove(ACCOUNT_KEY_ACCOUNT_USER_ID).apply()
        _currentUserId.value = readCurrentUserId()
    }

    private fun readCurrentUserId(): Long {
        val account = preferences.getString(ACCOUNT_KEY_ACCOUNT, "")?.trim().orEmpty()
        val password = preferences.getString(ACCOUNT_KEY_PASSWORD, "") ?: ""
        val hasLocalAccount = account.isNotBlank() && password.isNotBlank()
        val isLoggedIn = hasLocalAccount && preferences.getBoolean(ACCOUNT_KEY_IS_LOGGED_IN, false)
        val accountUserId = peekAccountUserId()
        return if (isLoggedIn && accountUserId != null) {
            accountUserId
        } else {
            GUEST_USER_ID
        }
    }

    private companion object {
        val observedKeys = setOf(
            ACCOUNT_KEY_ACCOUNT,
            ACCOUNT_KEY_PASSWORD,
            ACCOUNT_KEY_IS_LOGGED_IN,
            ACCOUNT_KEY_ACCOUNT_USER_ID,
        )
    }
}
