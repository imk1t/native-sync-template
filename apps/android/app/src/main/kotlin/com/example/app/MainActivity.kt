package com.example.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import com.example.core.navigation.NavigationState
import com.example.core.navigation.Route
import com.example.core.navigation.fromUrl

class MainActivity : ComponentActivity() {
    private val app: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) open(intent)
        setContent {
            // システムバーの外観と同じ OS 設定で、コンテンツの配色も切り替える。
            val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            MaterialTheme(colorScheme = colorScheme) {
                AppRoot(app.navigator) { route -> AppRouter(route, app.dependencies, app.navigator) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        open(intent)
    }

    private fun open(intent: Intent?) {
        val route = intent?.data?.toString()?.let(Route::fromUrl) ?: return
        app.navigator.reset(NavigationState.opening(route, app.navigator.state.value.selectedTab))
    }
}
