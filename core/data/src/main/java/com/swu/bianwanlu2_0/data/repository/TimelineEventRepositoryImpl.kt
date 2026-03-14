package com.swu.bianwanlu2_0.data.repository

import com.swu.bianwanlu2_0.data.local.dao.TimelineEventDao
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimelineEventRepositoryImpl @Inject constructor(
    private val timelineEventDao: TimelineEventDao,
) : TimelineEventRepository {

    override fun getAllEvents(userId: Long): Flow<List<TimelineEvent>> =
        timelineEventDao.getAllByUser(userId)

    override suspend fun insert(event: TimelineEvent): Long =
        timelineEventDao.insert(event)

    override suspend fun insertAll(events: List<TimelineEvent>) =
        timelineEventDao.insertAll(events)
}
