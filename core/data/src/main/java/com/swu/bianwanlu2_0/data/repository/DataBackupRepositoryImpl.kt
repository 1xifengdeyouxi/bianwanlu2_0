package com.swu.bianwanlu2_0.data.repository

import androidx.room.withTransaction
import com.swu.bianwanlu2_0.data.local.AppDatabase
import com.swu.bianwanlu2_0.data.local.CategorySelectionStore
import com.swu.bianwanlu2_0.data.local.dao.CategoryDao
import com.swu.bianwanlu2_0.data.local.dao.NoteDao
import com.swu.bianwanlu2_0.data.local.dao.TimelineEventDao
import com.swu.bianwanlu2_0.data.local.dao.TodoDao
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.data.local.entity.TimelineActionType
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import com.swu.bianwanlu2_0.data.local.entity.TimelineItemType
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.data.reminder.ReminderCoordinator
import com.swu.bianwanlu2_0.utils.GUEST_USER_ID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DataBackupRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val noteDao: NoteDao,
    private val todoDao: TodoDao,
    private val categoryDao: CategoryDao,
    private val timelineEventDao: TimelineEventDao,
    private val categorySelectionStore: CategorySelectionStore,
    private val reminderCoordinator: ReminderCoordinator,
) : DataBackupRepository {

    override suspend fun exportBackup(outputStream: java.io.OutputStream): BackupExportSummary =
        withContext(Dispatchers.IO) {
            val userId = GUEST_USER_ID
            val payload = BackupPayload(
                schemaVersion = BACKUP_SCHEMA_VERSION,
                exportedAt = System.currentTimeMillis(),
                sourceUserId = userId,
                selectedNoteCategoryId = categorySelectionStore.getSelectedCategoryId(CategoryType.NOTE),
                selectedTodoCategoryId = categorySelectionStore.getSelectedCategoryId(CategoryType.TODO),
                categories = categoryDao.getAllByUser(userId).first(),
                notes = noteDao.getAllByUser(userId).first(),
                todos = todoDao.getAllByUser(userId).first(),
                timelineEvents = timelineEventDao.getAllByUser(userId).first(),
            )
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(buildBackupJson(payload).toString(2))
            }
            BackupExportSummary(
                categoryCount = payload.categories.size,
                noteCount = payload.notes.size,
                todoCount = payload.todos.size,
                timelineEventCount = payload.timelineEvents.size,
            )
        }

    override suspend fun readBackup(inputStream: java.io.InputStream): BackupImportPreview =
        withContext(Dispatchers.IO) {
            val rawText = inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }
            require(rawText.isNotBlank()) { "备份文件为空" }

            val payload = parsePayload(JSONObject(rawText))
            BackupImportPreview(
                payload = payload,
                categoryCount = payload.categories.size,
                noteCount = payload.notes.size,
                todoCount = payload.todos.size,
                timelineEventCount = payload.timelineEvents.size,
                exportedAt = payload.exportedAt,
                warnings = buildWarnings(payload),
            )
        }

    override suspend fun restoreBackup(payload: BackupPayload): BackupRestoreSummary =
        withContext(Dispatchers.IO) {
            val userId = GUEST_USER_ID
            reminderCoordinator.clearAllScheduledState()
            runCatching {
                appDatabase.withTransaction {
                    timelineEventDao.deleteAllByUser(userId)
                    noteDao.deleteAllByUser(userId)
                    todoDao.deleteAllByUser(userId)
                    categoryDao.deleteAllByUser(userId)
                    categorySelectionStore.clearAll()

                    val restoredCategories = mutableListOf<Category>()
                    payload.categories
                        .sortedWith(compareBy<Category> { it.type.ordinal }.thenBy { it.sortOrder }.thenBy { it.id })
                        .forEach { category ->
                            val safeName = category.name.ifBlank { defaultNameFor(category.type) }
                            val insertedId = categoryDao.insert(
                                category.copy(
                                    name = safeName,
                                    userId = userId,
                                ),
                            )
                            restoredCategories += category.copy(
                                id = insertedId,
                                name = safeName,
                                userId = userId,
                            )
                        }

                    val noteDefault = ensureDefaultCategory(userId, CategoryType.NOTE, restoredCategories)
                    val todoDefault = ensureDefaultCategory(userId, CategoryType.TODO, restoredCategories)

                    val categoriesById = restoredCategories.associateBy { it.id }
                    val noteCategoryIds = restoredCategories.filter { it.type == CategoryType.NOTE }.map { it.id }.toSet()
                    val todoCategoryIds = restoredCategories.filter { it.type == CategoryType.TODO }.map { it.id }.toSet()

                    val restoredNotes = payload.notes.map { note ->
                        val resolvedCategoryId = note.categoryId.takeIf { it in noteCategoryIds } ?: noteDefault.id
                        note.copy(
                            categoryId = resolvedCategoryId,
                            userId = userId,
                            title = note.title,
                            content = note.content,
                        )
                    }
                    if (restoredNotes.isNotEmpty()) {
                        noteDao.insertAll(restoredNotes)
                    }

                    val restoredTodos = payload.todos.map { todo ->
                        val resolvedCategoryId = todo.categoryId.takeIf { it in todoCategoryIds } ?: todoDefault.id
                        todo.copy(
                            categoryId = resolvedCategoryId,
                            userId = userId,
                            title = todo.title,
                        )
                    }
                    if (restoredTodos.isNotEmpty()) {
                        todoDao.insertAll(restoredTodos)
                    }

                    val restoredTimelineEvents = payload.timelineEvents.map { event ->
                        val fallbackCategory = when (event.itemType) {
                            TimelineItemType.NOTE -> noteDefault
                            TimelineItemType.TODO -> todoDefault
                        }
                        val resolvedCategory = event.categoryId?.let(categoriesById::get) ?: fallbackCategory
                        event.copy(
                            categoryId = resolvedCategory.id,
                            categoryName = resolvedCategory.name,
                            userId = userId,
                        )
                    }
                    if (restoredTimelineEvents.isNotEmpty()) {
                        timelineEventDao.insertAll(restoredTimelineEvents)
                    }

                    categorySelectionStore.setSelectedCategoryId(
                        CategoryType.NOTE,
                        payload.selectedNoteCategoryId.takeIf { it in noteCategoryIds } ?: noteDefault.id,
                    )
                    categorySelectionStore.setSelectedCategoryId(
                        CategoryType.TODO,
                        payload.selectedTodoCategoryId.takeIf { it in todoCategoryIds } ?: todoDefault.id,
                    )

                    BackupRestoreSummary(
                        categoryCount = restoredCategories.size,
                        noteCount = restoredNotes.size,
                        todoCount = restoredTodos.size,
                        timelineEventCount = restoredTimelineEvents.size,
                    )
                }
            }.onFailure {
                reminderCoordinator.resyncAll()
            }.getOrElse { throw it }.also {
                reminderCoordinator.resyncAll()
            }
        }

    override suspend fun clearAllData(): BackupClearSummary = withContext(Dispatchers.IO) {
        val userId = GUEST_USER_ID
        val deletedCategories = categoryDao.getAllByUser(userId).first().size
        val deletedNotes = noteDao.getAllByUser(userId).first().size
        val deletedTodos = todoDao.getAllByUser(userId).first().size
        val deletedTimelineEvents = timelineEventDao.getAllByUser(userId).first().size

        reminderCoordinator.clearAllScheduledState()
        runCatching {
            appDatabase.withTransaction {
                timelineEventDao.deleteAllByUser(userId)
                noteDao.deleteAllByUser(userId)
                todoDao.deleteAllByUser(userId)
                categoryDao.deleteAllByUser(userId)
                categorySelectionStore.clearAll()

                val noteCategoryId = categoryDao.insert(
                    Category(
                        name = defaultNameFor(CategoryType.NOTE),
                        type = CategoryType.NOTE,
                        sortOrder = 0,
                        userId = userId,
                    ),
                )
                val todoCategoryId = categoryDao.insert(
                    Category(
                        name = defaultNameFor(CategoryType.TODO),
                        type = CategoryType.TODO,
                        sortOrder = 0,
                        userId = userId,
                    ),
                )
                categorySelectionStore.setSelectedCategoryId(CategoryType.NOTE, noteCategoryId)
                categorySelectionStore.setSelectedCategoryId(CategoryType.TODO, todoCategoryId)

                BackupClearSummary(
                    deletedCategoryCount = deletedCategories,
                    deletedNoteCount = deletedNotes,
                    deletedTodoCount = deletedTodos,
                    deletedTimelineEventCount = deletedTimelineEvents,
                )
            }
        }.onFailure {
            reminderCoordinator.resyncAll()
        }.getOrElse { throw it }
    }

    private suspend fun ensureDefaultCategory(
        userId: Long,
        type: CategoryType,
        categories: MutableList<Category>,
    ): Category {
        categories.firstOrNull { it.type == type }?.let { return it }
        val category = Category(
            name = defaultNameFor(type),
            type = type,
            sortOrder = nextSortOrder(categories, type),
            userId = userId,
        )
        val insertedId = categoryDao.insert(category)
        return category.copy(id = insertedId).also(categories::add)
    }

    private fun nextSortOrder(categories: List<Category>, type: CategoryType): Int {
        return categories.filter { it.type == type }.maxOfOrNull { it.sortOrder }?.plus(1) ?: 0
    }

    private fun buildWarnings(payload: BackupPayload): List<String> {
        val warnings = mutableListOf<String>()
        if (payload.schemaVersion > BACKUP_SCHEMA_VERSION) {
            warnings += "该备份来自较新版本，恢复后请检查显示是否完整。"
        }
        if (payload.notes.any { it.imageUris.isNotBlank() }) {
            warnings += "备份中包含图片引用，跨设备恢复时图片可能需要重新添加。"
        }
        if (payload.categories.none { it.type == CategoryType.NOTE }) {
            warnings += "备份中没有笔记分类，恢复时会自动补一个默认笔记分类。"
        }
        if (payload.categories.none { it.type == CategoryType.TODO }) {
            warnings += "备份中没有待办分类，恢复时会自动补一个默认待办分类。"
        }
        if (payload.notes.isEmpty() && payload.todos.isEmpty() && payload.timelineEvents.isEmpty()) {
            warnings += "该备份不包含笔记、待办和时间轴数据，恢复后仅保留默认分类。"
        }
        return warnings
    }

    private fun buildBackupJson(payload: BackupPayload): JSONObject {
        return JSONObject().apply {
            put("schema_version", payload.schemaVersion)
            putNullableLong("exported_at", payload.exportedAt)
            put("source_user_id", payload.sourceUserId)
            put(
                "selected_categories",
                JSONObject().apply {
                    putNullableLong("note", payload.selectedNoteCategoryId)
                    putNullableLong("todo", payload.selectedTodoCategoryId)
                },
            )
            put(
                "categories",
                JSONArray().apply {
                    payload.categories.forEach { category ->
                        put(
                            JSONObject().apply {
                                put("id", category.id)
                                put("name", category.name)
                                put("type", category.type.name)
                                put("sort_order", category.sortOrder)
                                put("created_at", category.createdAt)
                                put("user_id", category.userId)
                            },
                        )
                    }
                },
            )
            put(
                "notes",
                JSONArray().apply {
                    payload.notes.forEach { note ->
                        put(
                            JSONObject().apply {
                                put("id", note.id)
                                put("title", note.title)
                                put("content", note.content)
                                putNullableLong("category_id", note.categoryId)
                                put("status", note.status.name)
                                put("is_priority", note.isPriority)
                                putNullableLong("reminder_time", note.reminderTime)
                                put("card_color", note.cardColor)
                                put("text_color", note.textColor)
                                put("image_uris", note.imageUris)
                                put("sort_order", note.sortOrder)
                                put("created_at", note.createdAt)
                                put("updated_at", note.updatedAt)
                                put("user_id", note.userId)
                            },
                        )
                    }
                },
            )
            put(
                "todos",
                JSONArray().apply {
                    payload.todos.forEach { todo ->
                        put(
                            JSONObject().apply {
                                put("id", todo.id)
                                put("title", todo.title)
                                put("description", todo.description ?: "")
                                putNullableLong("category_id", todo.categoryId)
                                put("status", todo.status.name)
                                put("is_priority", todo.isPriority)
                                putNullableLong("reminder_time", todo.reminderTime)
                                put("card_color", todo.cardColor)
                                put("sort_order", todo.sortOrder)
                                put("created_at", todo.createdAt)
                                put("updated_at", todo.updatedAt)
                                putNullableLong("completed_at", todo.completedAt)
                                put("user_id", todo.userId)
                            },
                        )
                    }
                },
            )
            put(
                "timeline_events",
                JSONArray().apply {
                    payload.timelineEvents.forEach { event ->
                        put(
                            JSONObject().apply {
                                put("id", event.id)
                                put("item_id", event.itemId)
                                put("item_type", event.itemType.name)
                                put("action_type", event.actionType.name)
                                putNullableLong("category_id", event.categoryId)
                                put("category_name", event.categoryName)
                                put("title", event.title)
                                put("content_preview", event.contentPreview)
                                putNullableLong("reference_time", event.referenceTime)
                                put("occurred_at", event.occurredAt)
                                put("user_id", event.userId)
                            },
                        )
                    }
                },
            )
        }
    }

    private fun parsePayload(root: JSONObject): BackupPayload {
        val selectedCategories = root.optJSONObject("selected_categories")
        val categories = root.optJSONArray("categories").toCategoryList()
        val notes = root.optJSONArray("notes").toNoteList()
        val todos = root.optJSONArray("todos").toTodoList()
        val timelineEvents = root.optJSONArray("timeline_events").toTimelineEventList()

        return BackupPayload(
            schemaVersion = root.optNullableInt("schema_version") ?: BACKUP_SCHEMA_VERSION,
            exportedAt = root.optNullableLong("exported_at"),
            sourceUserId = root.optNullableLong("source_user_id") ?: GUEST_USER_ID,
            selectedNoteCategoryId = selectedCategories?.optNullableLong("note"),
            selectedTodoCategoryId = selectedCategories?.optNullableLong("todo"),
            categories = categories,
            notes = notes,
            todos = todos,
            timelineEvents = timelineEvents,
        )
    }

    private fun JSONArray?.toCategoryList(): List<Category> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                val type = parseCategoryType(item.opt("type"))
                add(
                    Category(
                        id = item.optNullableLong("id") ?: 0L,
                        name = item.optSafeString("name"),
                        type = type,
                        sortOrder = item.optNullableInt("sort_order") ?: 0,
                        createdAt = item.optNullableLong("created_at") ?: System.currentTimeMillis(),
                        userId = GUEST_USER_ID,
                    ),
                )
            }
        }
    }

    private fun JSONArray?.toNoteList(): List<Note> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    Note(
                        id = item.optNullableLong("id") ?: 0L,
                        title = item.optSafeString("title"),
                        content = item.optSafeString("content"),
                        categoryId = item.optNullableLong("category_id"),
                        status = parseNoteStatus(item.opt("status")),
                        isPriority = item.optNullableBoolean("is_priority") ?: false,
                        reminderTime = item.optNullableLong("reminder_time"),
                        cardColor = item.optNullableLong("card_color") ?: Note.DEFAULT_CARD_COLOR,
                        textColor = item.optNullableLong("text_color") ?: Note.DEFAULT_TEXT_COLOR,
                        imageUris = item.optSafeString("image_uris"),
                        sortOrder = item.optNullableLong("sort_order") ?: item.optNullableLong("updated_at") ?: System.currentTimeMillis(),
                        createdAt = item.optNullableLong("created_at") ?: System.currentTimeMillis(),
                        updatedAt = item.optNullableLong("updated_at") ?: System.currentTimeMillis(),
                        userId = GUEST_USER_ID,
                    ),
                )
            }
        }
    }

    private fun JSONArray?.toTodoList(): List<Todo> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    Todo(
                        id = item.optNullableLong("id") ?: 0L,
                        title = item.optSafeString("title"),
                        description = item.optSafeString("description").ifBlank { null },
                        categoryId = item.optNullableLong("category_id"),
                        status = parseTodoStatus(item.opt("status")),
                        isPriority = item.optNullableBoolean("is_priority") ?: false,
                        reminderTime = item.optNullableLong("reminder_time"),
                        cardColor = item.optNullableLong("card_color") ?: Todo.DEFAULT_CARD_COLOR,
                        sortOrder = item.optNullableLong("sort_order") ?: item.optNullableLong("updated_at") ?: System.currentTimeMillis(),
                        createdAt = item.optNullableLong("created_at") ?: System.currentTimeMillis(),
                        updatedAt = item.optNullableLong("updated_at") ?: System.currentTimeMillis(),
                        completedAt = item.optNullableLong("completed_at"),
                        userId = GUEST_USER_ID,
                    ),
                )
            }
        }
    }

    private fun JSONArray?.toTimelineEventList(): List<TimelineEvent> {
        if (this == null) return emptyList()
        return buildList(length()) {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    TimelineEvent(
                        id = item.optNullableLong("id") ?: 0L,
                        itemId = item.optNullableLong("item_id") ?: 0L,
                        itemType = parseTimelineItemType(item.opt("item_type")),
                        actionType = parseTimelineActionType(item.opt("action_type")),
                        categoryId = item.optNullableLong("category_id"),
                        categoryName = item.optSafeString("category_name"),
                        title = item.optSafeString("title"),
                        contentPreview = item.optSafeString("content_preview"),
                        referenceTime = item.optNullableLong("reference_time"),
                        occurredAt = item.optNullableLong("occurred_at") ?: System.currentTimeMillis(),
                        userId = GUEST_USER_ID,
                    ),
                )
            }
        }
    }

    private fun parseCategoryType(raw: Any?): CategoryType {
        return when (raw) {
            is Number -> CategoryType.entries.getOrElse(raw.toInt()) { CategoryType.NOTE }
            is String -> CategoryType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: CategoryType.NOTE
            else -> CategoryType.NOTE
        }
    }

    private fun parseNoteStatus(raw: Any?): NoteStatus {
        return when (raw) {
            is Number -> NoteStatus.entries.getOrElse(raw.toInt()) { NoteStatus.ACTIVE }
            is String -> NoteStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: NoteStatus.ACTIVE
            else -> NoteStatus.ACTIVE
        }
    }

    private fun parseTodoStatus(raw: Any?): TodoStatus {
        return when (raw) {
            is Number -> TodoStatus.entries.getOrElse(raw.toInt()) { TodoStatus.ACTIVE }
            is String -> TodoStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TodoStatus.ACTIVE
            else -> TodoStatus.ACTIVE
        }
    }

    private fun parseTimelineItemType(raw: Any?): TimelineItemType {
        return when (raw) {
            is Number -> TimelineItemType.entries.getOrElse(raw.toInt()) { TimelineItemType.NOTE }
            is String -> TimelineItemType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TimelineItemType.NOTE
            else -> TimelineItemType.NOTE
        }
    }

    private fun parseTimelineActionType(raw: Any?): TimelineActionType {
        return when (raw) {
            is Number -> TimelineActionType.entries.getOrElse(raw.toInt()) { TimelineActionType.CREATE }
            is String -> TimelineActionType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: TimelineActionType.CREATE
            else -> TimelineActionType.CREATE
        }
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
    }

    private fun JSONObject.optNullableBoolean(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return when (val value = opt(key)) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> null
        }
    }

    private fun JSONObject.optSafeString(key: String): String {
        return if (!has(key) || isNull(key)) "" else optString(key, "")
    }

    private fun JSONObject.putNullableLong(key: String, value: Long?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun defaultNameFor(type: CategoryType): String {
        return when (type) {
            CategoryType.NOTE -> "笔记"
            CategoryType.TODO -> "待办"
        }
    }

    private companion object {
        const val BACKUP_SCHEMA_VERSION = 1
    }
}
