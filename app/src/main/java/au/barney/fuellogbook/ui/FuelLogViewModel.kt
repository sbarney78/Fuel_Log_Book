package au.barney.fuellogbook.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import au.barney.fuellogbook.data.FuelLogRepository
import au.barney.fuellogbook.data.FuelType
import au.barney.fuellogbook.data.LogEntry
import au.barney.fuellogbook.data.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FuelLogViewModel(val repository: FuelLogRepository) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repository.allVehicles.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val fuelTypes: StateFlow<List<FuelType>> = repository.allFuelTypes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _logs = repository.allLogs
    val filterVehicleId = MutableStateFlow<Int?>(null)
    val filterFuelType = MutableStateFlow<String?>(null)
    val filterDateFrom = MutableStateFlow<Long?>(null)
    val filterDateTo = MutableStateFlow<Long?>(null)

    val filteredLogs: StateFlow<List<LogEntry>> = combine(
        _logs,
        filterVehicleId,
        filterFuelType,
        filterDateFrom,
        filterDateTo
    ) { logs, vehicleId, fuelType, dateFrom, dateTo ->
        logs.filter { log ->
            (vehicleId == null || log.vehicleID == vehicleId) &&
            (fuelType == null || log.fuelType == fuelType) &&
            (dateFrom == null || log.dateOfFill >= dateFrom) &&
            (dateTo == null || log.dateOfFill <= dateTo)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setDateFilter(from: Long?, to: Long?) {
        filterDateFrom.value = from
        // Extend 'to' date to the end of the day (23:59:59.999)
        filterDateTo.value = to?.let { it + (24 * 60 * 60 * 1000) - 1 }
    }

    val logs: StateFlow<List<LogEntry>> = _logs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    fun setVehicleFilter(vehicleId: Int?) {
        filterVehicleId.value = vehicleId
    }

    fun setFuelTypeFilter(fuelType: String?) {
        filterFuelType.value = fuelType
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteLog(id)
        }
    }

    fun exportToCsv(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d("FuelLogViewModel", "Starting CSV export to $uri")
                val logsList = repository.getAllLogsSync()
                val vehiclesList = repository.getAllVehiclesSync()
                Log.d("FuelLogViewModel", "Exporting ${logsList.size} logs")
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { writer ->
                        writer.write("Rego,Vehicle,Date,Odo Start,Odo End,Distance,Fuel Type,Price/L,Litres,Total Cost,L/100km,Notes\n")
                        logsList.forEach { log ->
                            val vehicle = vehiclesList.find { it.vehID == log.vehicleID }
                            val cleanNotes = (log.notes ?: "").replace("\n", " ").replace("\r", " ").replace(",", " ")
                            writer.write("${vehicle?.rego ?: ""},")
                            writer.write("${vehicle?.vehicle ?: ""},")
                            writer.write("${dateFormat.format(Date(log.dateOfFill))},")
                            writer.write("${log.odometerStart},")
                            writer.write("${log.odometerEnd},")
                            writer.write("${log.distance},")
                            writer.write("${log.fuelType},")
                            writer.write("${log.costPerLitre},")
                            writer.write("${log.litres},")
                            writer.write("${log.cost},")
                            writer.write("${log.litersPer100},")
                            writer.write("$cleanNotes\n")
                        }
                        writer.flush()
                    }
                }
                Log.d("FuelLogViewModel", "CSV export completed successfully")
            } catch (e: Exception) {
                Log.e("FuelLogViewModel", "Error exporting CSV", e)
                e.printStackTrace()
            }
        }
    }

    fun backupDatabase(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            au.barney.fuellogbook.util.BackupManager.backupToUri(context, uri)
        }
    }

    fun restoreDatabase(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            au.barney.fuellogbook.util.BackupManager.restoreDatabase(context, uri) {
                // The app usually needs a restart after a database file replacement
                // because the Room instance might be pointing to the old file or have open connections.
            }
        }
    }

    fun addVehicle(rego: String, vehicleName: String) {
        viewModelScope.launch {
            repository.insertVehicle(Vehicle(rego = rego, vehicle = vehicleName))
        }
    }

    fun deleteVehicle(vehicle: Vehicle, onInUse: () -> Unit) {
        viewModelScope.launch {
            val count = repository.getLogCountForVehicle(vehicle.vehID)
            if (count == 0) {
                repository.deleteVehicle(vehicle.vehID)
            } else {
                onInUse()
            }
        }
    }

    fun addFuelType(name: String) {
        viewModelScope.launch {
            repository.insertFuelType(FuelType(fuelType = name))
        }
    }

    fun deleteFuelType(fuelType: FuelType, onInUse: () -> Unit) {
        viewModelScope.launch {
            val count = repository.getLogCountForFuelType(fuelType.fuelType)
            if (count == 0) {
                repository.deleteFuelType(fuelType.fuelTypeID)
            } else {
                onInUse()
            }
        }
    }

    fun addLog(logEntry: LogEntry) {
        viewModelScope.launch {
            repository.insertLog(logEntry)
        }
    }

    fun updateLog(logEntry: LogEntry) {
        viewModelScope.launch {
            repository.updateLog(logEntry)
        }
    }

    suspend fun getLatestLogForVehicle(vehicleId: Int): LogEntry? {
        return repository.getLatestLogForVehicle(vehicleId)
    }

    class Factory(private val repository: FuelLogRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FuelLogViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return FuelLogViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
