package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeuCodeBgSecondary
import com.example.ui.theme.NeuCodeBorder
import com.example.ui.theme.NeuCodeBorderActive
import com.example.ui.theme.NeuCodeElectricViolet
import com.example.ui.theme.NeuCodeError
import com.example.ui.theme.NeuCodeInfo
import com.example.ui.theme.NeuCodeMutedPurple
import com.example.ui.theme.NeuCodePurpleDeep
import com.example.ui.theme.NeuCodePurplePrimary
import com.example.ui.theme.NeuCodeSoftLavender
import com.example.ui.theme.NeuCodeSuccess
import com.example.ui.theme.NeuCodeSurfacePrimary
import com.example.ui.theme.NeuCodeTextMuted
import com.example.ui.theme.NeuCodeTextPrimary
import com.example.ui.theme.NeuCodeTextSecondary
import com.example.ui.theme.NeuCodeWarning

/**
 * Supported execution states for the NEU\CODE Intelligence Core.
 */
enum class NeuCoreState {
  IDLE,
  THINKING,
  ANALYZING,
  SEARCHING,
  CODING,
  TOOL_EXECUTION,
  COMPLETED,
  ERROR
}

/**
 * NEU\CODE Nebula Intelligence Core.
 * Renders an animated nebula-inspired AI intelligence core featuring:
 * - Central electric violet core with soft lavender highlights
 * - Multiple counter-rotating orbital rings (segmented & dashed)
 * - State-driven animation rhythms (pulsing, searching scans, rapid code pulses, calm idle)
 */
@Composable
fun NeuCodeIntelligenceCore(
  modifier: Modifier = Modifier,
  size: Dp = 32.dp,
  state: NeuCoreState = NeuCoreState.THINKING
) {
  val transition = rememberInfiniteTransition(label = "neucode_core_transition")

  // Outer orbital ring rotation duration tailored by state
  val outerDuration = when (state) {
    NeuCoreState.CODING -> 1800
    NeuCoreState.ANALYZING -> 2400
    NeuCoreState.SEARCHING -> 2000
    NeuCoreState.TOOL_EXECUTION -> 2200
    NeuCoreState.IDLE -> 6000
    NeuCoreState.COMPLETED -> 8000
    NeuCoreState.ERROR -> 10000
    NeuCoreState.THINKING -> 3200
  }

  val outerRotation by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = outerDuration, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "neucode_outer_rotation"
  )

  // Inner orbital ring counter-rotation
  val innerRotation by transition.animateFloat(
    initialValue = 360f,
    targetValue = 0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = (outerDuration * 0.65f).toInt().coerceAtLeast(1000), easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "neucode_inner_rotation"
  )

  // Central core pulse scale
  val pulseTarget = when (state) {
    NeuCoreState.CODING -> 1.18f
    NeuCoreState.SEARCHING -> 1.15f
    NeuCoreState.TOOL_EXECUTION -> 1.14f
    NeuCoreState.ERROR -> 1.05f
    NeuCoreState.IDLE -> 1.04f
    else -> 1.10f
  }

  val pulseDuration = when (state) {
    NeuCoreState.CODING -> 650
    NeuCoreState.SEARCHING -> 750
    NeuCoreState.IDLE -> 2400
    else -> 1100
  }

  val pulseScale by transition.animateFloat(
    initialValue = 0.90f,
    targetValue = pulseTarget,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "neucode_pulse_scale"
  )

  // Glow aura intensity
  val glowAlpha by transition.animateFloat(
    initialValue = 0.45f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = pulseDuration, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "neucode_glow_alpha"
  )

  val statePrimaryColor = when (state) {
    NeuCoreState.ERROR -> NeuCodeError
    NeuCoreState.COMPLETED -> NeuCodeSuccess
    NeuCoreState.SEARCHING -> NeuCodeInfo
    NeuCoreState.ANALYZING -> NeuCodeElectricViolet
    NeuCoreState.CODING -> NeuCodeSoftLavender
    NeuCoreState.TOOL_EXECUTION -> NeuCodeWarning
    else -> NeuCodePurplePrimary
  }

  val stateAccentColor = when (state) {
    NeuCoreState.ERROR -> Color(0xFFB91C1C)
    NeuCoreState.COMPLETED -> Color(0xFF15803D)
    NeuCoreState.SEARCHING -> Color(0xFF2563EB)
    else -> NeuCodeElectricViolet
  }

  Canvas(
    modifier = modifier
      .size(size)
      .testTag("neucode_intelligence_core")
  ) {
    val center = Offset(this.size.width / 2f, this.size.height / 2f)
    val maxRadius = (this.size.minDimension / 2f) * 0.92f
    val coreRadius = maxRadius * 0.44f * pulseScale

    // 1. Soft Cosmic Radial Aura
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          statePrimaryColor.copy(alpha = 0.35f * glowAlpha),
          NeuCodePurpleDeep.copy(alpha = 0.14f * glowAlpha),
          Color.Transparent
        ),
        center = center,
        radius = maxRadius * 1.45f
      ),
      radius = maxRadius * 1.45f,
      center = center
    )

    // 2. Outer Technical Segmented Ring (Rotating clockwise)
    rotate(degrees = outerRotation, pivot = center) {
      drawArc(
        color = statePrimaryColor.copy(alpha = 0.85f),
        startAngle = 10f,
        sweepAngle = 70f,
        useCenter = false,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
      )
      drawArc(
        color = statePrimaryColor.copy(alpha = 0.85f),
        startAngle = 100f,
        sweepAngle = 70f,
        useCenter = false,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
      )
      drawArc(
        color = statePrimaryColor.copy(alpha = 0.85f),
        startAngle = 190f,
        sweepAngle = 70f,
        useCenter = false,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
      )
      drawArc(
        color = statePrimaryColor.copy(alpha = 0.85f),
        startAngle = 280f,
        sweepAngle = 70f,
        useCenter = false,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
      )
    }

    // 3. Middle Fine Dashed Orbital Ring (Counter-clockwise)
    rotate(degrees = innerRotation, pivot = center) {
      drawCircle(
        color = NeuCodeMutedPurple.copy(alpha = 0.65f),
        radius = maxRadius * 0.70f,
        center = center,
        style = Stroke(
          width = 1.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
        )
      )
    }

    // 4. Central Nebula Energy Core
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Color.White.copy(alpha = 0.95f * glowAlpha),
          stateAccentColor,
          statePrimaryColor,
          NeuCodePurpleDeep
        ),
        center = center,
        radius = coreRadius
      ),
      radius = coreRadius,
      center = center
    )
  }
}

