package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Metadata definition for displaying rich model cards in the selection bottom sheet.
 */
data class ModelCardInfo(
  val id: String,
  val displayName: String,
  val badge: String,
  val description: String,
  val provider: String,
  val isRecommended: Boolean = false
)

/**
 * Maps known model IDs and display names to rich metadata.
 */
fun getModelCardInfo(modelNameOrId: String): ModelCardInfo {
  val clean = modelNameOrId.trim()
  return when {
    clean.contains("nemotron", ignoreCase = true) -> ModelCardInfo(
      id = "nvidia/nemotron-3-nano-omni-30b-a3b-reasoning",
      displayName = "Nemotron 30B (Low Latency)",
      badge = "Low Latency",
      description = "Optimized for ultra-fast responses and responsive mobile coding assistance.",
      provider = "NVIDIA NIM",
      isRecommended = true
    )
    clean.contains("3-7-sonnet", ignoreCase = true) || clean.contains("3.7 Sonnet", ignoreCase = true) -> ModelCardInfo(
      id = "claude-3-7-sonnet-20250219",
      displayName = "Claude 3.7 Sonnet",
      badge = "Reasoning",
      description = "State-of-the-art coding, architecture planning, and extended thought reasoning.",
      provider = "Anthropic"
    )
    clean.contains("3-5-sonnet", ignoreCase = true) || clean.contains("3.5 Sonnet", ignoreCase = true) -> ModelCardInfo(
      id = "claude-3-5-sonnet-20241022",
      displayName = "Claude 3.5 Sonnet",
      badge = "Coding",
      description = "Highly capable coding companion with exceptional instruction-following speed.",
      provider = "Anthropic"
    )
    clean.contains("gpt-4o", ignoreCase = true) -> ModelCardInfo(
      id = "gpt-4o",
      displayName = "GPT-4o",
      badge = "Omni",
      description = "Versatile flagship model for complex multimodal coding, logic, and refactoring.",
      provider = "OpenAI"
    )
    clean.contains("glm", ignoreCase = true) -> ModelCardInfo(
      id = "z-ai/glm-5.3-flash",
      displayName = "GLM-5.3 Flash",
      badge = "Fast",
      description = "Lightweight and swift for immediate suggestions, inspections, and quick edits.",
      provider = "Z-AI"
    )
    clean.contains("gemini", ignoreCase = true) -> ModelCardInfo(
      id = "gemini-2.0-flash",
      displayName = "Gemini 2.0 Flash",
      badge = "Next-Gen",
      description = "Low-latency inference with expansive context reasoning and codebase navigation.",
      provider = "Google"
    )
    clean.contains("llama", ignoreCase = true) -> ModelCardInfo(
      id = "llama3.2",
      displayName = "Llama 3.2",
      badge = "Local",
      description = "Efficient local / on-device model execution for offline developer tasks and privacy.",
      provider = "Meta / Ollama"
    )
    else -> ModelCardInfo(
      id = clean,
      displayName = clean,
      badge = "Endpoint",
      description = "Discovered model configured on the active API endpoint.",
      provider = "Custom Endpoint"
    )
  }
}

