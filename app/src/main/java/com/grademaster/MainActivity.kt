package com.grademaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grademaster.data.local.AppPreferences
import com.grademaster.ui.navigation.GradeMasterNavHost
import com.grademaster.ui.theme.GradeMasterTheme
import com.grademaster.util.ExportEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences
    @Inject lateinit var exportEngine: ExportEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Observe dark theme preference from DataStore
            val isDarkTheme by appPreferences.isDarkTheme
                .collectAsStateWithLifecycle(initialValue = false)

            // Inside MainActivity.kt -> GradeMasterTheme { ... }
GradeMasterTheme(darkTheme = isDarkTheme) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // false = White icons (for dark backgrounds)
            // true = Dark icons (for light backgrounds)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        GradeMasterNavHost(exportEngine = exportEngine)
    }
}
        }
    }
}
