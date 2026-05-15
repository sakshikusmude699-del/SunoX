package com.soundamplifier.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomPresetDao {
    @Query("SELECT * FROM custom_presets WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getAllPresetsFlowForAccount(accountId: String): Flow<List<CustomPreset>>

    @Query("SELECT * FROM custom_presets WHERE accountId = :accountId ORDER BY createdAt DESC")
    suspend fun getAllPresetsForAccount(accountId: String): List<CustomPreset>

    @Insert
    suspend fun insert(preset: CustomPreset): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(preset: CustomPreset): Long

    @Delete
    suspend fun delete(preset: CustomPreset)

    @Query("DELETE FROM custom_presets WHERE accountId = :accountId AND name = :name")
    suspend fun deleteByNameForAccount(accountId: String, name: String)

    @Query(
        "SELECT * FROM custom_presets WHERE accountId = :accountId AND builtInPresetId = :builtInId LIMIT 1",
    )
    suspend fun getBuiltInOverride(accountId: String, builtInId: String): CustomPreset?

    @Query("DELETE FROM custom_presets WHERE accountId = :accountId AND builtInPresetId = :builtInId")
    suspend fun deleteBuiltInOverride(accountId: String, builtInId: String)

    @Update
    suspend fun update(preset: CustomPreset)
}
