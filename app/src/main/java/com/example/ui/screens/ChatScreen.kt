package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.model.ProjectFile
import com.example.ui.components.ClaudeThinkingOrb
import com.example.ui.components.ClaudeThinkingIndicatorRow
import com.example.ui.components.ModelSelectionBottomSheet
import com.example.ui.components.InlineSquareMotionVideoCard
import com.example.motion.model.ExportFormat
import com.example.motion.model.MotionProject
import com.example.ui.theme.ClaudeTerracotta
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import com.example.ui.viewmodel.AttachedFile
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.ripple
import androidx.compose.runtime.DisposableEffect
import kotlinx.coroutines.delay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MessageRole
import com.example.data.model.UiChatMessage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.formatting.SmartMessageBody
import com.example.ui.theme.DarkUserBubble
import com.example.ui.theme.DarkUserBubbleBorder
import com.example.ui.theme.DroidCodeTheme
import com.example.ui.theme.DroidOrange
import com.example.ui.theme.DroidOrangeDisabled
import com.example.ui.theme.LightOnBackground
import com.example.ui.theme.LightUserBubble
import com.example.ui.theme.LightUserBubbleBorder
import com.example.ui.viewmodel.ChatUiState
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.MainViewModel

enum class DroidNavTab(val label: String, val icon: ImageVector, val tag: String) {
  CHAT("Chat", Icons.Default.Home, "nav_tab_chat"),
  SETTINGS("Settings", Icons.Default.Settings, "nav_tab_settings")
}

data class SuggestionItem(
  val title: String,
  val prompt: String,
  val icon: ImageVector
)

/**
 * Stateful entry point integrating with MainViewModel and ChatViewModel
 */
@Composable
fun ChatScreen(
  viewModel: MainViewModel,
  onNavigateToWorkspace: () -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  onOpenDrawer: () -> Unit = {},
  onTabSelected: (DroidNavTab) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val chatViewModel = remember(viewModel) { ChatViewModel(mainViewModel = viewModel) }
  val uiState by chatViewModel.uiState.collectAsState()
  val context = LocalContext.current

  ChatScreenContent(
    uiState = uiState,
    onInputChanged = chatViewModel::onInputTextChanged,
    onSend = chatViewModel::onSendMessage,
    onRegenerate = chatViewModel::onRegeneratePrompt,
    onModelSelected = chatViewModel::onModelSelected,
    onReasoningEffortSelected = chatViewModel::onReasoningEffortSelected,
    onAttachFile = chatViewModel::onAttachFile,
    onRemoveAttachedFile = chatViewModel::onRemoveAttachedFile,
    getWorkspaceFiles = { viewModel.getWorkspaceProjectFiles() },
    readWorkspaceFile = { path -> viewModel.readProjectFile(path) },
    onSuggestionClick = chatViewModel::onSuggestionClick,
    onCancelTask = chatViewModel::onCancelTask,
    onOpenFile = { relativePath ->
      viewModel.openFileByRelativePath(relativePath)
      onNavigateToWorkspace()
    },
    onNavigateToSettings = onNavigateToSettings,
    onOpenDrawer = onOpenDrawer,
    currentTab = DroidNavTab.CHAT,
    onTabSelected = { tab ->
      when (tab) {
        DroidNavTab.CHAT -> { /* already here */ }
        DroidNavTab.SETTINGS -> onNavigateToSettings()
      }
      onTabSelected(tab)
    },
    onToggleMotionMode = chatViewModel::setMotionMode,
    onSaveMotion = { project ->
      chatViewModel.saveMotionVideo(project) { path ->
        Toast.makeText(context, "Saved animation project to $path", Toast.LENGTH_SHORT).show()
      }
    },
    onExportMotion = { project, fmt ->
      chatViewModel.exportMotionVideo(project, fmt) { status ->
        Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
      }
    },
    onEditWithAi = { editPrompt ->
      chatViewModel.onInputTextChanged(editPrompt)
    },
    modifier = modifier
  )
}

