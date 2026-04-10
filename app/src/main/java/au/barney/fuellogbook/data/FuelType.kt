package au.barney.fuellogbook.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_types")
data class FuelType(
    @PrimaryKey(autoGenerate = true)
    val fuelTypeID: Int = 0,
    val fuelType: String
)
