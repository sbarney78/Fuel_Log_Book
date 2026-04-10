package au.barney.fuellogbook.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "logs",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["vehID"],
            childColumns = ["vehicleID"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["vehicleID"])]
)
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val logID: Int = 0,
    val vehicleID: Int,
    val dateOfFill: Long,
    val odometerStart: Double,
    val odometerEnd: Double,
    val distance: Double,
    val fuelType: String,
    val costPerLitre: Double,
    val litres: Double,
    val cost: Double,
    val litersPer100: Double,
    val receiptPath: String? = null,
    val notes: String? = null
)
