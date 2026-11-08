package com.example.ui.components

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.system.AgentActivityManager
import com.example.ui.system.CodeBlockRenderer
import com.example.ui.system.CommandPaletteManager
import com.example.ui.system.DiffViewer
import com.example.ui.system.TaskProgressManager
import com.example.ui.system.ThinkingOrbView
import com.example.ui.system.WorkingFileManager
import com.example.ui.theme.NeuCodeBg
import com.example.ui.theme.NeuCodeBorder
import com.example.ui.theme.NeuCodeBorderActive
import com.example.ui.theme.NeuCodeElectricViolet
import com.example.ui.theme.NeuCodeLavender
import com.example.ui.theme.NeuCodePurplePrimary
import com.example.ui.theme.NeuCodeStatusError
import com.example.ui.theme.NeuCodeStatusSuccess
import com.example.ui.theme.NeuCodeStatusWarning
import com.example.ui.theme.NeuCodeSurfaceElevated
import com.example.ui.theme.NeuCodeSurfacePrimary
import com.example.ui.theme.NeuCodeSurfaceSecondary
import com.example.ui.theme.NeuCodeTextMuted
import com.example.ui.theme.NeuCodeTextPrimary
import com.example.ui.theme.NeuCodeTextSecondary

/**
 * Native Android View bridge for the Animated AI Intelligence Core.
 */
@Composable
fun NativeThinkingOrb(
  state: ThinkingOrbView.State,
  modifier: Modifier = Modifier
) {
  AndroidView(
    factory = { ctx ->
      ThinkingOrbView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      }
    },
    update = { view ->
      view.setState(state)
    },
    modifier = modifier
  )
}

/**
 * Real-Time Tool Activity Component showing operation duration and status.
 */
@Composable
fun RealTimeActivityCard(
  activity: AgentActivityManager.ActivityItem,
  onToggleExpand: () -> Unit,
  modifier: Modifier = Modifier
) {
  val statusColor = when (activity.status) {
    AgentActivityManager.Status.COMPLETED -> NeuCodeStatusSuccess
    AgentActivityManager.Status.FAILED -> NeuCodeStatusError
    AgentActivityManager.Status.CANCELLED -> NeuCodeStatusWarning
    AgentActivityManager.Status.RUNNING -> NeuCodeElectricViolet
    AgentActivityManager.Status.PENDING -> NeuCodeTextMuted
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = NeuCodeSurfacePrimary,
    border = BorderStroke(1.dp, NeuCodeBorder),
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize()
      .clickable { onToggleExpand() }
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(statusColor)
          )
          Text(
            text = activity.operationName,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp
            ),
            color = NeuCodeTextPrimary
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = activity.formattedDuration,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = NeuCodeTextMuted
          )
          Icon(
            imageVector = if (activity.isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = "Toggle details",
            tint = NeuCodeTextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      if (activity.resourcePath.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "◈ " + activity.resourcePath,
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
          ),
          color = NeuCodeLavender
        )
      }

      if (activity.isExpanded && activity.details.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = NeuCodeSurfaceSecondary,
          border = BorderStroke(1.dp, NeuCodeBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = activity.details,
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              lineHeight = 16.sp
            ),
            color = NeuCodeTextSecondary,
            modifier = Modifier.padding(8.dp)
          )
        }
      }
    }
  }
}

/**
 * Current Working File Indicator.
 */
@Composable
fun WorkingFileIndicatorBar(
  files: List<WorkingFileManager.ActiveFile>,
  onFileClick: (WorkingFileManager.ActiveFile) -> Unit,
  modifier: Modifier = Modifier
) {
  if (files.isEmpty()) return

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = NeuCodeSurfacePrimary,
    border = BorderStroke(1.dp, NeuCodeBorderActive),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "◈ WORKING ON",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            fontSize = 10.sp
          ),
          color = NeuCodeElectricViolet
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      files.take(3).forEach { file ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onFileClick(file) }
            .padding(vertical = 3.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Description,
              contentDescription = null,
              tint = NeuCodeLavender,
              modifier = Modifier.size(14.dp)
            )
            Text(
              text = file.fileName,
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
              ),
              color = NeuCodeTextPrimary
            )
            Text(
              text = "(" + file.currentOperation + ")",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = NeuCodeTextMuted
            )
          }
          Text(
            text = file.relativePath,
            style = MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            ),
            color = NeuCodeTextSecondary
          )
        }
      }
    }
  }
}

