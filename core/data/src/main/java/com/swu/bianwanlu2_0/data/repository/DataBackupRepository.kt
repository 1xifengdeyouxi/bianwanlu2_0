package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import com.swu.bianwanlu2_0.data.local.entity.Todo
import java.io.InputStream
import java.io.OutputStream

data class BackupPayload(
    val schemaVersion: Int,
    val exportedAt: Long?,
    val sourceUserId: Long,
    val selectedNoteCategoryId: Long?,
    val selectedTodoCategoryId: Long?,
    val categories: List<Category>,
    val notes: List<Note>,
    val todos: List<Todo>,
    val timelineEvents: List<TimelineEvent>,
)

data class BackupImportPreview(
    val payload: BackupPayload,
    val categoryCount: Int,
    val noteCount: Int,
    val todoCount: Int,
    val timelineEventCount: Int,
    val exportedAt: Long?,
    val warnings: List<String>,
)

data class BackupExportSummary(
    val categoryCount: Int,
    val noteCount: Int,
    val todoCount: Int,
    val timelineEventCount: Int,
)

data class BackupRestoreSummary(
    val categoryCount: Int,
    val noteCount: Int,
    val todoCount: Int,
    val timelineEventCount: Int,
)

data class BackupClearSummary(
    val deletedCategoryCount: Int,
    val deletedNoteCount: Int,
    val deletedTodoCount: Int,
    val deletedTimelineEventCount: Int,
)

interface DataBackupRepository {
    suspend fun exportBackup(outputStream: OutputStream): BackupExportSummary

    suspend fun readBackup(inputStream: InputStream): BackupImportPreview

    suspend fun restoreBackup(payload: BackupPayload): BackupRestoreSummary

    suspend fun clearAllData(): BackupClearSummary
}
