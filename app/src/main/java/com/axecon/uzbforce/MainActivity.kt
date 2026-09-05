package com.axecon.uzbforce

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.axecon.uzbforce.data.PreferencesManager
import com.axecon.uzbforce.ui.AirCursorMainApp
import com.axecon.uzbforce.ui.theme.MyApplicationTheme
import com.axecon.uzbforce.ui.viewmodel.AirCursorViewModel

class MainActivity : ComponentActivity() {

    private lateinit var preferencesManager: PreferencesManager
    private lateinit var viewModel: AirCursorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        applyHighRefreshRate()

        preferencesManager = PreferencesManager.getInstance(applicationContext)
        viewModel = AirCursorViewModel(preferencesManager)
        viewModel.checkPermissions(this)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            MyApplicationTheme(
                themeMode = uiState.config.themeMode,
                colorProfile = uiState.config.appColorProfile
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AirCursorMainApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyHighRefreshRate()
        if (::viewModel.isInitialized) {
            viewModel.checkPermissions(this)
        }
    }

    private fun applyHighRefreshRate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = window.decorView.display ?: display
                val modes = display?.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = maxMode.modeId
                    window.attributes = lp
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                val modes = display?.supportedModes
                val maxMode = modes?.maxByOrNull { it.refreshRate }
                if (maxMode != null) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = maxMode.modeId
                    window.attributes = lp
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

