package com.example.ui.screens.artifacts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.motion.model.MotionProject
import com.example.motion.viewmodel.ClaudeMotionViewModel
import com.example.ui.theme.ClaudeTerracotta
import com.example.ui.theme.DroidOrange
import com.example.ui.theme.DroidOrangeDark

/**
 * Artifact category items for non-motion artifacts.
 */
data class StaticArtifactItem(
  val id: String,
  val title: String,
  val category: String,
  val description: String,
  val icon: ImageVector,
  val timestamp: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArtifactsScreen(
  motionViewModel: ClaudeMotionViewModel,
  onNavigateToClaudeMotion: () -> Unit,
  onOpenDrawer: () -> Unit,
  modifier: Modifier = Modifier
) {
  val motionProjects by motionViewModel.projects.collectAsState()
  var searchQuery by remember { mutableStateOf("") }

  val sampleArtifacts = remember {
    listOf(
      StaticArtifactItem(
        id = "art-1",
        title = "Responsive Navigation Drawer",
        category = "UI Component",
        description = "Jetpack Compose adaptive navigation rail & modal drawer layout.",
        icon = Icons.Default.Dashboard,
        timestamp = "Today"
      ),
      StaticArtifactItem(
        id = "art-2",
        title = "Room Database Architecture",
        category = "Architecture Spec",
        description = "Schema definitions, DAO methods, and reactive Flow pipelines.",
        icon = Icons.Default.Code,
        timestamp = "Yesterday"
      ),
      StaticArtifactItem(
        id = "art-3",
        title = "Fast Fourier Transform Visualizer",
        category = "Algorithm",
        description = "Interactive audio spectrum visualizer using Kotlin Coroutines.",
        icon = Icons.Default.Widgets,
        timestamp = "Oct 24"
      )
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Artifacts",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.testTag("artifacts_drawer_button")
          ) {
            Icon(
              imageVector = Icons.Default.Menu,
              contentDescription = "Open navigation menu",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // 1. Search Bar
      item {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              text = "Search artifacts, animations, components...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Search",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClaudeTerracotta,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("artifacts_search_field")
        )
      }

      // 2. FEATURED HERO: CLAUDE MOTION (Phase 1 Requirement)
      item {
        ClaudeMotionHeroCard(
          projectCount = motionProjects.size,
          onOpenClaudeMotion = onNavigateToClaudeMotion
        )
      }

      // 3. RECENT MOTION PROJECTS (Horizontal Gallery)
      if (motionProjects.isNotEmpty()) {
        item {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Motion Graphics Projects",
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Open Editor",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = ClaudeTerracotta
              ),
              modifier = Modifier
                .clickable { onNavigateToClaudeMotion() }
                .padding(4.dp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            items(motionProjects, key = { it.id }) { project ->
              MotionProjectThumbnailCard(
                project = project,
                onClick = {
                  motionViewModel.selectProject(project)
                  onNavigateToClaudeMotion()
                }
              )
            }
          }
        }
      }

      // 4. CODE & UI ARTIFACTS SECTION
      item {
        Text(
          text = "Code & Design Artifacts",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
      }

      items(sampleArtifacts, key = { it.id }) { artifact ->
        StaticArtifactCard(artifact = artifact)
      }
    }
  }
}

/**
 * Featured Hero Entry for Claude Motion inside Artifacts.
 */
@Composable
fun ClaudeMotionHeroCard(
  projectCount: Int,
  onOpenClaudeMotion: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    onClick = onOpenClaudeMotion,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, ClaudeTerracotta.copy(alpha = 0.35f)),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("claude_motion_hero_entry")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              ClaudeTerracotta.copy(alpha = 0.08f),
              Color.Transparent
            )
          )
        )
        .padding(18.dp)
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Badge & Icon Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Animated Terracotta Icon
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = ClaudeTerracotta.copy(alpha = 0.15f),
            modifier = Modifier.size(46.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.MovieFilter,
                contentDescription = "Claude Motion Icon",
                tint = ClaudeTerracotta,
                modifier = Modifier.size(26.dp)
              )
            }
          }

          // Category Badge
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = ClaudeTerracotta.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, ClaudeTerracotta.copy(alpha = 0.3f))
          ) {
            Text(
              text = "AI MOTION ENGINE",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
              ),
              color = ClaudeTerracotta,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }

        // Title and Description
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "NEU\\CODE Motion",
            style = MaterialTheme.typography.headlineSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 21.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Create, animate, edit, and export motion graphics with AI.",
            style = MaterialTheme.typography.bodyMedium.copy(
              fontSize = 14.sp,
              lineHeight = 20.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        // Feature Tags
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          MotionFeaturePill("Canvas & SVG")
          MotionFeaturePill("AI Scene Plan")
          MotionFeaturePill("60fps Scrubber")
          MotionFeaturePill("Video Export")
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Launch Action Button
        Button(
          onClick = onOpenClaudeMotion,
          colors = ButtonDefaults.buttonColors(
            containerColor = ClaudeTerracotta,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("launch_claude_motion_button")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Animation,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (projectCount > 0) "Open Motion Workspace ($projectCount)" else "Launch Motion Studio",
              style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
fun MotionFeaturePill(text: String) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
    )
  }
}

/**
 * Project thumbnail card for the horizontal gallery in Artifacts.
 */
@Composable
fun MotionProjectThumbnailCard(
  project: MotionProject,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    modifier = modifier
      .width(190.dp)
      .clickable { onClick() }
  ) {
    Column {
      // Mock / Preview Canvas Block
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(105.dp)
          .background(
            Brush.linearGradient(
              colors = listOf(
                Color(0xFF161924),
                Color(0xFF241512)
              )
            )
          ),
        contentAlignment = Alignment.Center
      ) {
        Surface(
          shape = CircleShape,
          color = ClaudeTerracotta.copy(alpha = 0.85f),
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Play Preview",
              tint = Color.White,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // Aspect ratio chip in top right
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = Color.Black.copy(alpha = 0.65f),
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
        ) {
          Text(
            text = project.aspectRatio.label.substringBefore(" "),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Color.White),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }
      }

      // Details
      Column(
        modifier = Modifier.padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
      ) {
        Text(
          text = project.name,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
          ),
          maxLines = 1,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "${project.style.displayName} • ${project.durationSeconds.toInt()}s",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

/**
 * Standard artifact card for static items (Code, UI, Architecture).
 */
@Composable
fun StaticArtifactCard(
  artifact: StaticArtifactItem,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = artifact.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        Text(
          text = artifact.title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = artifact.description,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          maxLines = 2,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
