package com.soundamplifier.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AudiogramDao {
    @Query("SELECT * FROM audiogram_profiles ORDER BY createdAt DESC")
    fun getAllProfiles(): Flow<List<AudiogramProfile>>

    @Query("SELECT * FROM audiogram_profiles ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestProfile(): AudiogramProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AudiogramProfile): Long

    @Delete
    suspend fun deleteProfile(profile: AudiogramProfile)
}

@Database(entities = [AudiogramProfile::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audiogramDao(): AudiogramDao
}

class ProfileRepository(private val dao: AudiogramDao) {
    val allProfiles: Flow<List<AudiogramProfile>> = dao.getAllProfiles()

    suspend fun getLatestProfile(): AudiogramProfile? = dao.getLatestProfile()

    suspend fun saveProfile(profile: AudiogramProfile): Long = dao.insertProfile(profile)

    suspend fun deleteProfile(profile: AudiogramProfile) = dao.deleteProfile(profile)
}