/**
 * Stateless, Previewable ChatScreen content adhering strictly to M3 guidelines
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreenContent(
  uiState: ChatUiState,
  onInputChanged: (String) -> Unit,
  onSend: () -> Unit,
  onModelSelected: (String) -> Unit,
  onSuggestionClick: (String) -> Unit,
  onCancelTask: () -> Unit,
  onOpenFile: (String) -> Unit,
  onNavigateToSettings: () -> Unit,
  onOpenDrawer: () -> Unit = {},
  onRegenerate: (String) -> Unit = {},
  onReasoningEffortSelected: (String) -> Unit = {},
  onAttachFile: (AttachedFile) -> Unit = {},
  onRemoveAttachedFile: (AttachedFile) -> Unit = {},
  getWorkspaceFiles: suspend () -> List<ProjectFile> = { emptyList() },
  readWorkspaceFile: suspend (String) -> String = { "" },
  currentTab: DroidNavTab = DroidNavTab.CHAT,
  onTabSelected: (DroidNavTab) -> Unit = {},
  onToggleMotionMode: (Boolean) -> Unit = {},
  onSaveMotion: (MotionProject) -> Unit = {},
  onExportMotion: (MotionProject, ExportFormat) -> Unit = { _, _ -> },
  onEditWithAi: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current
  val density = LocalDensity.current
  val isKeyboardOpen = WindowInsets.isImeVisible || WindowInsets.ime.getBottom(density) > 0
  val coroutineScope = rememberCoroutineScope()
  val context = LocalContext.current

  var showModelSheet by remember { mutableStateOf(false) }
  var showAttachSheet by remember { mutableStateOf(false) }
  var workspaceFiles by remember { mutableStateOf<List<ProjectFile>>(emptyList()) }

  val openDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        var displayName = "attached_file"
        var fileSize: Long = 0
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
          if (cursor.moveToFirst()) {
            if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
          }
        }
        val textContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
        onAttachFile(
          AttachedFile(
            name = displayName,
            path = uri.toString(),
            content = textContent,
            sizeBytes = if (fileSize > 0) fileSize else textContent.toByteArray().size.toLong()
          )
        )
        Toast.makeText(context, "Attached $displayName", Toast.LENGTH_SHORT).show()
      } catch (e: Exception) {
        Toast.makeText(context, "Could not read file: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentColor = MaterialTheme.colorScheme.onBackground,
    contentWindowInsets = WindowInsets.statusBars,
    topBar = {
      ChatTopAppBar(
        selectedModel = uiState.selectedModel,
        onOpenModelSheet = { showModelSheet = true },
        onNavigateToSettings = onNavigateToSettings,
        onOpenDrawer = onOpenDrawer
      )
    }
  ) { paddingValues ->
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = paddingValues.calculateTopPadding())
        .imePadding(),
      contentAlignment = Alignment.TopCenter
    ) {
      // Responsive width capping on wide screens/tablets (cap at 720dp)
      val contentWidth = if (maxWidth >= 600.dp) 720.dp else maxWidth

      Column(
        modifier = Modifier
          .widthIn(max = contentWidth)
          .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Content Area: Empty State OR Message History
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          contentAlignment = Alignment.Center
        ) {
          val hasVisibleContent = remember(uiState.messages) {
            uiState.messages.any { it.content.isNotBlank() || it.role == MessageRole.USER || it.motionProject != null || it.isMotionGenerating }
          }
          if (!hasVisibleContent && !uiState.isLoading) {
            EmptyChatState(
              isMotionMode = uiState.isMotionMode,
              onSuggestionClick = { prompt ->
                onSuggestionClick(prompt)
                focusRequester.requestFocus()
                keyboardController?.show()
              }
            )
          } else {
            val currentStatus = uiState.currentTask?.currentActivity?.takeIf { it.isNotBlank() }
              ?: uiState.currentTask?.currentPlan?.takeIf { it.isNotBlank() }
              ?: "Generating response..."
            MessagesList(
              messages = uiState.messages,
              isLoading = uiState.isLoading,
              statusText = currentStatus,
              modelName = uiState.selectedModel,
              onOpenFile = onOpenFile,
              onCancelTask = onCancelTask,
              onRegenerate = onRegenerate,
              onSaveMotion = onSaveMotion,
              onExportMotion = onExportMotion,
              onEditWithAi = onEditWithAi
            )
          }
        }

        // Claude Motion Mode Indicator (Phase 1)
        if (uiState.isMotionMode) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = ClaudeTerracotta.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, ClaudeTerracotta.copy(alpha = 0.35f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp)
              .testTag("claude_motion_mode_badge")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.MovieFilter,
                  contentDescription = null,
                  tint = ClaudeTerracotta,
                  modifier = Modifier.size(18.dp)
                )
                Column {
                  Text(
                    text = "Claude Motion Mode Active",
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = FontWeight.Bold,
                      fontSize = 12.sp
                    ),
                    color = ClaudeTerracotta
                  )
                  Text(
                    text = "AI motion graphics & 1:1 video generation",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
              IconButton(
                onClick = { onToggleMotionMode(false) },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Exit Motion Mode",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }

        val activeFiles = remember { com.example.ui.system.WorkingFileManager.getInstance().activeFiles }
        if (activeFiles.isNotEmpty()) {
          com.example.ui.components.WorkingFileIndicatorBar(
            files = activeFiles,
            onFileClick = { file ->
              onOpenFile(file.relativePath)
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
          )
        }

        if (uiState.inputText.startsWith("/")) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 4.dp)
          ) {
            com.example.ui.components.CommandPaletteMenu(
              filterQuery = uiState.inputText,
              onSelectCommand = { cmd ->
                when (cmd) {
                  "/clear" -> {
                    onSuggestionClick("/clear")
                    onInputChanged("")
                  }
                  "/model", "/models" -> {
                    showModelSheet = true
                    onInputChanged("")
                  }
                  "/settings" -> {
                    onNavigateToSettings()
                    onInputChanged("")
                  }
                  else -> {
                    onInputChanged(cmd + " ")
                  }
                }
              },
              onDismiss = {
                onInputChanged("")
              }
            )
          }
        }

        // Pinned Composer directly above keyboard
        ChatComposerCard(
          inputText = uiState.inputText,
          onInputChanged = onInputChanged,
          onSend = onSend,
          selectedModel = uiState.selectedModel,
          onOpenModelSheet = { showModelSheet = true },
          attachedFiles = uiState.attachedFiles,
          onRemoveAttachedFile = onRemoveAttachedFile,
          onAttachClick = {
            coroutineScope.launch {
              workspaceFiles = getWorkspaceFiles()
              showAttachSheet = true
            }
          },
          isLoading = uiState.isLoading,
          onCancelTask = onCancelTask,
          focusRequester = focusRequester,
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp)
        )

        // Bottom navigation bar removed from CHAT screen per user directive
      }
    }

    if (showModelSheet) {
      ModelSelectionBottomSheet(
        selectedModel = uiState.selectedModel,
        availableModels = uiState.availableModels,
        onModelSelected = { model ->
          onModelSelected(model)
          showModelSheet = false
        },
        currentEffort = uiState.reasoningEffort,
        onEffortSelected = onReasoningEffortSelected,
        onNavigateToSettings = {
          showModelSheet = false
          onNavigateToSettings()
        },
        onDismiss = { showModelSheet = false }
      )
    }

    if (showAttachSheet) {
      AttachFileBottomSheet(
        workspaceFiles = workspaceFiles,
        onAttachDeviceFile = {
          showAttachSheet = false
          openDocumentLauncher.launch(arrayOf("*/*"))
        },
        onAttachWorkspaceFile = { file ->
          showAttachSheet = false
          coroutineScope.launch {
            val content = readWorkspaceFile(file.relativePath)
            onAttachFile(
              AttachedFile(
                name = file.name,
                path = file.relativePath,
                content = content,
                sizeBytes = if (file.sizeBytes > 0) file.sizeBytes else content.toByteArray().size.toLong()
              )
            )
            Toast.makeText(context, "Attached ${file.name}", Toast.LENGTH_SHORT).show()
          }
        },
        onDismiss = { showAttachSheet = false }
      )
    }
  }
}

