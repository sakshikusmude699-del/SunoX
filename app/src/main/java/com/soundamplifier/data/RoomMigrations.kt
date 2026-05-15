package com.soundamplifier.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE audiogram_profiles ADD COLUMN accountId TEXT NOT NULL DEFAULT '${AccountLocalIds.GUEST}'",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_audiogram_profiles_accountId_createdAt` ON `audiogram_profiles` (`accountId`, `createdAt`)",
        )
        db.execSQL(
            "ALTER TABLE custom_presets ADD COLUMN accountId TEXT NOT NULL DEFAULT '${AccountLocalIds.GUEST}'",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_custom_presets_accountId_createdAt` ON `custom_presets` (`accountId`, `createdAt`)",
        )
    }
}

/** Repairs DBs that reached v3 without the accountId/createdAt indices (legacy 2→3 migration bug). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_audiogram_profiles_accountId_createdAt` ON `audiogram_profiles` (`accountId`, `createdAt`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_custom_presets_accountId_createdAt` ON `custom_presets` (`accountId`, `createdAt`)",
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE custom_presets ADD COLUMN iconKey TEXT NOT NULL DEFAULT 'tune'")
        db.execSQL("ALTER TABLE custom_presets ADD COLUMN builtInPresetId TEXT DEFAULT NULL")
    }
}