/**
 * Premium, polished Model Selection Bottom Sheet styled according to the app's warm Claude identity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionBottomSheet(
  selectedModel: String,
  availableModels: List<String>,
  onModelSelected: (String) -> Unit,
  currentEffort: String = "Medium",
  onEffortSelected: (String) -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // Ensure unique list preserving selection and order
  val models = remember(availableModels, selectedModel) {
    val list = availableModels.toMutableList()
    if (selectedModel.isNotBlank() && list.none { it.equals(selectedModel, ignoreCase = true) }) {
      list.add(0, selectedModel)
    }
    list.distinctBy { it.lowercase() }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    scrimColor = Color.Black.copy(alpha = 0.45f),
    dragHandle = {
      // Clean, subtle drag handle bar
      Box(
        modifier = Modifier
          .padding(top = 10.dp, bottom = 4.dp)
          .size(width = 36.dp, height = 4.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.outlineVariant)
      )
    },
    modifier = Modifier.testTag("model_selection_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
    ) {
      // Header: Close Button (Left), Centered "Select Model" Heading, Symmetrical Right Spacer
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(48.dp)
            .testTag("model_sheet_close_btn")
            .semantics { contentDescription = "Close model selection" }
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
          )
        }

        Text(
          text = "Select Model",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            letterSpacing = (-0.2).sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f)
        )

        // Matching 48dp spacer to keep title perfectly centered
        Spacer(modifier = Modifier.size(48.dp))
      }

      Divider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp
      )

      // Scrollable Model Cards and Configuration Section
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 520.dp)
          .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        item {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "AVAILABLE MODELS",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 11.sp,
              letterSpacing = 0.8.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
          )
        }

        // Model Cards List
        items(models) { modelRaw ->
          val info = remember(modelRaw) { getModelCardInfo(modelRaw) }
          val isSelected = info.displayName.equals(selectedModel, ignoreCase = true) ||
            info.id.equals(selectedModel, ignoreCase = true) ||
            modelRaw.equals(selectedModel, ignoreCase = true)

          ModelCardItem(
            info = info,
            isSelected = isSelected,
            onClick = {
              onModelSelected(info.displayName)
            }
          )
        }

        // Section: Reasoning Effort Settings
        item {
          Spacer(modifier = Modifier.height(10.dp))
          ReasoningEffortSection(
            currentEffort = currentEffort,
            onEffortSelected = onEffortSelected
          )
        }

        // Section: Settings Navigation Shortcut
        item {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onNavigateToSettings() }
              .testTag("model_sheet_manage_providers_btn")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.Settings,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Column {
                  Text(
                    text = "Manage Endpoints & API Keys",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Configure custom base URLs, local Ollama, and API tokens",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
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
        }
      }
    }
  }
}

/**
 * Individual selectable model card with badge, description, and visual selection indicator.
 */
@Composable
fun ModelCardItem(
  info: ModelCardInfo,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val borderColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
    animationSpec = tween(180),
    label = "card_border"
  )

  val containerColor by animateColorAsState(
    targetValue = if (isSelected) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    },
    animationSpec = tween(180),
    label = "card_bg"
  )

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = containerColor,
    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .testTag("model_card_${info.displayName.lowercase().replace(" ", "_").replace("(", "").replace(")", "")}")
      .semantics { contentDescription = "Select ${info.displayName}${if (isSelected) ", currently selected" else ""}" }
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        // Name and Badge Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = info.displayName,
            style = MaterialTheme.typography.titleSmall.copy(
              fontSize = 15.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface
          )

          // Category / Capability Badge
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSelected) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
              MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(
              width = 1.dp,
              color = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
              } else {
                MaterialTheme.colorScheme.outlineVariant
              }
            )
          ) {
            Text(
              text = info.badge,
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
              ),
              color = if (isSelected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        // Description
        Text(
          text = info.description,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Provider attribution
        Text(
          text = info.provider,
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
      }

      // Selection Checkmark Indicator
      if (isSelected) {
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Selected",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
          )
        }
      } else {
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
      }
    }
  }
}

/**
 * Reasoning Effort Section: Supports Low, Medium, High thinking depth settings
 * directly connected to inference temperature and max reasoning iterations.
 */
@Composable
fun ReasoningEffortSection(
  currentEffort: String,
  onEffortSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Tune,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "Reasoning Effort",
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      Text(
        text = currentEffort,
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
      )
    }

    Text(
      text = "Balances response speed against thinking depth and step iterations.",
      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Segmented Pill Row for Low / Medium / High
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
        .padding(3.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      listOf("Low", "Medium", "High").forEach { effort ->
        val isSelected = effort.equals(currentEffort, ignoreCase = true)
        val effortBg by animateColorAsState(
          targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
          animationSpec = tween(160),
          label = "effort_bg"
        )
        val textColor by animateColorAsState(
          targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
          animationSpec = tween(160),
          label = "effort_text"
        )

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = effortBg,
          modifier = Modifier
            .weight(1f)
            .clickable { onEffortSelected(effort) }
            .testTag("effort_option_${effort.lowercase()}")
        ) {
          Text(
            text = effort,
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              fontSize = 12.sp
            ),
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
          )
        }
      }
    }
  }
}
