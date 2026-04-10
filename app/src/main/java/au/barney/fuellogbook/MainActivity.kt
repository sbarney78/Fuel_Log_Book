package au.barney.fuellogbook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.viewModels
import au.barney.fuellogbook.ui.FuelLogScreen
import au.barney.fuellogbook.ui.FuelLogViewModel
import au.barney.fuellogbook.ui.theme.FuelLogBookTheme
import au.barney.fuellogbook.util.BackupManager
import android.content.Context

class MainActivity : ComponentActivity() {
    private val viewModel: FuelLogViewModel by viewModels {
        FuelLogViewModel.Factory((application as FuelLogApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        handleAppUpdateBackup()
        
        enableEdgeToEdge()
        setContent {
            FuelLogBookTheme {
                FuelLogScreen(viewModel = viewModel)
            }
        }
    }

    private fun handleAppUpdateBackup() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentVersionCode = try {
            packageManager.getPackageInfo(packageName, 0).versionCode
        } catch (e: Exception) {
            0
        }
        val lastVersionCode = prefs.getInt("last_version_code", -1)

        if (lastVersionCode != -1 && currentVersionCode > lastVersionCode) {
            // App has been updated
            BackupManager.autoBackupToDownloads(this)
        }
        
        prefs.edit().putInt("last_version_code", currentVersionCode).apply()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FuelLogBookTheme {
        Greeting("Android")
    }
}