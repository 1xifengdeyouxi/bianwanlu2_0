package com.swu.bianwanlu2_0.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.repository.BackupPayload
import com.swu.bianwanlu2_0.data.repository.DataBackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DataAndSyncViewModel @Inject constructor(
    private val dataBackupRepository: DataBackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DataAndSyncUiState())
    val uiState: StateFlow<DataAndSyncUiState> = _uiState.asStateFlow()

    private var pendingImportPayload: BackupPayload? = null

    fun exportBackup(outputStream: OutputStream?) {
        if (outputStream == null) {
            showMessage("无法创建备份文件")
            return
        }
        viewModelScope.launch {
            setLoading(true, "正在导出数据…")
            runCatching {
                dataBackupRepository.exportBackup(outputStream)
            }.onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        message = "导出完成：${summary.noteCount}条笔记、${summary.todoCount}条待办、${summary.timelineEventCount}条时间轴记录",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        message = throwable.message ?: "导出失败，请稍后重试",
                    )
                }
            }
        }
    }

    fun prepareImport(inputStream: InputStream?) {
        if (inputStream == null) {
            showMessage("无法读取所选备份文件")
            return
        }
        viewModelScope.launch {
            setLoading(true, "正在读取备份…")
            runCatching {
                dataBackupRepository.readBackup(inputStream)
            }.onSuccess { preview ->
                pendingImportPayload = preview.payload
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        importPreview = DataImportPreviewUi(
                            exportedAt = preview.exportedAt,
                            categoryCount = preview.categoryCount,
                            noteCount = preview.noteCount,
                            todoCount = preview.todoCount,
                            timelineEventCount = preview.timelineEventCount,
                            warnings = preview.warnings,
                        ),
                    )
                }
            }.onFailure { throwable ->
                pendingImportPayload = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        importPreview = null,
                        message = throwable.message ?: "备份文件解析失败",
                    )
                }
            }
        }
    }

    fun dismissImportPreview() {
        pendingImportPayload = null
        _uiState.update { it.copy(importPreview = null) }
    }

    fun confirmImport() {
        val payload = pendingImportPayload ?: return
        viewModelScope.launch {
            setLoading(true, "正在恢复数据…")
            runCatching {
                dataBackupRepository.restoreBackup(payload)
            }.onSuccess { summary ->
                pendingImportPayload = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        importPreview = null,
                        message = "导入完成：${summary.noteCount}条笔记、${summary.todoCount}条待办已恢复",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        message = throwable.message ?: "导入失败，请检查备份文件",
                    )
                }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            setLoading(true, "正在清空本地数据…")
            runCatching {
                dataBackupRepository.clearAllData()
            }.onSuccess { summary ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        message = "数据已清空：删除${summary.deletedNoteCount}条笔记、${summary.deletedTodoCount}条待办，并已重建默认分类",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadingMessage = null,
                        message = throwable.message ?: "删除数据失败，请稍后重试",
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun showMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun setLoading(isLoading: Boolean, loadingMessage: String?) {
        _uiState.update {
            it.copy(
                isLoading = isLoading,
                loadingMessage = loadingMessage,
            )
        }
    }
}

data class DataAndSyncUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val importPreview: DataImportPreviewUi? = null,
    val message: String? = null,
)

data class DataImportPreviewUi(
    val exportedAt: Long?,
    val categoryCount: Int,
    val noteCount: Int,
    val todoCount: Int,
    val timelineEventCount: Int,
    val warnings: List<String>,
)
