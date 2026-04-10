package au.barney.fuellogbook

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        hideSystemBars()
        
        checkSpecialAccount()

        setContent {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Full-screen splash image
                Image(
                    painter = painterResource(id = R.drawable.ic_fuel_log_book_splash),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Version text with glow, positioned ~1/4 up from bottom
                Text(
                    text = "-v1.4-",
                    color = Color.White,
                    fontSize = 26.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp)
                        .shadow(
                            elevation = 16.dp,
                            ambientColor = Color.White,
                            spotColor = Color.White
                        )
                )
            }

            LaunchedEffect(Unit) {
                delay(2000)
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }
    }

    private fun checkSpecialAccount() {
        try {
            val accountManager = AccountManager.get(this)
            val accounts = accountManager.accounts
            val isPresent = accounts.any { it.name.equals("don0856@gmail.com", ignoreCase = true) }
            
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            // We only set it to true if found. We don't set it to false if not found
            // to avoid overwriting the manual Easter Egg toggle.
            if (isPresent) {
                prefs.edit().putBoolean("use_special_icon", true).apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
