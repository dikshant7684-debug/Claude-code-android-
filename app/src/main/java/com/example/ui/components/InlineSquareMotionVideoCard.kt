package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.motion.engine.MotionHtmlRenderer
import com.example.motion.model.ExportFormat
import com.example.motion.model.MotionProject
import com.example.ui.theme.ClaudeTerracotta
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Android Javascript Interface connecting WebView playback clock to Compose State.
 */
class InlineMotionBridge(
  private val onTimeUpdateCallback: (Double, Double) -> Unit,
  private val onPlaybackEndedCallback: () -> Unit
) {
  @JavascriptInterface
  fun onTimeUpdate(currentTimeSec: Double, totalDurationSec: Double) {
    onTimeUpdateCallback(currentTimeSec, totalDurationSec)
  }

  @JavascriptInterface
  fun onPlaybackEnded() {
    onPlaybackEndedCallback()
  }
}

/**
 * Dedicated Square 1:1 Video/Animation Preview Card rendered directly inside AI chat messages.
 * Fulfills Phase 4, Phase 6, Phase 7, and Phase 8.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InlineSquareMotionVideoCard(
  project: MotionProject,
  onEditWithAi: (String) -> Unit,
  onSave: () -> Unit,
  onExport: (ExportFormat) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  var isPlaying by remember { mutableStateOf(true) }
  var currentTime by remember { mutableDoubleStateOf(0.0) }
  var totalDuration by remember { mutableDoubleStateOf(project.durationSeconds.coerceAtLeast(1.0)) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var isWebViewSupported by remember { mutableStateOf(true) }
  var showQuickEdits by remember { mutableStateOf(false) }
  var showExportSheet by remember { mutableStateOf(false) }

  val executableHtml = remember(project) {
    if (project.executableHtml.isNotBlank()) project.executableHtml
    else MotionHtmlRenderer.generateExecutableHtml(project)
  }

  // Synchronize playback state changes with the WebView animation clock
  LaunchedEffect(isPlaying) {
    try {
      webViewRef?.evaluateJavascript(
        if (isPlaying) "if (window.ClaudeMotion) window.ClaudeMotion.play();" else "if (window.ClaudeMotion) window.ClaudeMotion.pause();",
        null
      )
    } catch (_: Throwable) {}
  }

  // Reload HTML when project updates
  LaunchedEffect(executableHtml) {
    try {
      webViewRef?.loadDataWithBaseURL("https://claude.motion.local/", executableHtml, "text/html", "UTF-8", null)
    } catch (_: Throwable) {}
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF10121A)),
    border = BorderStroke(1.dp, Color(0xFF262A38)),
    modifier = modifier
      .fillMaxWidth()
      .testTag("inline_motion_preview_card_${project.id}")
  ) {
    Column {
      // 1. TOP HEADER: Project Name, Duration & Style Badge
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF161924))
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.weight(1f)
        ) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = ClaudeTerracotta.copy(alpha = 0.2f),
            modifier = Modifier.size(28.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.MovieFilter,
                contentDescription = null,
                tint = ClaudeTerracotta,
                modifier = Modifier.size(16.dp)
              )
            }
          }
          Column {
            Text(
              text = project.name,
              style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              ),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "${project.style.displayName} • ${project.durationSeconds.toInt()}s • ${project.aspectRatio.label}",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
              color = Color.White.copy(alpha = 0.6f)
            )
          }
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = ClaudeTerracotta.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, ClaudeTerracotta.copy(alpha = 0.35f))
        ) {
          Text(
            text = "1:1 PREVIEW",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = ClaudeTerracotta,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }

      // 2. THE SQUARE (1:1 / 4:4) PREVIEW CONTAINER (Phase 4 & Phase 8)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(1f)
          .background(Color(0xFF07080C))
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
          ) {
            isPlaying = !isPlaying
          },
        contentAlignment = Alignment.Center
      ) {
        // Video viewport letterboxed to respect original format (e.g. 16:9, 9:16, 1:1)
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(project.aspectRatio.ratio)
            .background(Color.Black),
          contentAlignment = Alignment.Center
        ) {
          if (isWebViewSupported) {
            AndroidInlineWebView(
              html = executableHtml,
              onWebViewReady = { webView -> webViewRef = webView },
              onTimeUpdate = { t, d ->
                currentTime = t
                if (d > 0.0) totalDuration = d
              },
              onPlaybackEnded = { isPlaying = false },
              onFailure = { isWebViewSupported = false }
            )
          } else {
            // High-performance Native Compose Canvas Fallback
            NativeComposeCanvasPlayer(
              project = project,
              isPlaying = isPlaying,
              currentTime = currentTime,
              totalDuration = totalDuration
            )
          }
        }

        // Centered Play/Pause Overlay Button (Fades out when playing)
        if (!isPlaying) {
          Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier.size(54.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Animation",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
              )
            }
          }
        }
      }

      // 3. PLAYBACK SCRUBBER & CONTROLS
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF141722))
          .padding(horizontal = 12.dp, vertical = 8.dp)
      ) {
        // Scrubber Slider
        Slider(
          value = currentTime.toFloat(),
          onValueChange = { targetSec ->
            currentTime = targetSec.toDouble()
            try {
              webViewRef?.evaluateJavascript("if (window.ClaudeMotion) window.ClaudeMotion.seek($targetSec);", null)
            } catch (_: Throwable) {}
          },
          valueRange = 0f..totalDuration.toFloat(),
          colors = SliderDefaults.colors(
            thumbColor = ClaudeTerracotta,
            activeTrackColor = ClaudeTerracotta,
            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .testTag("inline_motion_scrubber")
        )

        // Time Indicator & Replay Control Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            IconButton(
              onClick = { isPlaying = !isPlaying },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            IconButton(
              onClick = {
                currentTime = 0.0
                isPlaying = true
                try {
                  webViewRef?.evaluateJavascript("if (window.ClaudeMotion) window.ClaudeMotion.replay();", null)
                } catch (_: Throwable) {}
              },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = "Replay Animation",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
              )
            }
          }

          Text(
            text = "${formatTimeSeconds(currentTime)} / ${formatTimeSeconds(totalDuration)}",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace
            ),
            color = Color.White.copy(alpha = 0.7f)
          )
        }
      }

      // 4. COMPACT ACTION ROW (Phase 7: Edit with AI, Replay, Save, Export)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(Color(0xFF0F1117))
          .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Edit with AI
        Button(
          onClick = { showQuickEdits = !showQuickEdits },
          colors = ButtonDefaults.buttonColors(
            containerColor = ClaudeTerracotta,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1.2f)
            .testTag("inline_motion_edit_btn")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = null,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Edit with AI",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }

        // Save Video Action
        OutlinedButton(
          onClick = onSave,
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("inline_motion_save_btn")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Save,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Save",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
          }
        }

        // Export Action
        OutlinedButton(
          onClick = { showExportSheet = true },
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("inline_motion_export_btn")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.FileDownload,
              contentDescription = null,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Export",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            )
          }
        }
      }

      // Quick edit suggestions bar (Revealed when Edit with AI is tapped)
      AnimatedVisibility(visible = showQuickEdits) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131620))
            .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
          Text(
            text = "Suggested Edits (Tap to apply or type your own instructions):",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color.White.copy(alpha = 0.6f)
          )
          Spacer(modifier = Modifier.height(6.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val quickPrompts = listOf(
              "Change background to pure black",
              "Slow down the animation",
              "Make it more cinematic",
              "Add glow particles",
              "Change format to vertical 9:16",
              "Speed up transitions"
            )
            quickPrompts.forEach { editPrompt ->
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier.clickable {
                  showQuickEdits = false
                  onEditWithAi(editPrompt)
                }
              ) {
                Text(
                  text = editPrompt,
                  style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                  color = Color.White,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
              }
            }
          }
        }
      }
    }
  }

  // Export Modal Sheet
  if (showExportSheet) {
    ModalBottomSheet(
      onDismissRequest = { showExportSheet = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = Color(0xFF161924)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = "Export Animation Package",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          ),
          color = Color.White
        )
        Text(
          text = "Choose your desired distribution format for \"${project.name}\":",
          style = MaterialTheme.typography.bodySmall,
          color = Color.White.copy(alpha = 0.7f)
        )

        val formats = listOf(
          ExportFormat.MP4 to "MP4 Video (H.264 Universal)",
          ExportFormat.WEBM to "WebM Video (VP9 Web Optimized)",
          ExportFormat.HTML_BUNDLE to "Standalone HTML5 / Canvas Package"
        )

        formats.forEach { (fmt, label) ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.05f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                showExportSheet = false
                onExport(fmt)
              }
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = label,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = Color.White
                )
                Text(
                  text = "Native 60fps • ${project.aspectRatio.label}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.White.copy(alpha = 0.5f)
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = ClaudeTerracotta,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

/**
 * Isolated Hardware-Accelerated WebView player with bridge and safe fallback.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AndroidInlineWebView(
  html: String,
  onWebViewReady: (WebView) -> Unit,
  onTimeUpdate: (Double, Double) -> Unit,
  onPlaybackEnded: () -> Unit,
  onFailure: () -> Unit
) {
  AndroidView(
    factory = { ctx ->
      try {
        WebView(ctx).apply {
          tag = html
          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_NO_CACHE
          }
          setBackgroundColor(android.graphics.Color.TRANSPARENT)
          webChromeClient = WebChromeClient()
          webViewClient = WebViewClient()

          addJavascriptInterface(
            InlineMotionBridge(
              onTimeUpdateCallback = onTimeUpdate,
              onPlaybackEndedCallback = onPlaybackEnded
            ),
            "AndroidMotionBridge"
          )

          loadDataWithBaseURL("https://claude.motion.local/", html, "text/html", "UTF-8", null)
          onWebViewReady(this)
        }
      } catch (t: Throwable) {
        onFailure()
        android.view.View(ctx)
      }
    },
    update = { webView ->
      if (webView is WebView) {
        try {
          if (webView.tag != html) {
            webView.tag = html
            webView.loadDataWithBaseURL("https://claude.motion.local/", html, "text/html", "UTF-8", null)
          }
        } catch (_: Throwable) {
          onFailure()
        }
      }
    },
    modifier = Modifier.fillMaxSize()
  )
}

/**
 * High-performance Native Compose Canvas fallback for environments without WebView.
 */
