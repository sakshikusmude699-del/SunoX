package com.soundamplifier.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiogramDao {
    @Query("SELECT * FROM audiogram_profiles WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getAllProfilesForAccount(accountId: String): Flow<List<AudiogramProfile>>

    @Query("SELECT * FROM audiogram_profiles WHERE accountId = :accountId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestProfileForAccount(accountId: String): AudiogramProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AudiogramProfile): Long

    @Delete
    suspend fun deleteProfile(profile: AudiogramProfile)
}

@Database(
    entities = [AudiogramProfile::class, CustomPreset::class],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audiogramDao(): AudiogramDao
    abstract fun customPresetDao(): CustomPresetDao
}

class ProfileRepository(private val dao: AudiogramDao) {

    suspend fun getLatestForAccount(accountId: String): AudiogramProfile? =
        dao.getLatestProfileForAccount(accountId)

    fun allProfilesForAccount(accountId: String): Flow<List<AudiogramProfile>> =
        dao.getAllProfilesForAccount(accountId)

    suspend fun saveProfile(profile: AudiogramProfile): Long = dao.insertProfile(profile)

    suspend fun deleteProfile(profile: AudiogramProfile) = dao.deleteProfile(profile)
}