/**
 * Top App Bar with Logo, Title (20sp semibold), readable Model Badge (13sp), and 48dp touch target settings button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopAppBar(
  selectedModel: String,
  onOpenModelSheet: () -> Unit = {},
  onNavigateToSettings: () -> Unit,
  onOpenDrawer: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  TopAppBar(
    navigationIcon = {
      IconButton(
        onClick = onOpenDrawer,
        modifier = Modifier
          .size(48.dp)
          .testTag("chat_open_drawer_btn")
      ) {
        Icon(
          imageVector = Icons.Default.Menu,
          contentDescription = "Open Drawer",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(24.dp)
        )
      }
    },
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Logo Brand Icon
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(com.example.ui.theme.NeuCodeSurfacePrimary)
            .border(1.dp, com.example.ui.theme.NeuCodeBorderActive, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.cat_logo),
            contentDescription = "NEU\\CODE Logo",
            modifier = Modifier.size(24.dp).clip(CircleShape)
          )
        }

        // Title with NEU\CODE and DEV ENVIRONMENT pill badge
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = "NEU\\CODE",
            style = MaterialTheme.typography.titleLarge.copy(
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = com.example.ui.theme.NeuCodePurpleContainer,
            border = BorderStroke(1.dp, com.example.ui.theme.NeuCodeBorderActive)
          ) {
            Text(
              text = "DEV CORE",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
              ),
              color = com.example.ui.theme.NeuCodeElectricViolet,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
      }
    },
    actions = {
      // Setting icon removed from CHAT screen per user directive
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = MaterialTheme.colorScheme.background,
      titleContentColor = MaterialTheme.colorScheme.onBackground
    ),
    modifier = modifier.testTag("chat_top_bar")
  )
}

/**
 * Empty State: Vertically centered greeting (34-40sp serif), subtitle (17-18sp muted), and suggestion chips
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyChatState(
  onSuggestionClick: (String) -> Unit,
  isMotionMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val suggestions = if (isMotionMode) {
    listOf(
      SuggestionItem("Cinematic AI Promo", "Create a cinematic AI promotional video with smooth lighting transitions", Icons.Default.MovieFilter),
      SuggestionItem("20s Explainer Video", "Make a 20-second animation explaining artificial intelligence concepts", Icons.Default.Animation),
      SuggestionItem("Animate Data Chart", "Animate this revenue bar chart with professional transitions", Icons.Default.AutoAwesome),
      SuggestionItem("Kinetic Typography", "Create a vertical kinetic text animation for social media", Icons.Default.PlayArrow)
    )
  } else {
    listOf(
      SuggestionItem("Scan Architecture", "Analyze the project codebase architecture and identify vulnerabilities", Icons.Default.Code),
      SuggestionItem("Neutralize Errors", "Inspect project source files and eliminate all syntax and compiler errors", Icons.Default.BugReport),
      SuggestionItem("Build Android UI", "Construct an advanced, high-performance Jetpack Compose screen", Icons.Default.Android),
      SuggestionItem("System Diagnostics", "Execute test suites and verify system state stability", Icons.Default.CheckCircle)
    )
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    // NEU\CODE Brand emblem
    Image(
      painter = painterResource(id = R.drawable.cat_logo),
      contentDescription = "NEU\\CODE Emblem",
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .border(1.5.dp, com.example.ui.theme.NeuCodeBorderActive, CircleShape)
        .testTag("app_logo_image")
    )

    Spacer(modifier = Modifier.height(16.dp))

    // NEU\CODE Heading
    Text(
      text = if (isMotionMode) "Motion Studio" else "NEU\\CODE",
      style = MaterialTheme.typography.displayLarge.copy(
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = 2.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold
      ),
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag("chat_greeting_text")
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Subtitle
    Text(
      text = if (isMotionMode) {
        "AI motion graphics and cinematic rendering engine."
      } else {
        "Autonomous developer environment. What shall we construct?"
      },
      style = MaterialTheme.typography.titleMedium.copy(
        fontSize = 15.sp,
        lineHeight = 22.sp
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag("chat_subtitle_text")
    )

    Spacer(modifier = Modifier.height(28.dp))

    // Suggestion chips in FlowRow with 10dp spacing
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Center,
      verticalArrangement = Arrangement.spacedBy(10.dp),
      maxItemsInEachRow = 2
    ) {
      suggestions.forEach { item ->
        SuggestionChipItem(
          label = item.title,
          icon = item.icon,
          onClick = { onSuggestionClick(item.prompt) },
          modifier = Modifier.padding(horizontal = 4.dp)
        )
      }
    }
  }
}

/**
 * Suggestion chip: 48dp minimum height, 16dp horizontal padding, 20dp icons, 15sp text
 */
