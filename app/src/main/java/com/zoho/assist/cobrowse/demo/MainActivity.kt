package com.zoho.assist.cobrowse.demo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.zoho.assist.cobrowse.demo.bank.BankApp
import com.zoho.assist.cobrowse.demo.bank.BankSettings
import com.zoho.assist.cobrowse.demo.bank.ThemeOption
import com.zoho.assist.cobrowse.demo.common.BankTheme

/**
 * The bank's single Activity, hosting [BankApp]. Its `decorView` is what
 * in-app capture records, so the SDK lifecycle hooks live here; everything
 * else about a session lives in [CobrowseController].
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Must run before the Activity is STARTED: the SDK registers its
        // permission launchers with registerForActivityResult.
        CobrowseController.attach(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        setContent {
            // Light by default: the redaction bars read better on a light screen.
            val dark = when (BankSettings.theme) {
                ThemeOption.LIGHT -> false
                ThemeOption.DARK -> true
                ThemeOption.SYSTEM -> isSystemInDarkTheme()
            }
            BankTheme(darkTheme = dark) {
                BankApp(modifier = Modifier.fillMaxSize())
            }
        }
    }

    // Forwarded by hand; missing them stops capture pausing in the background
    // and the socket recovering after unlock.
    override fun onResume() {
        super.onResume()
        CobrowseController.onResume()
    }

    override fun onStop() {
        super.onStop()
        CobrowseController.onStop()
    }
}
