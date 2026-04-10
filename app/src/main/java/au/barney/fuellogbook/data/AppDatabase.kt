package au.barney.fuellogbook.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Vehicle::class, FuelType::class, LogEntry::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelTypeDao(): FuelTypeDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE logs ADD COLUMN receiptPath TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE logs ADD COLUMN notes TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create the new table with Foreign Key constraint
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `logs_new` (
                        `logID` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `vehicleID` INTEGER NOT NULL, 
                        `dateOfFill` INTEGER NOT NULL, 
                        `odometerStart` REAL NOT NULL, 
                        `odometerEnd` REAL NOT NULL, 
                        `distance` REAL NOT NULL, 
                        `fuelType` TEXT NOT NULL, 
                        `costPerLitre` REAL NOT NULL, 
                        `litres` REAL NOT NULL, 
                        `cost` REAL NOT NULL, 
                        `litersPer100` REAL NOT NULL, 
                        `receiptPath` TEXT, 
                        `notes` TEXT, 
                        FOREIGN KEY(`vehicleID`) REFERENCES `vehicles`(`vehID`) ON UPDATE NO ACTION ON DELETE RESTRICT 
                    )
                """.trimIndent())

                // Copy data from the old table to the new one
                database.execSQL("""
                    INSERT INTO logs_new (logID, vehicleID, dateOfFill, odometerStart, odometerEnd, distance, fuelType, costPerLitre, litres, cost, litersPer100, receiptPath, notes)
                    SELECT logID, vehicleID, dateOfFill, odometerStart, odometerEnd, distance, fuelType, costPerLitre, litres, cost, litersPer100, receiptPath, notes FROM logs
                """.trimIndent())

                // Remove the old table
                database.execSQL("DROP TABLE logs")

                // Rename the new table to 'logs'
                database.execSQL("ALTER TABLE logs_new RENAME TO logs")

                // Create the index
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_logs_vehicleID` ON `logs` (`vehicleID`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fuel_log_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