@Composable
fun SuggestionChipItem(
  label: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 1.dp,
    modifier = modifier
      .defaultMinSize(minHeight = 48.dp)
      .semantics { contentDescription = "Suggestion: $label" }
      .testTag("suggestion_chip_${label.lowercase().replace(" ", "_")}")
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp)
      )
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium.copy(
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium
        ),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

/**
 * Composer: Rounded card (28dp corners, 64dp min height, subtle border),
 * multi-line text field (16sp, up to 6 lines),
 * bottom row with 48dp attach button, model dropdown pill, and 48dp orange circular send button.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatComposerCard(
  inputText: String,
  onInputChanged: (String) -> Unit,
  onSend: () -> Unit,
  selectedModel: String,
  onOpenModelSheet: () -> Unit,
  attachedFiles: List<AttachedFile> = emptyList(),
  onRemoveAttachedFile: (AttachedFile) -> Unit = {},
  onAttachClick: () -> Unit = {},
  isLoading: Boolean,
  onCancelTask: () -> Unit = {},
  focusRequester: FocusRequester,
  modifier: Modifier = Modifier
) {
  val isSendEnabled = inputText.trim().isNotEmpty() || attachedFiles.isNotEmpty()

  Surface(
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    tonalElevation = 2.dp,
    modifier = modifier
      .defaultMinSize(minHeight = 64.dp)
      .animateContentSize(tween(180))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      // Attached files row if any files are attached
      if (attachedFiles.isNotEmpty()) {
        FlowRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          attachedFiles.forEach { file ->
            AttachedFileChip(
              file = file,
              onRemove = { onRemoveAttachedFile(file) }
            )
          }
        }
      }

      // Multi-line OutlinedTextField (16sp, growing up to 6 lines, then scrolling)
      OutlinedTextField(
        value = inputText,
        onValueChange = onInputChanged,
        placeholder = {
          Text(
            text = "State your directive...",
            style = MaterialTheme.typography.bodyLarge.copy(
              fontSize = 16.sp,
              lineHeight = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
          )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 16.sp,
          lineHeight = 24.sp,
          color = MaterialTheme.colorScheme.onSurface
        ),
        maxLines = 6,
        keyboardOptions = KeyboardOptions(
          capitalization = KeyboardCapitalization.Sentences,
          keyboardType = KeyboardType.Text,
          imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
          onSend = {
            if (isSendEnabled) onSend()
          }
        ),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          disabledContainerColor = Color.Transparent,
          focusedBorderColor = Color.Transparent,
          unfocusedBorderColor = Color.Transparent
        ),
        modifier = Modifier
          .fillMaxWidth()
          .focusRequester(focusRequester)
          .testTag("chat_composer_input")
      )

      // Bottom Row: "+" Attach (48dp), Model Selector Pill, Circular Send (48dp)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // "+" Attach IconButton (48dp touch target)
          IconButton(
            onClick = onAttachClick,
            modifier = Modifier
              .size(48.dp)
              .semantics { contentDescription = "Attach file or workspace context" }
              .testTag("composer_attach_button")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "Attach",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(24.dp)
            )
          }

          // Model Selector Pill with Bottom Sheet Launcher
          Surface(
            onClick = onOpenModelSheet,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
              .defaultMinSize(minHeight = 40.dp)
              .semantics { contentDescription = "Select AI model: currently $selectedModel" }
              .testTag("composer_model_selector")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = selectedModel,
                style = MaterialTheme.typography.labelMedium.copy(
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        // Circular Orange Send Button (48dp touch target)
        val buttonColor by animateColorAsState(
          targetValue = when {
            isLoading -> MaterialTheme.colorScheme.error
            isSendEnabled -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outlineVariant
          },
          animationSpec = tween(160),
          label = "send_button_color"
        )

        IconButton(
          onClick = {
            if (isLoading) {
              onCancelTask()
            } else if (isSendEnabled) {
              onSend()
            }
          },
          enabled = isSendEnabled || isLoading,
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(buttonColor)
            .semantics {
              contentDescription = if (isLoading) "Stop generation" else "Send message"
            }
            .testTag("composer_send_button")
        ) {
          if (isLoading) {
            Icon(
              imageVector = Icons.Default.Stop,
              contentDescription = "Stop",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          } else {
            Icon(
              imageVector = Icons.Default.ArrowUpward,
              contentDescription = "Send",
              tint = if (isSendEnabled) Color.White else DroidOrangeDisabled,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }
  }
}

/**
 * Chip representing an attached file with quick remove button.
 */
