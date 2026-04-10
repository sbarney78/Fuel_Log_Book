package au.barney.fuellogbook.data

import kotlinx.coroutines.flow.Flow

class FuelLogRepository(
    private val vehicleDao: VehicleDao,
    private val fuelTypeDao: FuelTypeDao,
    private val logDao: LogDao
) {
    val allVehicles: Flow<List<Vehicle>> = vehicleDao.getAllVehicles()
    val allFuelTypes: Flow<List<FuelType>> = fuelTypeDao.getAllFuelTypes()
    val allLogs: Flow<List<LogEntry>> = logDao.getAllLogs()

    suspend fun getAllLogsSync(): List<LogEntry> = logDao.getAllLogsSync()
    suspend fun getAllVehiclesSync(): List<Vehicle> = vehicleDao.getAllVehiclesSync()

    suspend fun insertVehicle(vehicle: Vehicle) = vehicleDao.insertVehicle(vehicle)
    suspend fun deleteVehicle(id: Int) = vehicleDao.deleteVehicleById(id)

    suspend fun insertFuelType(fuelType: FuelType) = fuelTypeDao.insertFuelType(fuelType)
    suspend fun deleteFuelType(id: Int) = fuelTypeDao.deleteFuelTypeById(id)

    suspend fun getLogCountForFuelType(fuelType: String): Int = logDao.getLogCountForFuelType(fuelType)
    suspend fun getLogCountForVehicle(vehicleId: Int): Int = logDao.getLogCountForVehicle(vehicleId)
    suspend fun getLatestLogForVehicle(vehicleId: Int): LogEntry? = logDao.getLatestLogForVehicle(vehicleId)

    suspend fun insertLog(logEntry: LogEntry) = logDao.insertLog(logEntry)
    suspend fun updateLog(logEntry: LogEntry) = logDao.updateLog(logEntry)
    suspend fun deleteLog(id: Int) = logDao.deleteLogById(id)
}
