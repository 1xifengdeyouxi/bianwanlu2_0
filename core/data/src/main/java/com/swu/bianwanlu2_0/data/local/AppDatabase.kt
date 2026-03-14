package com.swu.bianwanlu2_0.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.swu.bianwanlu2_0.data.local.converter.Converters
import com.swu.bianwanlu2_0.data.local.dao.CategoryDao
import com.swu.bianwanlu2_0.data.local.dao.NoteDao
import com.swu.bianwanlu2_0.data.local.dao.TimelineEventDao
import com.swu.bianwanlu2_0.data.local.dao.TodoDao
import com.swu.bianwanlu2_0.data.local.entity.Category
import com.swu.bianwanlu2_0.data.local.entity.Note
import com.swu.bianwanlu2_0.data.local.entity.TimelineEvent
import com.swu.bianwanlu2_0.data.local.entity.Todo

@Database(
    entities = [Note::class, Todo::class, Category::class, TimelineEvent::class],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao
    abstract fun categoryDao(): CategoryDao
    abstract fun timelineEventDao(): TimelineEventDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        type INTEGER NOT NULL,
                        sort_order INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        user_id INTEGER NOT NULL
                    )"""
                )
                db.execSQL("ALTER TABLE notes ADD COLUMN category_id INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN category_id INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN reminder_time INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE todos ADD COLUMN card_color INTEGER NOT NULL DEFAULT ${0xFFFFF8E1L}")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN text_color INTEGER NOT NULL DEFAULT ${Note.DEFAULT_TEXT_COLOR}")
                db.execSQL("ALTER TABLE notes ADD COLUMN image_uris TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reminder_time INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE notes ADD COLUMN card_color INTEGER NOT NULL DEFAULT ${Note.DEFAULT_CARD_COLOR}")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE todos ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE todos SET sort_order = updated_at")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE notes SET sort_order = updated_at")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS timeline_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        item_id INTEGER NOT NULL,
                        item_type INTEGER NOT NULL,
                        action_type INTEGER NOT NULL,
                        category_id INTEGER,
                        category_name TEXT NOT NULL DEFAULT '',
                        title TEXT NOT NULL DEFAULT '',
                        content_preview TEXT NOT NULL DEFAULT '',
                        reference_time INTEGER,
                        occurred_at INTEGER NOT NULL,
                        user_id INTEGER NOT NULL
                    )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timeline_events_user_id ON timeline_events(user_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timeline_events_occurred_at ON timeline_events(occurred_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_timeline_events_category_id ON timeline_events(category_id)")

                db.execSQL(
                    """INSERT INTO timeline_events (
                        item_id, item_type, action_type, category_id, category_name, title,
                        content_preview, reference_time, occurred_at, user_id
                    )
                    SELECT
                        n.id,
                        0,
                        CASE
                            WHEN n.status = 1 THEN 3
                            WHEN n.reminder_time IS NOT NULL THEN 4
                            WHEN n.updated_at > n.created_at THEN 1
                            ELSE 0
                        END,
                        n.category_id,
                        COALESCE(c.name, '笔记'),
                        COALESCE(n.title, ''),
                        COALESCE(n.content, ''),
                        n.reminder_time,
                        CASE
                            WHEN n.status = 1 THEN n.updated_at
                            WHEN n.reminder_time IS NOT NULL THEN n.updated_at
                            WHEN n.updated_at > n.created_at THEN n.updated_at
                            ELSE n.created_at
                        END,
                        n.user_id
                    FROM notes n
                    LEFT JOIN categories c ON c.id = n.category_id"""
                )

                db.execSQL(
                    """INSERT INTO timeline_events (
                        item_id, item_type, action_type, category_id, category_name, title,
                        content_preview, reference_time, occurred_at, user_id
                    )
                    SELECT
                        t.id,
                        1,
                        CASE
                            WHEN t.status = 1 THEN 3
                            WHEN t.reminder_time IS NOT NULL THEN 4
                            WHEN t.updated_at > t.created_at THEN 1
                            ELSE 0
                        END,
                        t.category_id,
                        COALESCE(c.name, '待办'),
                        COALESCE(t.title, ''),
                        COALESCE(t.description, ''),
                        t.reminder_time,
                        CASE
                            WHEN t.status = 1 THEN COALESCE(t.completed_at, t.updated_at)
                            WHEN t.reminder_time IS NOT NULL THEN t.updated_at
                            WHEN t.updated_at > t.created_at THEN t.updated_at
                            ELSE t.created_at
                        END,
                        t.user_id
                    FROM todos t
                    LEFT JOIN categories c ON c.id = t.category_id"""
                )
            }
        }
    }
}
