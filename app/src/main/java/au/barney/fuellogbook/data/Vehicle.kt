package au.barney.fuellogbook.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val vehID: Int = 0,
    val rego: String,
    val vehicle: String
)
