package com.bennyjon.auiandroid.data.chat.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for demo chat persistence.
 *
 * Version history:
 * - v1: Initial schema with `text` + `auiJson` columns.
 * - v2: Replaced `text` + `auiJson` with single `rawContent` column.
 */
@Database(
    entities = [ChatMessageEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {

    /** Returns the DAO for [ChatMessageEntity]. */
    abstract fun chatMessageDao(): ChatMessageDao
}
