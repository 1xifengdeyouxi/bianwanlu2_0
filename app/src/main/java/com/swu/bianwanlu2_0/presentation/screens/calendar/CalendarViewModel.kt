package com.swu.bianwanlu2_0.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swu.bianwanlu2_0.data.local.CurrentUserStore
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.NoteStatus
import com.swu.bianwanlu2_0.data.local.entity.Todo
import com.swu.bianwanlu2_0.data.local.entity.TodoStatus
import com.swu.bianwanlu2_0.data.repository.CategoryRepository
import com.swu.bianwanlu2_0.data.repository.NoteRepository
import com.swu.bianwanlu2_0.data.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val CALENDAR_PREVIEW_REMINDER_LIMIT = 2

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    noteRepository: NoteRepository,
    todoRepository: TodoRepository,
    categoryRepository: CategoryRepository,
    currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _displayMonthStart = MutableStateFlow(startOfMonth(System.currentTimeMillis()))
    val displayMonthStart: StateFlow<Long> = _displayMonthStart.asStateFlow()

    private val _isMonthPickerVisible = MutableStateFlow(false)
    val isMonthPickerVisible: StateFlow<Boolean> = _isMonthPickerVisible.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _refreshVersion = MutableStateFlow(0L)

    val toolbarTitle: StateFlow<String> = displayMonthStart
        .map(::formatToolbarMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), formatToolbarMonth(_displayMonthStart.value))

    private val allNotes = currentUserStore.currentUserId
        .flatMapLatest { userId ->
            noteRepository.getAllNotes(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allTodos = currentUserStore.currentUserId
        .flatMapLatest { userId ->
            todoRepository.getAllTodos(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allCategories = currentUserStore.currentUserId
        .flatMapLatest { userId ->
            categoryRepository.getAllCategories(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<CalendarUiState> = combine(
        allNotes,
        allTodos,
        allCategories,
        _displayMonthStart,
        _refreshVersion,
    ) { notes, todos, categories, monthStart, _ ->
        buildCalendarUiState(
            monthStart = monthStart,
            notes = notes,
            todos = todos,
            categories = categories,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildEmptyCalendarUiState())

    fun showMonthPicker() {
        _isMonthPickerVisible.value = true
    }

    fun dismissMonthPicker() {
        _isMonthPickerVisible.value = false
    }

    fun selectMonth(year: Int, month: Int) {
        _displayMonthStart.value = startOfMonth(year, month)
        _isMonthPickerVisible.value = false
    }

    fun goToPreviousMonth() {
        _displayMonthStart.value = shiftMonth(_displayMonthStart.value, -1)
        _isMonthPickerVisible.value = false
    }

    fun goToNextMonth() {
        _displayMonthStart.value = shiftMonth(_displayMonthStart.value, 1)
        _isMonthPickerVisible.value = false
    }

    fun jumpToToday() {
        _displayMonthStart.value = startOfMonth(System.currentTimeMillis())
        _isMonthPickerVisible.value = false
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            _isMonthPickerVisible.value = false
            _refreshVersion.value += 1
            delay(700)
            _isRefreshing.value = false
        }
    }

    private fun buildCalendarUiState(
        monthStart: Long,
        notes: List<Note>,
        todos: List<Todo>,
        categories: List<Category>,
    ): CalendarUiState {
        val categoryNameById = categories.associateBy(Category::id).mapValues { it.value.name }
        val monthReminders = buildReminderItems(notes, todos, categoryNameById)
        val firstVisibleDay = firstVisibleDayOfGrid(monthStart)
        val todayStart = startOfDay(System.currentTimeMillis())
        val reminderMap = monthReminders.groupBy { startOfDay(it.reminderTime) }

        val dayCells = (0 until 42).map { index ->
            val dayMillis = addDays(firstVisibleDay, index)
            val allReminders = reminderMap[dayMillis]
                .orEmpty()
                .sortedWith(
                    compareByDescending<CalendarReminderItemUi> { it.isPriority }
                        .thenBy { it.reminderTime }
                        .thenByDescending { it.createdAt },
                )
            val badge = buildSpecialDayBadge(dayMillis)
            CalendarDayCellUi(
                key = "calendar_day_$dayMillis",
                dateMillis = dayMillis,
                dayOfMonth = dayOfMonth(dayMillis),
                isCurrentMonth = isSameMonth(dayMillis, monthStart),
                isToday = dayMillis == todayStart,
                badgeLabel = badge?.label,
                badgeType = badge?.type,
                reminders = allReminders,
                hiddenReminderCount = (allReminders.size - CALENDAR_PREVIEW_REMINDER_LIMIT).coerceAtLeast(0),
                totalReminderCount = allReminders.size,
            )
        }

        val currentMonthReminderCount = monthReminders.count { isSameMonth(it.reminderTime, monthStart) }
        val displayedMonthCalendar = Calendar.getInstance().apply { timeInMillis = monthStart }
        val displayedYear = displayedMonthCalendar.get(Calendar.YEAR)
        val displayedMonth = displayedMonthCalendar.get(Calendar.MONTH) + 1
        val isShowingCurrentMonth = isSameMonth(monthStart, System.currentTimeMillis())

        return CalendarUiState(
            monthStartMillis = monthStart,
            toolbarTitle = formatToolbarMonth(monthStart),
            monthDescription = formatMonthDescription(monthStart),
            summaryText = if (currentMonthReminderCount == 0) {
                "本月暂无提醒事项"
            } else {
                "本月有 $currentMonthReminderCount 条提醒事项"
            },
            year = displayedYear,
            month = displayedMonth,
            weekLabels = CALENDAR_WEEK_LABELS,
            days = dayCells,
            isCurrentMonthDisplayed = isShowingCurrentMonth,
        )
    }

    private fun buildReminderItems(
        notes: List<Note>,
        todos: List<Todo>,
        categoryNameById: Map<Long, String>,
    ): List<CalendarReminderItemUi> {
        val noteItems = notes.mapNotNull { note ->
            val reminderTime = note.reminderTime ?: return@mapNotNull null
            CalendarReminderItemUi(
                uniqueKey = "note_${note.id}_$reminderTime",
                itemId = note.id,
                type = CalendarReminderItemType.NOTE,
                title = note.title.trim().takeIf { it.isNotBlank() }
                    ?: note.content.trim().takeIf { it.isNotBlank() }
                    ?: "笔记",
                content = note.content,
                categoryName = note.categoryId?.let(categoryNameById::get).orEmpty(),
                reminderTime = reminderTime,
                createdAt = note.createdAt,
                isCompleted = note.status == NoteStatus.COMPLETED,
                isPriority = note.isPriority,
                note = note,
            )
        }

        val todoItems = todos.mapNotNull { todo ->
            val reminderTime = todo.reminderTime ?: return@mapNotNull null
            CalendarReminderItemUi(
                uniqueKey = "todo_${todo.id}_$reminderTime",
                itemId = todo.id,
                type = CalendarReminderItemType.TODO,
                title = todo.title.trim().takeIf { it.isNotBlank() }
                    ?: todo.description.orEmpty().trim().takeIf { it.isNotBlank() }
                    ?: "待办",
                content = todo.description.orEmpty(),
                categoryName = todo.categoryId?.let(categoryNameById::get).orEmpty(),
                reminderTime = reminderTime,
                createdAt = todo.createdAt,
                isCompleted = todo.status == TodoStatus.COMPLETED,
                isPriority = todo.isPriority,
                todo = todo,
            )
        }

        return noteItems + todoItems
    }
}

private data class CalendarSpecialDayBadge(
    val label: String,
    val type: CalendarDayBadgeType,
)

private fun buildEmptyCalendarUiState(): CalendarUiState {
    val monthStart = startOfMonth(System.currentTimeMillis())
    return CalendarUiState(
        monthStartMillis = monthStart,
        toolbarTitle = formatToolbarMonth(monthStart),
        monthDescription = formatMonthDescription(monthStart),
        summaryText = "本月暂无提醒事项",
        year = Calendar.getInstance().get(Calendar.YEAR),
        month = Calendar.getInstance().get(Calendar.MONTH) + 1,
        weekLabels = CALENDAR_WEEK_LABELS,
        days = emptyList(),
        isCurrentMonthDisplayed = true,
    )
}

private fun buildSpecialDayBadge(dateMillis: Long): CalendarSpecialDayBadge? {
    val officialBadge = OFFICIAL_SPECIAL_DAYS[dateKey(dateMillis)]
    if (officialBadge != null) return officialBadge

    SOLAR_FESTIVALS[monthDayKey(dateMillis)]?.let { label ->
        return CalendarSpecialDayBadge(label = label, type = CalendarDayBadgeType.FESTIVAL)
    }

    return when (Calendar.getInstance().apply { timeInMillis = dateMillis }.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SATURDAY,
        Calendar.SUNDAY,
        -> CalendarSpecialDayBadge(label = "休", type = CalendarDayBadgeType.REST_DAY)

        else -> null
    }
}

private val SOLAR_FESTIVALS = mapOf(
    "1-1" to "元旦",
    "2-14" to "情人",
    "3-8" to "妇女",
    "5-1" to "劳动",
    "6-1" to "儿童",
    "9-10" to "教师",
    "10-1" to "国庆",
    "12-25" to "圣诞",
)

private val OFFICIAL_SPECIAL_DAYS: Map<Int, CalendarSpecialDayBadge> = buildMap {
    fun putSingle(year: Int, month: Int, day: Int, label: String, type: CalendarDayBadgeType) {
        put(composeDateKey(year, month, day), CalendarSpecialDayBadge(label, type))
    }

    fun putRange(
        startYear: Int,
        startMonth: Int,
        startDay: Int,
        endYear: Int,
        endMonth: Int,
        endDay: Int,
        label: String,
        type: CalendarDayBadgeType,
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, startYear)
            set(Calendar.MONTH, startMonth - 1)
            set(Calendar.DAY_OF_MONTH, startDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = Calendar.getInstance().apply {
            set(Calendar.YEAR, endYear)
            set(Calendar.MONTH, endMonth - 1)
            set(Calendar.DAY_OF_MONTH, endDay)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        while (calendar.timeInMillis <= end) {
            put(dateKey(calendar.timeInMillis), CalendarSpecialDayBadge(label, type))
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    putSingle(2025, 1, 1, "元旦", CalendarDayBadgeType.FESTIVAL)
    putSingle(2025, 1, 26, "班", CalendarDayBadgeType.WORK_DAY)
    putRange(2025, 1, 28, 2025, 2, 4, "春节", CalendarDayBadgeType.FESTIVAL)
    putSingle(2025, 2, 8, "班", CalendarDayBadgeType.WORK_DAY)
    putRange(2025, 4, 4, 2025, 4, 6, "清明", CalendarDayBadgeType.FESTIVAL)
    putSingle(2025, 4, 27, "班", CalendarDayBadgeType.WORK_DAY)
    putRange(2025, 5, 1, 2025, 5, 5, "劳动", CalendarDayBadgeType.FESTIVAL)
    putRange(2025, 5, 31, 2025, 6, 2, "端午", CalendarDayBadgeType.FESTIVAL)
    putSingle(2025, 9, 28, "班", CalendarDayBadgeType.WORK_DAY)
    putRange(2025, 10, 1, 2025, 10, 8, "国庆", CalendarDayBadgeType.FESTIVAL)
    putSingle(2025, 10, 11, "班", CalendarDayBadgeType.WORK_DAY)

    putRange(2026, 1, 1, 2026, 1, 3, "元旦", CalendarDayBadgeType.FESTIVAL)
    putSingle(2026, 1, 4, "班", CalendarDayBadgeType.WORK_DAY)
    putSingle(2026, 2, 14, "班", CalendarDayBadgeType.WORK_DAY)
    putRange(2026, 2, 15, 2026, 2, 23, "春节", CalendarDayBadgeType.FESTIVAL)
    putSingle(2026, 2, 28, "班", CalendarDayBadgeType.WORK_DAY)
    putRange(2026, 4, 4, 2026, 4, 6, "清明", CalendarDayBadgeType.FESTIVAL)
    putRange(2026, 5, 1, 2026, 5, 5, "劳动", CalendarDayBadgeType.FESTIVAL)
    putSingle(2026, 5, 9, "班", CalendarDayBadgeType.WORK_DAY)
    putRange(2026, 6, 19, 2026, 6, 21, "端午", CalendarDayBadgeType.FESTIVAL)
    putSingle(2026, 9, 20, "班", CalendarDayBadgeType.WORK_DAY)
    putRange(2026, 9, 25, 2026, 9, 27, "中秋", CalendarDayBadgeType.FESTIVAL)
    putRange(2026, 10, 1, 2026, 10, 7, "国庆", CalendarDayBadgeType.FESTIVAL)
    putSingle(2026, 10, 10, "班", CalendarDayBadgeType.WORK_DAY)
}

private val CALENDAR_WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")

private fun formatToolbarMonth(timeMillis: Long): String =
    SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(Date(timeMillis))

private fun formatMonthDescription(timeMillis: Long): String =
    SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(Date(timeMillis))

private fun startOfMonth(timeMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timeMillis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun startOfMonth(year: Int, month: Int): Long = Calendar.getInstance().apply {
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, month - 1)
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun shiftMonth(timeMillis: Long, delta: Int): Long = Calendar.getInstance().apply {
    timeInMillis = timeMillis
    add(Calendar.MONTH, delta)
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timeMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun addDays(timeMillis: Long, days: Int): Long = Calendar.getInstance().apply {
    timeInMillis = timeMillis
    add(Calendar.DAY_OF_MONTH, days)
}.let { startOfDay(it.timeInMillis) }

private fun firstVisibleDayOfGrid(monthStart: Long): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = monthStart }
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val offset = (dayOfWeek + 5) % 7
    calendar.add(Calendar.DAY_OF_MONTH, -offset)
    return startOfDay(calendar.timeInMillis)
}

private fun isSameMonth(first: Long, second: Long): Boolean {
    val firstCalendar = Calendar.getInstance().apply { timeInMillis = first }
    val secondCalendar = Calendar.getInstance().apply { timeInMillis = second }
    return firstCalendar.get(Calendar.YEAR) == secondCalendar.get(Calendar.YEAR) &&
        firstCalendar.get(Calendar.MONTH) == secondCalendar.get(Calendar.MONTH)
}

private fun dayOfMonth(timeMillis: Long): Int = Calendar.getInstance().apply { timeInMillis = timeMillis }
    .get(Calendar.DAY_OF_MONTH)

private fun dateKey(timeMillis: Long): Int {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return composeDateKey(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH),
    )
}

private fun composeDateKey(year: Int, month: Int, day: Int): Int = year * 10_000 + month * 100 + day

private fun monthDayKey(timeMillis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timeMillis }
    return "${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
}
