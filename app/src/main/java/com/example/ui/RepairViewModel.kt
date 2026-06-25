package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Repair
import com.example.data.RepairRepository
import com.example.data.SparePartLog
import com.example.data.SyncManager
import com.example.data.SyncResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class FilterType { ALL, PENDING, REPAIRED }

class RepairViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: RepairRepository
    private val prefs = application.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow(FilterType.ALL)

    // Sync state
    val syncCode = MutableStateFlow("")
    val isSyncing = MutableStateFlow(false)
    val syncResult = MutableStateFlow<SyncResult?>(null)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RepairRepository(database.repairDao(), database.sparePartLogDao())
        syncCode.value = prefs.getString("sync_code", "") ?: ""
    }

    val sparePartLogs: StateFlow<List<SparePartLog>> = repository.allSparePartLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val repairs: StateFlow<List<Repair>> = combine(
        repository.allRepairs,
        searchQuery,
        filterType
    ) { list, query, filter ->
        val filteredByQuery = if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.brandModel.contains(query, ignoreCase = true) ||
                it.clientName.contains(query, ignoreCase = true) ||
                it.clientPhone.contains(query, ignoreCase = true) ||
                it.technicianName.contains(query, ignoreCase = true) ||
                it.issueDescription.contains(query, ignoreCase = true)
            }
        }
        when (filter) {
            FilterType.ALL -> filteredByQuery
            FilterType.PENDING -> filteredByQuery.filter { !it.isCompleted }
            FilterType.REPAIRED -> filteredByQuery.filter { it.isCompleted }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertRepair(
        brandModel: String,
        clientName: String,
        clientPhone: String,
        issueDescription: String,
        photoPaths: List<String>,
        price: Double
    ) {
        viewModelScope.launch {
            val repair = Repair(
                brandModel = brandModel.trim(),
                clientName = clientName.trim(),
                clientPhone = clientPhone.trim(),
                issueDescription = issueDescription.trim(),
                photoPath1 = photoPaths.getOrNull(0),
                photoPath2 = photoPaths.getOrNull(1),
                photoPath3 = photoPaths.getOrNull(2),
                price = price,
                lastUpdated = System.currentTimeMillis()
            )
            repository.insert(repair)
            triggerAutoSync()
        }
    }

    fun updateRepairStatus(repair: Repair, isCompleted: Boolean, technicianName: String, price: Double) {
        viewModelScope.launch {
            val updated = repair.copy(
                isCompleted = isCompleted,
                technicianName = technicianName.trim(),
                completedAt = if (isCompleted) System.currentTimeMillis() else null,
                price = price,
                lastUpdated = System.currentTimeMillis()
            )
            repository.update(updated)
            triggerAutoSync()
        }
    }

    fun deleteRepair(repair: Repair) {
        viewModelScope.launch {
            repository.delete(repair)
            triggerAutoSync()
            // Clean up files in background
            val context = getApplication<Application>()
            listOfNotNull(repair.photoPath1, repair.photoPath2, repair.photoPath3).forEach { path ->
                try {
                    val file = File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun savePhotoFromUri(uri: Uri): String? {
        val context = getApplication<Application>()
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "repair_photo_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val file = File(context.filesDir, fileName)
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun savePhotoFromBitmap(bitmap: Bitmap): String? {
        val context = getApplication<Application>()
        return try {
            val fileName = "repair_photo_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
            val file = File(context.filesDir, fileName)
            file.outputStream().use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun insertSparePartLog(partName: String, quantity: Int, cost: Double, technicianName: String) {
        viewModelScope.launch {
            val log = SparePartLog(
                partName = partName.trim(),
                quantity = quantity,
                cost = cost,
                technicianName = technicianName.trim(),
                lastUpdated = System.currentTimeMillis()
            )
            repository.insertSparePartLog(log)
            triggerAutoSync()
        }
    }

    fun deleteSparePartLog(log: SparePartLog) {
        viewModelScope.launch {
            repository.deleteSparePartLog(log)
            triggerAutoSync()
        }
    }

    fun saveSyncCode(code: String) {
        syncCode.value = code
        prefs.edit().putString("sync_code", code).apply()
    }

    fun syncWithCloud(code: String) {
        val finalCode = code.trim()
        if (finalCode.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            isSyncing.value = true
            syncResult.value = null
            
            val result = SyncManager.performSync(finalCode, repository)
            
            syncResult.value = result
            isSyncing.value = false
            
            if (result is SyncResult.Success) {
                saveSyncCode(finalCode)
            }
        }
    }

    private fun triggerAutoSync() {
        val storedCode = syncCode.value
        if (storedCode.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                SyncManager.performSync(storedCode, repository)
            }
        }
    }
}
