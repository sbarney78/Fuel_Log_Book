package au.barney.fuellogbook.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelTypeDao {
    @Query("SELECT * FROM fuel_types")
    fun getAllFuelTypes(): Flow<List<FuelType>>

    @Insert
    suspend fun insertFuelType(fuelType: FuelType)

    @Query("DELETE FROM fuel_types WHERE fuelTypeID = :id")
    suspend fun deleteFuelTypeById(id: Int)
}
