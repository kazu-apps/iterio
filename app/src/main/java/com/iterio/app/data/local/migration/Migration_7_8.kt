package com.iterio.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 7 to 8
 * Adds deadline support to subject_groups
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE subject_groups ADD COLUMN hasDeadline INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE subject_groups ADD COLUMN deadlineDate TEXT DEFAULT NULL")
    }
}