@Composable
fun AttachedFileChip(
  file: AttachedFile,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.height(32.dp)
  ) {
    Row(
      modifier = Modifier.padding(start = 8.dp, end = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Description,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
      )
      Text(
        text = file.name,
        style = MaterialTheme.typography.labelMedium.copy(
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        ),
        maxLines = 1,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      IconButton(
        onClick = onRemove,
        modifier = Modifier
          .size(24.dp)
          .testTag("remove_attached_file_${file.name}")
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Remove attachment",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(14.dp)
        )
      }
    }
  }
}

/**
 * Bottom Sheet for attaching files either from device storage or the active workspace.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachFileBottomSheet(
  workspaceFiles: List<ProjectFile>,
  onAttachDeviceFile: () -> Unit,
  onAttachWorkspaceFile: (ProjectFile) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    scrimColor = Color.Black.copy(alpha = 0.45f),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 10.dp, bottom = 4.dp)
          .size(width = 36.dp, height = 4.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.outlineVariant)
      )
    },
    modifier = Modifier.testTag("attach_file_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      // Header: Close icon and Centered Title
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        contentAlignment = Alignment.Center
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .align(Alignment.CenterStart)
            .size(36.dp)
            .testTag("close_attach_sheet_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }

        Text(
          text = "Attach Context",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )
      }

      // Option 1: Pick from Device
      Surface(
        onClick = onAttachDeviceFile,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("attach_device_file_option")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.FolderOpen,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Browse Device Files",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Attach any document, code, or text file from device storage",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Section: Workspace Files
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Workspace Files",
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )

        Text(
          text = "${workspaceFiles.size} available",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      if (workspaceFiles.isEmpty()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          Text(
            text = "No workspace files found. Tap 'Browse Device Files' above to attach external files.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 260.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(workspaceFiles) { file ->
            Surface(
              onClick = { onAttachWorkspaceFile(file) },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("workspace_file_${file.name}")
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Description,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = file.relativePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                  )
                }

                Surface(
                  shape = RoundedCornerShape(6.dp),
                  color = MaterialTheme.colorScheme.surface,
                  border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                  Text(
                    text = if (file.sizeBytes > 1024) "${file.sizeBytes / 1024} KB" else "${file.sizeBytes} B",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

/**
 * Message history list with auto-scroll to latest message
 */
/**
 * Text-to-Speech manager state holder for assistant responses
 */
class TtsState(
  val speakingMessageId: String?,
  val speak: (String, String) -> Unit,
  val stop: () -> Unit
)

@Composable
fun rememberTtsState(): TtsState {
  val context = LocalContext.current
  var tts by remember { mutableStateOf<TextToSpeech?>(null) }
  var speakingMessageId by remember { mutableStateOf<String?>(null) }
  var isReady by remember { mutableStateOf(false) }

  DisposableEffect(context) {
    val speech = TextToSpeech(context) { status ->
      if (status == TextToSpeech.SUCCESS) {
        isReady = true
      }
    }
    tts = speech
    onDispose {
      speech.stop()
      speech.shutdown()
    }
  }

  return remember(tts, speakingMessageId, isReady) {
    TtsState(
      speakingMessageId = speakingMessageId,
      speak = { id, text ->
        if (speakingMessageId == id) {
          tts?.stop()
          speakingMessageId = null
        } else {
          tts?.stop()
          val cleanText = text
            .replace("```[a-zA-Z]*".toRegex(), "")
            .replace("```", "")
            .replace("#", "")
            .replace("*", "")
            .trim()
          speakingMessageId = id
          tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
              speakingMessageId = null
            }
            override fun onError(utteranceId: String?) {
              speakingMessageId = null
            }
          })
          tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, id)
        }
      },
      stop = {
        tts?.stop()
        speakingMessageId = null
      }
    )
  }
}

enum class ThumbState {
  NONE, UP, DOWN
}

/**
 * Subtle action icon button with hover / press animation, outline style, and dark/light support
 */
@Composable
fun ToolbarActionButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  tint: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
  tag: String = ""
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isHovered by interactionSource.collectIsHoveredAsState()
  val isPressed by interactionSource.collectIsPressedAsState()

  val backgroundColor by animateColorAsState(
    targetValue = when {
      isPressed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
      isHovered -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
      else -> Color.Transparent
    },
    label = "toolbar_btn_bg"
  )

  Box(
    modifier = modifier
      .size(34.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(backgroundColor)
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = true, radius = 17.dp),
        onClick = onClick
      )
      .semantics { this.contentDescription = contentDescription }
      .testTag(tag),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(17.dp)
    )
  }
}

/**
 * Action toolbar directly below AI-generated message bubbles, matching Claude mobile layout:
 * Copy, Play (TTS), Share, Thumbs Up, Thumbs Down, Regenerate.
 */
