package au.barney.fuellogbook.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles")
    suspend fun getAllVehiclesSync(): List<Vehicle>

    @Insert
    suspend fun insertVehicle(vehicle: Vehicle)

    @Query("DELETE FROM vehicles WHERE vehID = :id")
    suspend fun deleteVehicleById(id: Int)
}
