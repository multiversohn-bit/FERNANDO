package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SparePartLogDao {
    @Query("SELECT * FROM spare_part_logs ORDER BY date DESC")
    fun getAllSparePartLogs(): Flow<List<SparePartLog>>

    @Query("SELECT * FROM spare_part_logs")
    suspend fun getSparePartLogsList(): List<SparePartLog>

    @Query("SELECT * FROM spare_part_logs WHERE uuid = :uuid LIMIT 1")
    suspend fun getSparePartLogByUuid(uuid: String): SparePartLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSparePartLog(log: SparePartLog): Long

    @Delete
    suspend fun deleteSparePartLog(log: SparePartLog)
}
