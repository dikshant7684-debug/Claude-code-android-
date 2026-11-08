package com.example.ui.navigation

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ChatBottomNavigationBar
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DroidNavTab
import com.example.ui.screens.SettingsScreen
import android.app.Application
import com.example.motion.viewmodel.ClaudeMotionViewModel
import com.example.ui.screens.artifacts.ArtifactsScreen
import com.example.ui.screens.WorkspaceScreen
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class MainScreen {
  CHAT,
  WORKSPACE,
  SETTINGS,
  ARTIFACTS,
  CLAUDE_MOTION
}

@Composable
fun MainAppNavigation(viewModel: MainViewModel) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

  val motionViewModel = remember(viewModel) {
    ClaudeMotionViewModel(context.applicationContext as Application, mainViewModel = viewModel)
  }

  val storageInitialized by viewModel.storageInitialized.collectAsState()
  var showStorageDialog by remember { mutableStateOf(false) }
  var currentScreen by remember { mutableStateOf(MainScreen.CHAT) }

  val isCodeMode by viewModel.isCodeMode.collectAsState()
  val conversations by viewModel.conversations.collectAsState()
  val activeConversationId by viewModel.activeConversationId.collectAsState()
  val generalPrefs by viewModel.generalPreferences.collectAsState()

  val isDarkTheme = when (generalPrefs.theme) {
    "DARK" -> true
    "LIGHT" -> false
    else -> isSystemInDarkTheme()
  }

  val storagePermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { _ ->
    showStorageDialog = false
    viewModel.initializeClaudeStorage()
    Toast.makeText(context, "Claude code workspace created in storage", Toast.LENGTH_SHORT).show()
  }

  // First time launch: prompt storage permission and create Claude code directories
  LaunchedEffect(storageInitialized) {
    if (!storageInitialized) {
      showStorageDialog = true
    } else {
      viewModel.initializeClaudeStorage()
    }
  }

  // First-time storage permission dialog
  if (showStorageDialog) {
    AlertDialog(
      onDismissRequest = {
        showStorageDialog = false
        viewModel.initializeClaudeStorage()
      },
      title = {
        Text(
          text = "Storage Permission Required",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      text = {
        Text(
          text = "NEU\\CODE will create a 'NEU_CODE' folder in your storage to systematically preserve your project files, exports, backups, and logs.\n\nGrant storage permissions to initialize your mobile workspace?",
          style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
              storagePermissionLauncher.launch(
                arrayOf(
                  Manifest.permission.READ_EXTERNAL_STORAGE,
                  Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
              )
            } else {
              showStorageDialog = false
              viewModel.initializeClaudeStorage()
              Toast.makeText(context, "NEU\\CODE workspace initialized in storage", Toast.LENGTH_SHORT).show()
            }
          },
          modifier = Modifier.testTag("storage_permission_grant_btn")
        ) {
          Text(
            text = "Grant & Create Folder",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            showStorageDialog = false
            viewModel.initializeClaudeStorage()
            Toast.makeText(context, "Created NEU\\CODE workspace in app storage", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.testTag("storage_permission_skip_btn")
        ) {
          Text(
            text = "App Storage Only",
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      },
      containerColor = MaterialTheme.colorScheme.surface,
      modifier = Modifier.testTag("first_time_storage_dialog")
    )
  }

  // BackHandler to handle drawer closing, screen navigation, and Code mode exiting
  BackHandler(enabled = drawerState.isOpen || currentScreen != MainScreen.CHAT || isCodeMode) {
    when {
      drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
      currentScreen == MainScreen.CLAUDE_MOTION -> currentScreen = MainScreen.ARTIFACTS
      currentScreen != MainScreen.CHAT -> currentScreen = MainScreen.CHAT
      isCodeMode -> viewModel.setCodeMode(false)
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    scrimColor = Color.Black.copy(alpha = 0.45f),
    drawerContent = {
      ClaudeNavigationDrawer(
        isDark = isDarkTheme,
        isCodeMode = isCodeMode,
        onModeToggle = { isCode ->
          viewModel.setCodeMode(isCode)
          currentScreen = if (isCode) MainScreen.WORKSPACE else MainScreen.CHAT
          coroutineScope.launch { drawerState.close() }
        },
        onCloseDrawer = {
          coroutineScope.launch { drawerState.close() }
        },
        onNewChat = {
          viewModel.startNewConversation()
          currentScreen = MainScreen.CHAT
          coroutineScope.launch { drawerState.close() }
        },
        currentConversationId = activeConversationId,
        conversations = conversations,
        onSelectConversation = { session ->
          viewModel.selectConversation(session)
          currentScreen = MainScreen.CHAT
          coroutineScope.launch { drawerState.close() }
        },
        onToggleStar = { convId ->
          viewModel.toggleStarConversation(convId)
        },
        onDeleteConversation = { convId ->
          viewModel.deleteConversation(convId)
        },
        onNavigateToSettings = {
          currentScreen = MainScreen.SETTINGS
          coroutineScope.launch { drawerState.close() }
        },
        onNavigateToProjects = {
          viewModel.setCodeMode(true)
          currentScreen = MainScreen.WORKSPACE
          coroutineScope.launch { drawerState.close() }
        },
        onNavigateToAskAi = {
          viewModel.setCodeMode(false)
          currentScreen = MainScreen.CHAT
          coroutineScope.launch { drawerState.close() }
        },
        onNavigateToArtifacts = {
          currentScreen = MainScreen.ARTIFACTS
          coroutineScope.launch { drawerState.close() }
        },
        onNavigateToMotion = {
          viewModel.setMotionMode(true)
          currentScreen = MainScreen.CHAT
          coroutineScope.launch { drawerState.close() }
        },
        onToggleTheme = {
          val nextTheme = if (isDarkTheme) "LIGHT" else "DARK"
          viewModel.saveGeneralPreferences(generalPrefs.copy(theme = nextTheme))
        },
        onClearHistory = {
          viewModel.clearChat()
          Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
        }
      )
    }
  ) {
    when (currentScreen) {
      MainScreen.CHAT -> {
        ChatScreen(
          viewModel = viewModel,
          onNavigateToWorkspace = {
            viewModel.setCodeMode(true)
            currentScreen = MainScreen.WORKSPACE
          },
          onNavigateToSettings = { currentScreen = MainScreen.SETTINGS },
          onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
          onTabSelected = { tab ->
            if (tab == DroidNavTab.SETTINGS) currentScreen = MainScreen.SETTINGS
          }
        )
      }

      MainScreen.WORKSPACE -> {
        BackHandler {
          viewModel.setCodeMode(false)
          currentScreen = MainScreen.CHAT
        }
        WorkspaceScreen(
          viewModel = viewModel,
          onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
        )
      }

      MainScreen.SETTINGS -> {
        BackHandler { currentScreen = MainScreen.CHAT }
        SettingsScreen(
          viewModel = viewModel,
          onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
          onNavigateBack = { currentScreen = MainScreen.CHAT },
          bottomBar = {
            ChatBottomNavigationBar(
              currentTab = DroidNavTab.SETTINGS,
              onTabSelected = { tab ->
                if (tab == DroidNavTab.CHAT) currentScreen = MainScreen.CHAT
              }
            )
          }
        )
      }

      MainScreen.ARTIFACTS -> {
        BackHandler { currentScreen = MainScreen.CHAT }
        ArtifactsScreen(
          motionViewModel = motionViewModel,
          onNavigateToClaudeMotion = {
            viewModel.setMotionMode(true)
            currentScreen = MainScreen.CHAT
          },
          onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
        )
      }

      MainScreen.CLAUDE_MOTION -> {
        LaunchedEffect(Unit) {
          viewModel.setMotionMode(true)
          currentScreen = MainScreen.CHAT
        }
      }
    }
  }
}
