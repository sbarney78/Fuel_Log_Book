package au.barney.fuellogbook.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM logs ORDER BY dateOfFill DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs ORDER BY dateOfFill DESC")
    suspend fun getAllLogsSync(): List<LogEntry>

    @Insert
    suspend fun insertLog(logEntry: LogEntry)

    @androidx.room.Update
    suspend fun updateLog(logEntry: LogEntry)

    @Query("DELETE FROM logs WHERE logID = :id")
    suspend fun deleteLogById(id: Int)

    @Query("SELECT COUNT(*) FROM logs WHERE fuelType = :fuelType")
    suspend fun getLogCountForFuelType(fuelType: String): Int

    @Query("SELECT COUNT(*) FROM logs WHERE vehicleID = :vehicleId")
    suspend fun getLogCountForVehicle(vehicleId: Int): Int

    @Query("SELECT * FROM logs WHERE vehicleID = :vehicleId ORDER BY dateOfFill DESC, odometerEnd DESC LIMIT 1")
    suspend fun getLatestLogForVehicle(vehicleId: Int): LogEntry?
}
