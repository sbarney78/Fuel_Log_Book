package au.barney.fuellogbook

import android.app.Application
import au.barney.fuellogbook.data.AppDatabase
import au.barney.fuellogbook.data.FuelLogRepository

class FuelLogApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { 
        FuelLogRepository(database.vehicleDao(), database.fuelTypeDao(), database.logDao()) 
    }
}