@Composable
private fun NativeComposeCanvasPlayer(
  project: MotionProject,
  isPlaying: Boolean,
  currentTime: Double,
  totalDuration: Double
) {
  val primaryScene = project.scenes.firstOrNull()
  val bgColor = remember(primaryScene?.background) {
    try {
      Color(android.graphics.Color.parseColor(primaryScene?.background ?: "#12141D"))
    } catch (_: Exception) {
      Color(0xFF12141D)
    }
  }

  val progress = (currentTime / totalDuration.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(bgColor),
    contentAlignment = Alignment.Center
  ) {
    // Dynamic animated rings & progress particles
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val center = Offset(w / 2f, h / 2f)
      val radius = (minOf(w, h) * 0.32f)

      // Background Track
      drawCircle(
        color = Color.White.copy(alpha = 0.08f),
        radius = radius,
        center = center,
        style = Stroke(width = 6.dp.toPx())
      )

      // Animated Progress Arc
      drawArc(
        color = ClaudeTerracotta,
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
      )
    }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.padding(16.dp)
    ) {
      Surface(
        shape = CircleShape,
        color = ClaudeTerracotta.copy(alpha = 0.18f),
        modifier = Modifier.size(46.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Animation,
            contentDescription = null,
            tint = ClaudeTerracotta,
            modifier = Modifier.size(24.dp)
          )
        }
      }
      Text(
        text = project.name,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        ),
        color = Color.White
      )
      Text(
        text = "${project.scenes.size} Scenes • ${project.style.displayName}",
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.7f)
      )
    }
  }
}

private fun formatTimeSeconds(sec: Double): String {
  val minutes = (sec / 60).toInt()
  val remainingSeconds = (sec % 60).toInt()
  val millisTenths = ((sec - sec.toInt()) * 10).toInt()
  return String.format(Locale.US, "%02d:%02d.%d", minutes, remainingSeconds, millisTenths)
}
