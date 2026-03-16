package com.swu.bianwanlu2_0.data.local

import androidx.room.withTransaction
import com.swu.bianwanlu2_0.data.local.dao.CategoryDao
import com.swu.bianwanlu2_0.data.local.dao.NoteDao
import com.swu.bianwanlu2_0.data.local.dao.TimelineEventDao
import com.swu.bianwanlu2_0.data.local.dao.TodoDao
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class UserDataIsolationManager @Inject constructor(
    private val appDatabase: AppDatabase,
    private val categoryDao: CategoryDao,
    private val noteDao: NoteDao,
    private val todoDao: TodoDao,
    private val timelineEventDao: TimelineEventDao,
    private val categorySelectionStore: CategorySelectionStore,
) {
    private val migrationMutex = Mutex()

    suspend fun migrateGuestDataToUserIfNeeded(targetUserId: Long): Boolean {
        if (targetUserId <= GUEST_USER_ID) return false

        return migrationMutex.withLock {
            val guestCategories = categoryDao.getAllByUser(GUEST_USER_ID).first()
            val guestNotes = noteDao.getAllByUser(GUEST_USER_ID).first()
            val guestTodos = todoDao.getAllByUser(GUEST_USER_ID).first()
            val guestTimelineEvents = timelineEventDao.getAllByUser(GUEST_USER_ID).first()
            val targetCategories = categoryDao.getAllByUser(targetUserId).first()
            val targetNotes = noteDao.getAllByUser(targetUserId).first()
            val targetTodos = todoDao.getAllByUser(targetUserId).first()
            val targetTimelineEvents = timelineEventDao.getAllByUser(targetUserId).first()

            val hasGuestData = guestCategories.isNotEmpty() ||
                guestNotes.isNotEmpty() ||
                guestTodos.isNotEmpty() ||
                guestTimelineEvents.isNotEmpty()
            val targetHasData = targetCategories.isNotEmpty() ||
                targetNotes.isNotEmpty() ||
                targetTodos.isNotEmpty() ||
                targetTimelineEvents.isNotEmpty()

            if (!hasGuestData || targetHasData) {
                return@withLock false
            }

            appDatabase.withTransaction {
                categoryDao.reassignUser(GUEST_USER_ID, targetUserId)
                noteDao.reassignUser(GUEST_USER_ID, targetUserId)
                todoDao.reassignUser(GUEST_USER_ID, targetUserId)
                timelineEventDao.reassignUser(GUEST_USER_ID, targetUserId)
                categorySelectionStore.copySelections(GUEST_USER_ID, targetUserId)
                categorySelectionStore.clearAll(GUEST_USER_ID)
            }

            true
        }
    }

    suspend fun clearUserData(userId: Long) {
        if (userId < 0L) return

        migrationMutex.withLock {
            appDatabase.withTransaction {
                timelineEventDao.deleteAllByUser(userId)
                noteDao.deleteAllByUser(userId)
                todoDao.deleteAllByUser(userId)
                categoryDao.deleteAllByUser(userId)
                categorySelectionStore.clearAll(userId)
            }
        }
    }

    suspend fun ensureDefaultGuestCategories() {
        if (categoryDao.getFirstByUserAndType(GUEST_USER_ID, CategoryType.NOTE) == null) {
            categoryDao.insert(
                Category(
                    name = "\u7b14\u8bb0",
                    type = CategoryType.NOTE,
                    sortOrder = 0,
                    userId = GUEST_USER_ID,
                ),
            )
        }
        if (categoryDao.getFirstByUserAndType(GUEST_USER_ID, CategoryType.TODO) == null) {
            categoryDao.insert(
                Category(
                    name = "\u5f85\u529e",
                    type = CategoryType.TODO,
                    sortOrder = 0,
                    userId = GUEST_USER_ID,
                ),
            )
        }
    }
}