/**
 * Live Task Progress Component.
 */
@Composable
fun LiveTaskProgressCard(
  progress: TaskProgressManager.TaskProgress?,
  modifier: Modifier = Modifier
) {
  if (progress == null) return

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = NeuCodeSurfacePrimary,
    border = BorderStroke(1.dp, NeuCodeBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = progress.taskName,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          ),
          color = NeuCodeTextPrimary
        )
        Text(
          text = progress.formattedElapsed,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
          color = NeuCodeLavender
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      progress.operations.forEach { op ->
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          when (op.state) {
            TaskProgressManager.OpState.COMPLETED -> {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = NeuCodeStatusSuccess,
                modifier = Modifier.size(14.dp)
              )
            }
            TaskProgressManager.OpState.ACTIVE -> {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(NeuCodeElectricViolet)
              )
            }
            TaskProgressManager.OpState.FAILED -> {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Failed",
                tint = NeuCodeStatusError,
                modifier = Modifier.size(14.dp)
              )
            }
            TaskProgressManager.OpState.PENDING -> {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(NeuCodeTextMuted.copy(alpha = 0.5f))
              )
            }
          }
          Text(
            text = op.title,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = if (op.state == TaskProgressManager.OpState.ACTIVE) NeuCodeTextPrimary else NeuCodeTextSecondary
          )
        }
      }
    }
  }
}

/**
 * Bottom Command Palette Overlay triggered by typing '/' in the composer.
 */
@Composable
fun CommandPaletteMenu(
  filterQuery: String,
  onSelectCommand: (String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val commands = remember(filterQuery) {
    CommandPaletteManager.filterCommands(filterQuery)
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = NeuCodeSurfacePrimary,
    border = BorderStroke(1.dp, NeuCodeBorderActive),
    shadowElevation = 8.dp,
    modifier = modifier
      .fillMaxWidth()
      .heightIn(max = 280.dp)
      .testTag("command_palette_overlay")
  ) {
    Column(modifier = Modifier.padding(8.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "COMMAND PALETTE",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontSize = 10.sp
          ),
          color = NeuCodeElectricViolet
        )
        Text(
          text = "${commands.size} available",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = NeuCodeTextMuted
        )
      }

      LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(commands) { cmd ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable {
                onSelectCommand(cmd.command)
              }
              .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            val icon = when (cmd.iconType) {
              "terminal", "build", "test" -> Icons.Default.Terminal
              "files", "search" -> Icons.Default.Folder
              "settings" -> Icons.Default.Settings
              else -> Icons.Default.Code
            }
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = NeuCodeLavender,
              modifier = Modifier.size(16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = cmd.command,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                ),
                color = NeuCodeTextPrimary
              )
              Text(
                text = cmd.description,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = NeuCodeTextSecondary
              )
            }
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = NeuCodeSurfaceElevated,
              border = BorderStroke(1.dp, NeuCodeBorder)
            ) {
              Text(
                text = cmd.category,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = NeuCodeTextMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Unified Code Block View with syntax highlighting and copy button.
 */
@Composable
fun InteractiveCodeBlock(
  code: String,
  language: String = "kotlin",
  fileName: String? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var copied by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = NeuCodeSurfacePrimary,
    border = BorderStroke(1.dp, NeuCodeBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column {
      // Header Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(NeuCodeSurfaceSecondary)
          .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = fileName ?: language.uppercase(),
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          ),
          color = NeuCodeLavender
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable {
              CodeBlockRenderer.copyToClipboard(context, fileName ?: "Code", code)
              copied = true
            }
            .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
          Icon(
            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = "Copy code",
            tint = if (copied) NeuCodeStatusSuccess else NeuCodeTextSecondary,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (copied) "Copied!" else "Copy",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (copied) NeuCodeStatusSuccess else NeuCodeTextSecondary
          )
        }
      }

      // Code Body with horizontal scroll
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp)
          .horizontalScroll(rememberScrollState())
      ) {
        Text(
          text = code,
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp
          ),
          color = NeuCodeTextPrimary
        )
      }
    }
  }
}
