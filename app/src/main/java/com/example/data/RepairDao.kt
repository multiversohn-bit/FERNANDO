package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RepairDao {
    @Query("SELECT * FROM repairs ORDER BY createdAt DESC")
    fun getAllRepairs(): Flow<List<Repair>>

    @Query("SELECT * FROM repairs")
    suspend fun getRepairsList(): List<Repair>

    @Query("SELECT * FROM repairs WHERE id = :id LIMIT 1")
    suspend fun getRepairById(id: Long): Repair?

    @Query("SELECT * FROM repairs WHERE uuid = :uuid LIMIT 1")
    suspend fun getRepairByUuid(uuid: String): Repair?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepair(repair: Repair): Long

    @Update
    suspend fun updateRepair(repair: Repair)

    @Delete
    suspend fun deleteRepair(repair: Repair)
}
