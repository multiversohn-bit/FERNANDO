package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "repairs")
data class Repair(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val brandModel: String,
    val clientName: String,
    val clientPhone: String,
    val issueDescription: String,
    val technicianName: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val photoPath1: String? = null,
    val photoPath2: String? = null,
    val photoPath3: String? = null,
    val price: Double = 0.0
)
