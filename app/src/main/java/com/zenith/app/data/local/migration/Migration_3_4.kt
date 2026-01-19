package com.zenith.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 3 to 4
 * Adds schedule-related columns to tasks table
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // スケジュールタイプ (none/repeat/deadline/specific)
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN scheduleType TEXT DEFAULT NULL"
        )

        // 繰り返し曜日 ("1,3,5" 形式、1=月曜, 7=日曜)
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN repeatDays TEXT DEFAULT NULL"
        )

        // 期限日
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN deadlineDate TEXT DEFAULT NULL"
        )

        // 特定日
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN specificDate TEXT DEFAULT NULL"
        )

        // 最終学習日時
        database.execSQL(
            "ALTER TABLE tasks ADD COLUMN lastStudiedAt TEXT DEFAULT NULL"
        )
    }
}
