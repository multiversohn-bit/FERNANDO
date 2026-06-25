package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    data class Success(val repairsSynced: Int, val partsSynced: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val syncTime: Long,
    val repairs: List<SyncRepair>,
    val spareParts: List<SyncSparePart>
)

@JsonClass(generateAdapter = true)
data class SyncRepair(
    val uuid: String,
    val lastUpdated: Long,
    val brandModel: String,
    val clientName: String,
    val clientPhone: String,
    val issueDescription: String,
    val technicianName: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long,
    val completedAt: Long? = null,
    val price: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class SyncSparePart(
    val uuid: String,
    val lastUpdated: Long,
    val partName: String,
    val quantity: Int,
    val cost: Double = 0.0,
    val technicianName: String = "",
    val date: Long
)

object SyncManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(SyncPayload::class.java)
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun performSync(syncCode: String, repository: RepairRepository): SyncResult {
        if (syncCode.isBlank()) {
            return SyncResult.Error("El código de sincronización no puede estar vacío")
        }

        val cleanCode = syncCode.trim().lowercase()
        // Generate a fully unique public path for the user's sync code
        val url = "https://kvdb.io/aistudio_repairs_sync_v1/code_$cleanCode"

        try {
            // 1. Fetch remote sync payload
            val getRequest = Request.Builder()
                .url(url)
                .get()
                .build()

            var remotePayload: SyncPayload? = null

            try {
                client.newCall(getRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string()
                        if (!bodyString.isNullOrBlank()) {
                            remotePayload = adapter.fromJson(bodyString)
                        }
                    } else if (response.code != 404) {
                        return SyncResult.Error("Error de red: Servidor retornó código ${response.code}")
                    }
                }
            } catch (e: Exception) {
                // If network fails completely on GET
                return SyncResult.Error("No se pudo conectar al servidor. Revisa tu conexión de red: ${e.localizedMessage}")
            }

            // 2. Load all local records
            val localRepairs = repository.getRepairsList()
            val localParts = repository.getSparePartLogsList()

            val localRepairsByUuid = localRepairs.associateBy { it.uuid }
            val localPartsByUuid = localParts.associateBy { it.uuid }

            var repairsSyncedCount = 0
            var partsSyncedCount = 0

            // 3. Merge repairs from remote to local
            remotePayload?.repairs?.forEach { remote ->
                val local = localRepairsByUuid[remote.uuid]
                if (local != null) {
                    // Update local if remote is newer
                    if (remote.lastUpdated > local.lastUpdated) {
                        val updated = local.copy(
                            brandModel = remote.brandModel,
                            clientName = remote.clientName,
                            clientPhone = remote.clientPhone,
                            issueDescription = remote.issueDescription,
                            technicianName = remote.technicianName,
                            isCompleted = remote.isCompleted,
                            createdAt = remote.createdAt,
                            completedAt = remote.completedAt,
                            price = remote.price,
                            lastUpdated = remote.lastUpdated
                        )
                        repository.update(updated)
                        repairsSyncedCount++
                    }
                } else {
                    // Insert remote as new local repair
                    val newRepair = Repair(
                        id = 0, // Auto-generate
                        uuid = remote.uuid,
                        lastUpdated = remote.lastUpdated,
                        brandModel = remote.brandModel,
                        clientName = remote.clientName,
                        clientPhone = remote.clientPhone,
                        issueDescription = remote.issueDescription,
                        technicianName = remote.technicianName,
                        isCompleted = remote.isCompleted,
                        createdAt = remote.createdAt,
                        completedAt = remote.completedAt,
                        price = remote.price
                    )
                    repository.insert(newRepair)
                    repairsSyncedCount++
                }
            }

            // 4. Merge spare parts from remote to local
            remotePayload?.spareParts?.forEach { remote ->
                val local = localPartsByUuid[remote.uuid]
                if (local != null) {
                    // Update local if remote is newer
                    if (remote.lastUpdated > local.lastUpdated) {
                        val updated = local.copy(
                            partName = remote.partName,
                            quantity = remote.quantity,
                            cost = remote.cost,
                            technicianName = remote.technicianName,
                            date = remote.date,
                            lastUpdated = remote.lastUpdated
                        )
                        // Local update helper
                        repository.deleteSparePartLog(local) // Replacing is safer to update keys
                        repository.insertSparePartLog(updated)
                        partsSyncedCount++
                    }
                } else {
                    // Insert remote as new local spare part
                    val newPart = SparePartLog(
                        id = 0, // Auto-generate
                        uuid = remote.uuid,
                        lastUpdated = remote.lastUpdated,
                        partName = remote.partName,
                        quantity = remote.quantity,
                        cost = remote.cost,
                        technicianName = remote.technicianName,
                        date = remote.date
                    )
                    repository.insertSparePartLog(newPart)
                    partsSyncedCount++
                }
            }

            // 5. Gather absolute merged state from local database to push back to cloud
            val finalLocalRepairs = repository.getRepairsList()
            val finalLocalParts = repository.getSparePartLogsList()

            val mergedPayload = SyncPayload(
                syncTime = System.currentTimeMillis(),
                repairs = finalLocalRepairs.map {
                    SyncRepair(
                        uuid = it.uuid,
                        lastUpdated = it.lastUpdated,
                        brandModel = it.brandModel,
                        clientName = it.clientName,
                        clientPhone = it.clientPhone,
                        issueDescription = it.issueDescription,
                        technicianName = it.technicianName,
                        isCompleted = it.isCompleted,
                        createdAt = it.createdAt,
                        completedAt = it.completedAt,
                        price = it.price
                    )
                },
                spareParts = finalLocalParts.map {
                    SyncSparePart(
                        uuid = it.uuid,
                        lastUpdated = it.lastUpdated,
                        partName = it.partName,
                        quantity = it.quantity,
                        cost = it.cost,
                        technicianName = it.technicianName,
                        date = it.date
                    )
                }
            )

            // 6. Serialize and PUT back to cloud
            val jsonString = adapter.toJson(mergedPayload)
            val putBody = jsonString.toRequestBody(jsonMediaType)
            val putRequest = Request.Builder()
                .url(url)
                .put(putBody)
                .build()

            client.newCall(putRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    return SyncResult.Error("Sincronización local exitosa, pero no se pudo actualizar la nube (Código: ${response.code})")
                }
            }

            return SyncResult.Success(
                repairsSynced = finalLocalRepairs.size,
                partsSynced = finalLocalParts.size
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return SyncResult.Error("Error inesperado en sincronización: ${e.localizedMessage}")
        }
    }
}
