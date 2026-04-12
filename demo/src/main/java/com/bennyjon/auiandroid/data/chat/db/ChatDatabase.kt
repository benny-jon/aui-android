package com.bennyjon.auiandroid.data.chat.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** Room database for demo chat persistence. */
@Database(
    entities = [ChatMessageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ChatDatabase : RoomDatabase() {

    /** Returns the DAO for [ChatMessageEntity]. */
    abstract fun chatMessageDao(): ChatMessageDao
}
