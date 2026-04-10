package au.barney.fuellogbook.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.sqlite.db.SimpleSQLiteQuery
import au.barney.fuellogbook.data.AppDatabase
import au.barney.fuellogbook.data.FuelLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object BackupManager {

    /**
     * Silently backups the database to the Downloads folder.
     * Useful for automatic backups during app updates.
     * Copies files directly to avoid triggering Room migrations before the backup is done.
     */
    fun autoBackupToDownloads(context: Context) {
        try {
            val dbFile = context.getDatabasePath("fuel_log_database")
            if (!dbFile.exists()) return

            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            
            // Backup main DB
            val backupFile = File(downloadsDir, "fuel_log_auto_backup_$timestamp.db")
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Backup WAL/SHM if they exist to ensure data consistency
            if (walFile.exists()) {
                val backupWal = File(downloadsDir, "fuel_log_auto_backup_$timestamp.db-wal")
                FileInputStream(walFile).use { it.copyTo(FileOutputStream(backupWal)) }
            }
            if (shmFile.exists()) {
                val backupShm = File(downloadsDir, "fuel_log_auto_backup_$timestamp.db-shm")
                FileInputStream(shmFile).use { it.copyTo(FileOutputStream(backupShm)) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Backs up the database to the provided Uri.
     */
    fun backupToUri(context: Context, uri: Uri) {
        try {
            val db = AppDatabase.getDatabase(context)
            // Force Checkpoint
            db.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA checkpoint(FULL)"))

            val dbFile = context.getDatabasePath("fuel_log_database")
            context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Copies the current database file to a shareable file and opens the system chooser.
     */
    fun backupDatabase(context: Context) {
        try {
            val db = AppDatabase.getDatabase(context)
            // Force Checkpoint
            db.openHelper.writableDatabase.query(SimpleSQLiteQuery("PRAGMA checkpoint(FULL)"))

            val dbFile = context.getDatabasePath("fuel_log_database")
            val exportDir = File(context.filesDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val backupFile = File(exportDir, "fuel_log_backup.db")
            
            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            val uri = FileProvider.getUriForFile(context, "au.barney.fuellogbook.fileprovider", backupFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(intent, "Save Backup To...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Replaces the current app database with the file provided via the Uri.
     */
    fun restoreDatabase(context: Context, backupUri: Uri, onComplete: () -> Unit) {
        try {
            val dbFile = context.getDatabasePath("fuel_log_database")
            
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            Toast.makeText(context, "Restore successful! Please restart the app.", Toast.LENGTH_LONG).show()
            onComplete()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exports logs to CSV and shares it.
     */
    suspend fun exportToCsv(context: Context, repository: FuelLogRepository) {
        withContext(Dispatchers.IO) {
            try {
                val logs = repository.allLogs.first()
                val vehicles = repository.allVehicles.first()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                val csvHeader = "Rego,Vehicle,Date,Odo Start,Odo End,Distance,Fuel Type,Price/L,Litres,Total Cost,L/100km\n"
                val csvContent = StringBuilder(csvHeader)

                logs.forEach { log ->
                    val vehicle = vehicles.find { it.vehID == log.vehicleID }
                    csvContent.append("${vehicle?.rego ?: ""},")
                    csvContent.append("${vehicle?.vehicle ?: ""},")
                    csvContent.append("${dateFormat.format(Date(log.dateOfFill))},")
                    csvContent.append("${log.odometerStart},")
                    csvContent.append("${log.odometerEnd},")
                    csvContent.append("${log.distance},")
                    csvContent.append("${log.fuelType},")
                    csvContent.append("${log.costPerLitre},")
                    csvContent.append("${log.litres},")
                    csvContent.append("${log.cost},")
                    csvContent.append("${log.litersPer100}\n")
                }

                val exportDir = File(context.filesDir, "exports")
                if (!exportDir.exists()) exportDir.mkdirs()
                
                val exportFile = File(exportDir, "fuel_logs_${System.currentTimeMillis()}.csv")
                FileOutputStream(exportFile).use { it.write(csvContent.toString().toByteArray()) }

                val uri = FileProvider.getUriForFile(context, "au.barney.fuellogbook.fileprovider", exportFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                val chooser = Intent.createChooser(intent, "Share Fuel Logs")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