/**
 * Full-fidelity Telemetry Card for NEU\CODE Agent execution.
 * Surfaces real agent events: current task, working file, active tool, execution status, completed & pending count.
 */
@Composable
fun NeuCodeTelemetryCard(
  statusText: String,
  currentFile: String? = null,
  activeTool: String? = null,
  completedOps: Int = 0,
  pendingOps: Int = 0,
  coreState: NeuCoreState = NeuCoreState.THINKING,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = NeuCodeSurfacePrimary,
    border = BorderStroke(1.dp, NeuCodeBorderActive),
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
      .testTag("neucode_telemetry_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      NeuCodeIntelligenceCore(size = 34.dp, state = coreState)

      Column(modifier = Modifier.weight(1f)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = "NEU\\CODE CORE",
              style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
              ),
              color = NeuCodeElectricViolet
            )
            Text(
              text = "• ${coreState.name}",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
              ),
              color = NeuCodeTextSecondary
            )
          }

          if (completedOps > 0 || pendingOps > 0) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              if (completedOps > 0) {
                Text(
                  text = "$completedOps done",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = NeuCodeSuccess
                )
              }
              if (pendingOps > 0) {
                Text(
                  text = "($pendingOps pending)",
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                  color = NeuCodeTextMuted
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
          text = statusText.ifBlank { "Executing directive..." },
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          ),
          color = NeuCodeTextPrimary
        )

        if (!currentFile.isNullOrBlank() || !activeTool.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (!activeTool.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeuCodeBgSecondary,
                border = BorderStroke(1.dp, NeuCodeBorder)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Build,
                    contentDescription = null,
                    tint = NeuCodePurplePrimary,
                    modifier = Modifier.size(11.dp)
                  )
                  Text(
                    text = activeTool,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontFamily = FontFamily.Monospace,
                      fontSize = 10.sp
                    ),
                    color = NeuCodeTextSecondary
                  )
                }
              }
            }

            if (!currentFile.isNullOrBlank()) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = NeuCodeBgSecondary,
                border = BorderStroke(1.dp, NeuCodeBorder)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = NeuCodeSoftLavender,
                    modifier = Modifier.size(11.dp)
                  )
                  Text(
                    text = currentFile,
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
      }
    }
  }
}
