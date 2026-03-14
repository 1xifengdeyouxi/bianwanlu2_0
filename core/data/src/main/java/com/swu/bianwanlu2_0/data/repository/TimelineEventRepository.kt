package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import kotlinx.coroutines.flow.Flow

interface TimelineEventRepository {
    fun getAllEvents(userId: Long): Flow<List<TimelineEvent>>
    suspend fun insert(event: TimelineEvent): Long
    suspend fun insertAll(events: List<TimelineEvent>)
}
