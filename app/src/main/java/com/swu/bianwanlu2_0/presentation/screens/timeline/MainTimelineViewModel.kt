package com.swu.bianwanlu2_0.presentation.screens.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.CategoryType
import com.swu.bianwanlu2_0.data.local.entity.TimelineActionType
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import com.swu.bianwanlu2_0.data.local.entity.TimelineItemType
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.data.repository.TimelineEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class TimelineSourceTab(val label: String) {
    ALL("全部"),
    NOTE("笔记"),
    TODO("待办"),
}

enum class TimelineActionFilter(val label: String, val actionType: TimelineActionType?) {
    ALL("全部操作", null),
    CREATE("新增", TimelineActionType.CREATE),
    UPDATE("修改", TimelineActionType.UPDATE),
    DELETE("删除", TimelineActionType.DELETE),
    COMPLETE("完成", TimelineActionType.COMPLETE),
    REMINDER("提醒", TimelineActionType.REMINDER),
}

data class TimelineCategoryFilterOption(
    val id: Long,
    val label: String,
    val type: CategoryType,
)

private data class TimelineFilterState(
    val sourceTab: TimelineSourceTab,
    val actionFilter: TimelineActionFilter,
    val categoryId: Long?,
    val startDate: Long?,
    val endDate: Long?,
)

@HiltViewModel
class MainTimelineViewModel @Inject constructor(
    timelineEventRepository: TimelineEventRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _sourceTab = MutableStateFlow(TimelineSourceTab.ALL)
    val sourceTab: StateFlow<TimelineSourceTab> = _sourceTab.asStateFlow()

    private val _actionFilter = MutableStateFlow(TimelineActionFilter.ALL)
    val actionFilter: StateFlow<TimelineActionFilter> = _actionFilter.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    val categories: StateFlow<List<Category>> = categoryRepository
        .getAllCategories(TIMELINE_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allEvents: StateFlow<List<TimelineEvent>> = timelineEventRepository
        .getAllEvents(TIMELINE_USER_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val filterState = combine(
        _sourceTab,
        _actionFilter,
        _selectedCategoryId,
        _startDate,
        _endDate,
    ) { sourceTab, actionFilter, categoryId, startDate, endDate ->
        TimelineFilterState(sourceTab, actionFilter, categoryId, startDate, endDate)
    }

    val categoryOptions: StateFlow<List<TimelineCategoryFilterOption>> = combine(categories, _sourceTab) { list, tab ->
        list.filterBy(tab)
            .sortedWith(compareBy<Category> { it.type.ordinal }.thenBy { it.sortOrder }.thenBy { it.id })
            .map { category ->
                TimelineCategoryFilterOption(
                    id = category.id,
                    label = if (tab == TimelineSourceTab.ALL) {
                        "${category.type.displayLabel()} · ${category.name}"
                    } else {
                        category.name
                    },
                    type = category.type,
                )
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredEvents: StateFlow<List<TimelineEvent>> = combine(allEvents, filterState) { events, filter ->
        val startBound = filter.startDate?.let(::startOfDay)
        val endBound = filter.endDate?.let(::endOfDay)

        events.filter { event ->
            val matchesSource = when (filter.sourceTab) {
                TimelineSourceTab.ALL -> true
                TimelineSourceTab.NOTE -> event.itemType == TimelineItemType.NOTE
                TimelineSourceTab.TODO -> event.itemType == TimelineItemType.TODO
            }
            val matchesAction = filter.actionFilter.actionType == null || event.actionType == filter.actionFilter.actionType
            val matchesCategory = filter.categoryId == null || event.categoryId == filter.categoryId
            val matchesStart = startBound == null || event.occurredAt >= startBound
            val matchesEnd = endBound == null || event.occurredAt <= endBound
            matchesSource && matchesAction && matchesCategory && matchesStart && matchesEnd
        }.sortedWith(
            if (filter.sourceTab == TimelineSourceTab.ALL && filter.actionFilter == TimelineActionFilter.ALL) {
                compareBy<TimelineEvent> { if (it.actionType.isPinnedAction()) 0 else 1 }
                    .thenByDescending { it.occurredAt }
                    .thenByDescending { it.id }
            } else {
                compareByDescending<TimelineEvent> { it.occurredAt }
                    .thenByDescending { it.id }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSourceTab(tab: TimelineSourceTab) {
        _sourceTab.value = tab
        val selectedCategoryId = _selectedCategoryId.value ?: return
        val visibleCategoryIds = categories.value.filterBy(tab).map { it.id }.toSet()
        if (selectedCategoryId !in visibleCategoryIds) {
            _selectedCategoryId.value = null
        }
    }

    fun setActionFilter(filter: TimelineActionFilter) {
        _actionFilter.value = filter
    }

    fun setCategoryFilter(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun setStartDate(dateMillis: Long?) {
        if (dateMillis == null) {
            _startDate.value = null
            return
        }
        val normalized = startOfDay(dateMillis)
        _startDate.value = normalized
        val currentEnd = _endDate.value
        if (currentEnd != null && endOfDay(currentEnd) < normalized) {
            _endDate.value = normalized
        }
    }

    fun setEndDate(dateMillis: Long?) {
        if (dateMillis == null) {
            _endDate.value = null
            return
        }
        val normalized = startOfDay(dateMillis)
        _endDate.value = normalized
        val currentStart = _startDate.value
        if (currentStart != null && normalized < startOfDay(currentStart)) {
            _startDate.value = normalized
        }
    }

    private fun List<Category>.filterBy(tab: TimelineSourceTab): List<Category> {
        return when (tab) {
            TimelineSourceTab.ALL -> this
            TimelineSourceTab.NOTE -> filter { it.type == CategoryType.NOTE }
            TimelineSourceTab.TODO -> filter { it.type == CategoryType.TODO }
        }
    }

    private fun CategoryType.displayLabel(): String {
        return when (this) {
            CategoryType.NOTE -> "笔记"
            CategoryType.TODO -> "待办"
        }
    }

    private fun TimelineActionType.isPinnedAction(): Boolean {
        return this == TimelineActionType.COMPLETE || this == TimelineActionType.REMINDER
    }

    private fun startOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun endOfDay(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}

private const val TIMELINE_USER_ID = 1L
