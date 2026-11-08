package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeuCodePurplePrimary
import com.example.ui.theme.NeuCodeTextPrimary

/**
 * Backward-compatibility wrappers linking to NeuCodeIntelligenceCore
 */
@Composable
fun ClaudeThinkingOrb(
  modifier: Modifier = Modifier,
  size: Dp = 28.dp,
  primaryColor: Color = NeuCodePurplePrimary
) {
  NeuCodeIntelligenceCore(modifier = modifier, size = size, state = NeuCoreState.THINKING)
}

@Composable
fun UltronIntelligenceCore(
  modifier: Modifier = Modifier,
  size: Dp = 32.dp,
  primaryColor: Color = NeuCodePurplePrimary
) {
  NeuCodeIntelligenceCore(modifier = modifier, size = size, state = NeuCoreState.THINKING)
}

@Composable
fun UltronThinkingIndicatorCard(
  statusText: String,
  currentFile: String? = null,
  activeTool: String? = null,
  modifier: Modifier = Modifier
) {
  NeuCodeTelemetryCard(
    statusText = statusText,
    currentFile = currentFile,
    activeTool = activeTool,
    coreState = if (!activeTool.isNullOrBlank()) NeuCoreState.TOOL_EXECUTION else NeuCoreState.THINKING,
    modifier = modifier
  )
}

@Composable
fun ClaudeThinkingIndicatorRow(
  statusText: String,
  modifier: Modifier = Modifier,
  orbSize: Dp = 26.dp
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    NeuCodeIntelligenceCore(size = orbSize, state = NeuCoreState.THINKING)

    Text(
      text = statusText.ifBlank { "NEU\\CODE Core: Processing directive..." },
      style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = NeuCodeTextPrimary
      )
    )
  }
}
