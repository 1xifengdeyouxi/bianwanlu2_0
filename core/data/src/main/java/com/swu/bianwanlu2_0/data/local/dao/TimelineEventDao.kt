package com.swu.bianwanlu2_0.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TimelineEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<TimelineEvent>)

    @Query("SELECT * FROM timeline_events WHERE user_id = :userId ORDER BY occurred_at DESC, id DESC")
    fun getAllByUser(userId: Long): Flow<List<TimelineEvent>>
}
