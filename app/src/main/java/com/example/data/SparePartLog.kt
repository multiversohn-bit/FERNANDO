package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "spare_part_logs")
data class SparePartLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val partName: String,
    val quantity: Int,
    val cost: Double = 0.0,
    val technicianName: String = "",
    val date: Long = System.currentTimeMillis()
)
