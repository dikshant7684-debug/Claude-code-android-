package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ClaudeTerracotta
import com.example.ui.theme.DroidOrangeDark
import com.example.ui.theme.DroidOrangeLight

/**
 * Animated Claude Thinking Orb.
 * Displays a multi-layered warm terracotta orb with continuous smooth rotation,
 * subtle breathing scale, soft radial glow, and fluid color transitions.
 */
@Composable
fun ClaudeThinkingOrb(
  modifier: Modifier = Modifier,
  size: Dp = 28.dp,
  primaryColor: Color = ClaudeTerracotta
) {
  val transition = rememberInfiniteTransition(label = "claude_orb_transition")

  // Smooth continuous rotation (2.6s)
  val rotation by transition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2600, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "orb_rotation"
  )

  // Subtle breathing pulse scale (0.92x to 1.08x)
  val scale by transition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.08f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "orb_pulse"
  )

  // Gentle shimmer glow opacity (0.75 to 1.0)
  val glowAlpha by transition.animateFloat(
    initialValue = 0.70f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "orb_glow"
  )

  val orbColors = listOf(
    primaryColor,
    Color(0xFFFF8A65), // Warm coral
    Color(0xFFFFB74D), // Amber gold
    Color(0xFFE64A19), // Deep burnt orange
    Color(0xFFD97757), // Terracotta
    primaryColor
  )

  Canvas(
    modifier = modifier
      .size(size)
      .testTag("claude_thinking_orb")
  ) {
    val center = Offset(this.size.width / 2f, this.size.height / 2f)
    val baseRadius = (this.size.minDimension / 2f) * 0.82f
    val currentRadius = baseRadius * scale

    // 1. Outer ambient radial glow
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          primaryColor.copy(alpha = 0.35f * glowAlpha),
          primaryColor.copy(alpha = 0.12f * glowAlpha),
          Color.Transparent
        ),
        center = center,
        radius = currentRadius * 1.35f
      ),
      radius = currentRadius * 1.35f,
      center = center
    )

    // 2. Rotating swirling sweep gradient layer
    rotate(degrees = rotation, pivot = center) {
      drawCircle(
        brush = Brush.sweepGradient(
          colors = orbColors,
          center = center
        ),
        radius = currentRadius,
        center = center
      )
    }

    // 3. Inner radial core with luminous warmth
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFFFFF3E0).copy(alpha = 0.90f * glowAlpha), // Warm white center glint
          primaryColor.copy(alpha = 0.75f),
          DroidOrangeDark.copy(alpha = 0.95f)
        ),
        center = Offset(center.x - currentRadius * 0.15f, center.y - currentRadius * 0.15f),
        radius = currentRadius * 0.85f
      ),
      radius = currentRadius * 0.70f,
      center = center
    )

    // 4. Subtle top-left light specular reflection
    drawCircle(
      color = Color.White.copy(alpha = 0.65f * glowAlpha),
      radius = currentRadius * 0.22f,
      center = Offset(center.x - currentRadius * 0.28f, center.y - currentRadius * 0.28f)
    )
  }
}

/**
 * Single authoritative in-progress status row for assistant responses.
 * Combines the Claude Thinking Orb with ONE concise status message.
 */
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
    ClaudeThinkingOrb(size = orbSize)

    Text(
      text = statusText.ifBlank { "Thinking..." },
      style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}
