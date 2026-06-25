package com.example.data

import kotlinx.coroutines.flow.Flow

class RepairRepository(
    private val repairDao: RepairDao,
    private val sparePartLogDao: SparePartLogDao
) {
    val allRepairs: Flow<List<Repair>> = repairDao.getAllRepairs()
    val allSparePartLogs: Flow<List<SparePartLog>> = sparePartLogDao.getAllSparePartLogs()

    suspend fun getRepairsList(): List<Repair> {
        return repairDao.getRepairsList()
    }

    suspend fun getSparePartLogsList(): List<SparePartLog> {
        return sparePartLogDao.getSparePartLogsList()
    }

    suspend fun getRepairById(id: Long): Repair? {
        return repairDao.getRepairById(id)
    }

    suspend fun getRepairByUuid(uuid: String): Repair? {
        return repairDao.getRepairByUuid(uuid)
    }

    suspend fun getSparePartLogByUuid(uuid: String): SparePartLog? {
        return sparePartLogDao.getSparePartLogByUuid(uuid)
    }

    suspend fun insert(repair: Repair): Long {
        return repairDao.insertRepair(repair)
    }

    suspend fun update(repair: Repair) {
        repairDao.updateRepair(repair)
    }

    suspend fun delete(repair: Repair) {
        repairDao.deleteRepair(repair)
    }

    suspend fun insertSparePartLog(log: SparePartLog): Long {
        return sparePartLogDao.insertSparePartLog(log)
    }

    suspend fun deleteSparePartLog(log: SparePartLog) {
        sparePartLogDao.deleteSparePartLog(log)
    }
}