@Composable
fun AssistantActionToolbar(
  message: UiChatMessage,
  isSpeaking: Boolean,
  onPlayClick: () -> Unit,
  onRegenerateClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var hasCopied by remember { mutableStateOf(false) }
  var thumbState by remember { mutableStateOf(ThumbState.NONE) }

  LaunchedEffect(hasCopied) {
    if (hasCopied) {
      delay(2000)
      hasCopied = false
    }
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. Copy
    ToolbarActionButton(
      icon = if (hasCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
      contentDescription = if (hasCopied) "Copied!" else "Copy message",
      tint = if (hasCopied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
      tag = "ai_msg_action_copy",
      onClick = {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Claude Code Message", message.content))
        hasCopied = true
        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
      }
    )

    // 2. Play (Text-to-Speech)
    ToolbarActionButton(
      icon = if (isSpeaking) Icons.Outlined.Stop else Icons.Outlined.VolumeUp,
      contentDescription = if (isSpeaking) "Stop speech" else "Read aloud",
      tint = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
      tag = "ai_msg_action_tts",
      onClick = onPlayClick
    )

    // 3. Share
    ToolbarActionButton(
      icon = Icons.Outlined.Share,
      contentDescription = "Share response",
      tag = "ai_msg_action_share",
      onClick = {
        val sendIntent = Intent().apply {
          action = Intent.ACTION_SEND
          putExtra(Intent.EXTRA_TEXT, message.content)
          type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share response")
        context.startActivity(shareIntent)
      }
    )

    // 4. Thumbs Up
    ToolbarActionButton(
      icon = if (thumbState == ThumbState.UP) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
      contentDescription = "Thumbs up",
      tint = if (thumbState == ThumbState.UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
      tag = "ai_msg_action_thumbs_up",
      onClick = {
        thumbState = if (thumbState == ThumbState.UP) ThumbState.NONE else ThumbState.UP
        if (thumbState == ThumbState.UP) {
          Toast.makeText(context, "Thanks for the feedback!", Toast.LENGTH_SHORT).show()
        }
      }
    )

    // 5. Thumbs Down
    ToolbarActionButton(
      icon = if (thumbState == ThumbState.DOWN) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
      contentDescription = "Thumbs down",
      tint = if (thumbState == ThumbState.DOWN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
      tag = "ai_msg_action_thumbs_down",
      onClick = {
        thumbState = if (thumbState == ThumbState.DOWN) ThumbState.NONE else ThumbState.DOWN
        if (thumbState == ThumbState.DOWN) {
          Toast.makeText(context, "Feedback recorded", Toast.LENGTH_SHORT).show()
        }
      }
    )

    // 6. Regenerate
    ToolbarActionButton(
      icon = Icons.Outlined.Refresh,
      contentDescription = "Regenerate response",
      tag = "ai_msg_action_regenerate",
      onClick = onRegenerateClick
    )
  }
}

/**
 * Message history list with auto-scroll to latest message
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessagesList(
  messages: List<UiChatMessage>,
  isLoading: Boolean,
  onOpenFile: (String) -> Unit,
  onCancelTask: () -> Unit,
  onRegenerate: (String) -> Unit = {},
  onSaveMotion: (MotionProject) -> Unit = {},
  onExportMotion: (MotionProject, ExportFormat) -> Unit = { _, _ -> },
  onEditWithAi: (String) -> Unit = {},
  statusText: String = "Generating response...",
  modelName: String = "NEU\\CODE",
  modifier: Modifier = Modifier
) {
  val listState = rememberLazyListState()
  val ttsState = rememberTtsState()

  // Filter messages to strictly display valid items with content, deduplicated by id
  val displayMessages = remember(messages) {
    messages.filter {
      it.content.isNotBlank() || it.role == MessageRole.USER || it.motionProject != null || it.isMotionGenerating
    }.distinctBy { it.id }
  }

  val isKeyboardOpen = WindowInsets.isImeVisible
  LaunchedEffect(displayMessages.size, isLoading, isKeyboardOpen) {
    if (displayMessages.isNotEmpty() || isLoading) {
      val targetIndex = (displayMessages.size + if (isLoading) 0 else -1).coerceAtLeast(0)
      listState.animateScrollToItem(targetIndex)
    }
  }

  LazyColumn(
    state = listState,
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
  ) {
    items(displayMessages, key = { it.id }) { message ->
      when (message.role) {
        MessageRole.USER -> UserMessageBubble(message = message)
        MessageRole.ASSISTANT -> {
          val messageIndex = displayMessages.indexOf(message)
          val previousUserPrompt = if (messageIndex > 0) {
            displayMessages.subList(0, messageIndex).lastOrNull { it.role == MessageRole.USER }?.content ?: ""
          } else ""

          AssistantMessageItem(
            message = message,
            userPrompt = previousUserPrompt,
            onOpenFile = onOpenFile,
            ttsState = ttsState,
            onRegenerate = { onRegenerate(previousUserPrompt) },
            onSaveMotion = onSaveMotion,
            onExportMotion = onExportMotion,
            onEditWithAi = onEditWithAi
          )
        }
        MessageRole.SYSTEM -> {}
      }
    }

    val hasActiveStreamingAssistant = displayMessages.any {
      it.role == MessageRole.ASSISTANT && (it.isRunning || it.isMotionGenerating)
    }
    if (isLoading && !hasActiveStreamingAssistant) {
      item(key = "transient_loading_indicator") {
        LoadingIndicatorRow(
          statusText = statusText,
          modelName = modelName,
          onCancelTask = onCancelTask
        )
      }
    }
  }
}

/**
 * User Message: Right-aligned bubble in light orange, max width 85%, min 16sp font
 */
@Composable
fun UserMessageBubble(
  message: UiChatMessage,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.End
  ) {
    Surface(
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
      color = com.example.ui.theme.NeuUserBubble,
      border = BorderStroke(1.dp, com.example.ui.theme.NeuUserBubbleBorder),
      modifier = Modifier
        .widthIn(max = 320.dp)
        .padding(start = 48.dp)
    ) {
      Text(
        text = message.content,
        style = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 15.sp,
          lineHeight = 22.sp
        ),
        color = com.example.ui.theme.NeuCodeTextPrimary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
      )
    }
  }
}

/**
 * Assistant Message: Full width, smart response formatted text, monospace prompt blocks,
 * syntax highlighted code blocks, tables, inline 1:1 motion preview card, and horizontal action toolbar.
 */
@Composable
fun AssistantMessageItem(
  message: UiChatMessage,
  userPrompt: String = "",
  onOpenFile: (String) -> Unit,
  ttsState: TtsState,
  onRegenerate: () -> Unit = {},
  onSaveMotion: (MotionProject) -> Unit = {},
  onExportMotion: (MotionProject, ExportFormat) -> Unit = { _, _ -> },
  onEditWithAi: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Header Row: Assistant Avatar and Name
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Image(
        painter = painterResource(id = R.drawable.cat_logo),
        contentDescription = "NEU\\CODE Assistant",
        modifier = Modifier
          .size(28.dp)
          .clip(CircleShape)
      )
      Text(
        text = "NEU\\CODE",
        style = MaterialTheme.typography.titleSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.8.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    // Smart Response Formatting with live streaming indicator
    if (message.content.isNotBlank()) {
      SmartMessageBody(
        content = message.content,
        userPrompt = userPrompt,
        onOpenFile = onOpenFile
      )
    }

    // Claude Motion: Square 1:1 Video/Animation Preview inside chat (Phase 4)
    if (message.motionProject != null) {
      InlineSquareMotionVideoCard(
        project = message.motionProject,
        onEditWithAi = onEditWithAi,
        onSave = { onSaveMotion(message.motionProject) },
        onExport = { fmt -> onExportMotion(message.motionProject, fmt) },
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
      )
    }

    // Real-time Motion generation status (Phase 3)
    if (message.isMotionGenerating) {
      Row(
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ClaudeThinkingOrb(size = 14.dp)
        Text(
          text = message.motionStageText?.takeIf { it.isNotBlank() } ?: "Generating animation...",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else if (message.isRunning) {
      Row(
        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        ClaudeThinkingOrb(size = 14.dp)
        Text(
          text = message.currentStatusText?.takeIf { it.isNotBlank() } ?: "Generating...",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    // Action Toolbar directly below the AI text bubble
    if (!message.isRunning && !message.isMotionGenerating && message.content.isNotBlank()) {
      AssistantActionToolbar(
        message = message,
        isSpeaking = ttsState.speakingMessageId == message.id,
        onPlayClick = { ttsState.speak(message.id, message.content) },
        onRegenerateClick = onRegenerate
      )
    }
  }
}

/**
 * Markdown Body Parser: Handles text, inline formatting, and code blocks
 */
@Composable
fun MarkdownMessageBody(
  content: String,
  onOpenFile: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val segments = remember(content) { parseMarkdownSegments(content) }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    segments.forEach { segment ->
      when (segment) {
        is MarkdownSegment.CodeBlock -> {
          CodeBlockCard(
            code = segment.code,
            language = segment.language
          )
        }
        is MarkdownSegment.Paragraph -> {
          SelectionContainer {
            Text(
              text = segment.text,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}

/**
 * Monospace Code Block Card with rounded surface, horizontal scroll, and copy button
 */
@Composable
fun CodeBlockCard(
  code: String,
  language: String,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier.fillMaxWidth()
  ) {
    Column {
      // Code Block Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = language.ifBlank { "code" },
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          ),
          color = MaterialTheme.colorScheme.primary
        )

        IconButton(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("code_snippet", code))
            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = "Copy code snippet" }
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy snippet",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      // Horizontally scrollable code area
      SelectionContainer {
        Text(
          text = code,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 18.sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(14.dp)
        )
      }
    }
  }
}

/**
 * Animated Running / Loading Row
 */
@Composable
fun LoadingIndicatorRow(
  statusText: String = "Executing directive...",
  modelName: String = "NEU\\CODE",
  onCancelTask: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Image(
      painter = painterResource(id = R.drawable.cat_logo),
      contentDescription = "NEU\\CODE Core",
      modifier = Modifier
        .size(28.dp)
        .clip(CircleShape)
    )
    ClaudeThinkingOrb(size = 20.dp)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = statusText,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = modelName,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    IconButton(
      onClick = onCancelTask,
      modifier = Modifier
        .size(40.dp)
        .semantics { contentDescription = "Cancel active generation task" }
    ) {
      Icon(
        imageVector = Icons.Default.Stop,
        contentDescription = "Stop generation",
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}

/**
 * Material 3 NavigationBar at default height (~80dp) with 24dp icons, 12sp labels, and orange indicator pill
 */
@Composable
fun ChatBottomNavigationBar(
  currentTab: DroidNavTab,
  onTabSelected: (DroidNavTab) -> Unit,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    windowInsets = WindowInsets.navigationBars,
    tonalElevation = 0.dp,
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant))
      .testTag("droid_bottom_navigation")
  ) {
    DroidNavTab.values().forEach { tab ->
      val isSelected = tab == currentTab
      NavigationBarItem(
        selected = isSelected,
        onClick = { onTabSelected(tab) },
        icon = {
          Icon(
            imageVector = tab.icon,
            contentDescription = "${tab.label} tab",
            modifier = Modifier.size(24.dp)
          )
        },
        label = {
          Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = com.example.ui.theme.NeuCodeElectricViolet,
          selectedTextColor = com.example.ui.theme.NeuCodeElectricViolet,
          indicatorColor = com.example.ui.theme.NeuCodePurpleContainer,
          unselectedIconColor = com.example.ui.theme.NeuCodeTextSecondary,
          unselectedTextColor = com.example.ui.theme.NeuCodeTextSecondary
        ),
        modifier = Modifier.testTag(tab.tag)
      )
    }
  }
}

// =========================================================================
// MARKDOWN PARSING HELPER
// =========================================================================

sealed class MarkdownSegment {
  data class Paragraph(val text: String) : MarkdownSegment()
  data class CodeBlock(val code: String, val language: String) : MarkdownSegment()
}

fun parseMarkdownSegments(raw: String): List<MarkdownSegment> {
  val result = mutableListOf<MarkdownSegment>()
  val parts = raw.split("```")

  for (i in parts.indices) {
    val part = parts[i]
    if (i % 2 == 1) {
      // Code block
      val firstNewline = part.indexOf('\n')
      val language = if (firstNewline != -1) part.substring(0, firstNewline).trim() else ""
      val code = if (firstNewline != -1) part.substring(firstNewline + 1) else part
      result.add(MarkdownSegment.CodeBlock(code = code.trimEnd(), language = language))
    } else {
      // Text paragraph
      val trimmed = part.trim()
      if (trimmed.isNotEmpty()) {
        result.add(MarkdownSegment.Paragraph(trimmed))
      }
    }
  }

  if (result.isEmpty()) {
    result.add(MarkdownSegment.Paragraph(raw))
  }
  return result
}

// =========================================================================
// PREVIEWS (360x640, 411x891, Tablet in both Light & Dark)
// =========================================================================

private val previewSampleUiState = ChatUiState(
  messages = listOf(
    UiChatMessage(
      id = "1",
      role = MessageRole.USER,
      content = "How do I create a custom button with ripple in Jetpack Compose?",
      timestamp = System.currentTimeMillis() - 60000
    ),
    UiChatMessage(
      id = "2",
      role = MessageRole.ASSISTANT,
      content = "You can use `Surface` with `onClick` or a `Box` with `Modifier.clickable`:\n\n```kotlin\n@Composable\nfun CustomRippleButton(onClick: () -> Unit) {\n    Surface(\n        onClick = onClick,\n        shape = RoundedCornerShape(12.dp),\n        color = MaterialTheme.colorScheme.primary\n    ) {\n        Text(\"Tap Me\", modifier = Modifier.padding(16.dp))\n    }\n}\n```",
      timestamp = System.currentTimeMillis()
    )
  ),
  inputText = "",
  selectedModel = "GPT-4o",
  isLoading = false
)

@Preview(name = "Small Phone - Empty State Light", widthDp = 360, heightDp = 640)
@Composable
fun PreviewSmallPhoneEmptyLight() {
  DroidCodeTheme(darkTheme = false) {
    ChatScreenContent(
      uiState = ChatUiState(),
      onInputChanged = {},
      onSend = {},
      onModelSelected = {},
      onSuggestionClick = {},
      onCancelTask = {},
      onOpenFile = {},
      onNavigateToSettings = {}
    )
  }
}

@Preview(name = "Small Phone - Empty State Dark", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSmallPhoneEmptyDark() {
  DroidCodeTheme(darkTheme = true) {
    ChatScreenContent(
      uiState = ChatUiState(),
      onInputChanged = {},
      onSend = {},
      onModelSelected = {},
      onSuggestionClick = {},
      onCancelTask = {},
      onOpenFile = {},
      onNavigateToSettings = {}
    )
  }
}

@Preview(name = "Large Phone - Messages Light", widthDp = 411, heightDp = 891)
@Composable
fun PreviewLargePhoneMessagesLight() {
  DroidCodeTheme(darkTheme = false) {
    ChatScreenContent(
      uiState = previewSampleUiState,
      onInputChanged = {},
      onSend = {},
      onModelSelected = {},
      onSuggestionClick = {},
      onCancelTask = {},
      onOpenFile = {},
      onNavigateToSettings = {}
    )
  }
}

@Preview(name = "Large Phone - Messages Dark", widthDp = 411, heightDp = 891, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewLargePhoneMessagesDark() {
  DroidCodeTheme(darkTheme = true) {
    ChatScreenContent(
      uiState = previewSampleUiState,
      onInputChanged = {},
      onSend = {},
      onModelSelected = {},
      onSuggestionClick = {},
      onCancelTask = {},
      onOpenFile = {},
      onNavigateToSettings = {}
    )
  }
}

@Preview(name = "Tablet - Messages Light", widthDp = 800, heightDp = 1280)
@Composable
fun PreviewTabletMessagesLight() {
  DroidCodeTheme(darkTheme = false) {
    ChatScreenContent(
      uiState = previewSampleUiState,
      onInputChanged = {},
      onSend = {},
      onModelSelected = {},
      onSuggestionClick = {},
      onCancelTask = {},
      onOpenFile = {},
      onNavigateToSettings = {}
    )
  }
}

@Preview(name = "Tablet - Messages Dark", widthDp = 800, heightDp = 1280, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewTabletMessagesDark() {
  DroidCodeTheme(darkTheme = true) {
    ChatScreenContent(
      uiState = previewSampleUiState,
      onInputChanged = {},
      onSend = {},
      onModelSelected = {},
      onSuggestionClick = {},
      onCancelTask = {},
      onOpenFile = {},
      onNavigateToSettings = {}
    )
  }
}
