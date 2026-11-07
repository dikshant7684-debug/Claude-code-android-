package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.MainAppNavigation
import com.example.ui.theme.ClaudeCodeTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val generalPrefs by viewModel.generalPreferences.collectAsStateWithLifecycle()
      val systemInDark = isSystemInDarkTheme()
      val isDark = when (generalPrefs.theme) {
        "DARK" -> true
        "LIGHT" -> false
        else -> systemInDark
      }
      ClaudeCodeTheme(darkTheme = isDark) {
        MainAppNavigation(viewModel = viewModel)
      }
    }
  }
}
