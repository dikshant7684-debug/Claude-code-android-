package com.example.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProviderType
import com.example.data.preferences.AppCapabilities
import com.example.data.preferences.ToolPermissionPreferences
import com.example.data.preferences.UserAccountInfo
import com.example.mcp.model.McpAuthType
import com.example.mcp.model.McpServerConfig
import com.example.mcp.model.McpTransportType
import com.example.ui.viewmodel.MainViewModel

// =========================================================================
// NEU\CODE NEBULA PURPLE + CHARCOAL BLACK PALETTE
// =========================================================================
private val DarkBg = com.example.ui.theme.NeuCodeBg // #09090D
private val CardPrimary = com.example.ui.theme.NeuCodeSurfacePrimary // #181720
private val CardSecondary = com.example.ui.theme.NeuCodeSurfaceSecondary // #201D29
private val TextPrimary = com.example.ui.theme.NeuCodeTextPrimary // #F5F3FA
private val TextSecondary = com.example.ui.theme.NeuCodeTextSecondary // #AAA4B8
private val BorderDark = com.example.ui.theme.NeuCodeBorder // #302B3A
private val DividerDark = com.example.ui.theme.NeuCodeBorder // #302B3A
private val AccentWhite = com.example.ui.theme.NeuCodeElectricViolet // #A78BFA
private val AccentPillBg = com.example.ui.theme.NeuCodePurplePrimary // #8B5CF6
private val AccentPillText = Color(0xFFFFFFFF)
private val BadgeBg = com.example.ui.theme.NeuCodePurpleContainer // #251B38
private val BadgeText = com.example.ui.theme.NeuCodeSoftLavender // #C4B5FD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: MainViewModel,
  onOpenDrawer: () -> Unit = {},
  onNavigateBack: () -> Unit = {},
  bottomBar: @Composable () -> Unit = {}
) {
  val context = LocalContext.current
  val userAccount by viewModel.userAccount.collectAsState()
  val appCapabilities by viewModel.appCapabilities.collectAsState()
  val toolPermissions by viewModel.toolPermissions.collectAsState()
  val currentConfig by viewModel.providerConfig.collectAsState()
  val mcpServers by viewModel.mcpServers.collectAsState()
  val editorPrefs by viewModel.editorPreferences.collectAsState()
  val generalPrefs by viewModel.generalPreferences.collectAsState()
  val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
  val hapticEnabled by viewModel.hapticEnabled.collectAsState()

  // Dialog & Sheet states
  var showInfoDialog by remember { mutableStateOf(false) }
  var showProfileDialog by remember { mutableStateOf(false) }
  var showBillingDialog by remember { mutableStateOf(false) }
  var showCapabilitiesDialog by remember { mutableStateOf(false) }
  var showConnectorsDialog by remember { mutableStateOf(false) }
  var showPermissionsDialog by remember { mutableStateOf(false) }
  var showMemoryDialog by remember { mutableStateOf(false) }
  var showAiConfigDialog by remember { mutableStateOf(false) }
  var showResetConfirmDialog by remember { mutableStateOf(false) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBg)
  ) {
    Scaffold(
      containerColor = DarkBg,
      contentColor = TextPrimary,
      contentWindowInsets = WindowInsets.systemBars,
      topBar = {
      // Top Navigation Bar matching design specification
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkBg)
          .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onOpenDrawer,
          modifier = Modifier
            .size(48.dp)
            .testTag("settings_drawer_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = "Open navigation menu",
            tint = TextPrimary,
            modifier = Modifier.size(24.dp)
          )
        }

        Text(
          text = "Settings",
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            letterSpacing = (-0.2).sp
          ),
          color = TextPrimary
        )

        IconButton(
          onClick = { showInfoDialog = true },
          modifier = Modifier
            .size(48.dp)
            .testTag("settings_info_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Application Information",
            tint = TextSecondary,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    },
    bottomBar = bottomBar
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBg)
        .padding(
          top = padding.calculateTopPadding(),
          bottom = padding.calculateBottomPadding()
        ),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 680.dp)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {

        // =====================================================================
        // 1. ACCOUNT INFORMATION CARD
        // =====================================================================
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showProfileDialog = true }
            .testTag("account_info_card")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = userAccount.email,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
              ),
              color = TextPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = 12.dp)
            )

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = BadgeBg,
                border = BorderStroke(1.dp, BorderDark)
              ) {
                Text(
                  text = userAccount.subscriptionPlan,
                  style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                  ),
                  color = if (userAccount.subscriptionPlan.equals("Pro", ignoreCase = true)) AccentWhite else BadgeText,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }

              Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        // =====================================================================
        // 2. SUBSCRIPTION UPGRADE PROMOTIONAL CARD
        // =====================================================================
        val isPro = userAccount.subscriptionPlan.equals("Pro", ignoreCase = true)
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("subscription_upgrade_card")
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = if (isPro) "NEU\\CODE Pro Active" else "Want more NEU\\CODE?",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = (-0.3).sp
                ),
                color = TextPrimary
              )
              Text(
                text = if (isPro)
                  "You have extended reasoning budgets, autonomous multi-agent execution, and unlimited MCP integrations."
                else
                  "Upgrade for elevated execution quotas and high-tier model routing.",
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontSize = 14.sp,
                  lineHeight = 20.sp
                ),
                color = TextSecondary
              )
            }

            Surface(
              shape = RoundedCornerShape(20.dp),
              color = AccentPillBg,
              modifier = Modifier
                .clickable { showBillingDialog = true }
                .testTag("upgrade_button")
            ) {
              Text(
                text = if (isPro) "Manage Plan" else "Upgrade",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold
                ),
                color = AccentPillText,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
              )
            }
          }
        }

        // =====================================================================
        // 3. PROFILE AND BILLING GROUPED SECTION
        // =====================================================================
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            // PROFILE ROW
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showProfileDialog = true }
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .testTag("profile_row"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = CardSecondary,
                  modifier = Modifier.size(40.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Outlined.Person,
                      contentDescription = null,
                      tint = TextPrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Text(
                  text = "Profile",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                  ),
                  color = TextPrimary
                )
              }
              Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
              )
            }

            Divider(color = DividerDark, thickness = 1.dp)

            // BILLING ROW
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showBillingDialog = true }
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .testTag("billing_row"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                Surface(
                  shape = CircleShape,
                  color = CardSecondary,
                  modifier = Modifier.size(40.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = Icons.Outlined.CreditCard,
                      contentDescription = null,
                      tint = TextPrimary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
                Text(
                  text = "Billing",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                  ),
                  color = TextPrimary
                )
              }
              Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        // =====================================================================
        // 4. CAPABILITIES SECTION
        // =====================================================================
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showCapabilitiesDialog = true }
            .testTag("capabilities_row")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = CardSecondary,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Column {
                Text(
                  text = "Capabilities",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                  ),
                  color = TextPrimary
                )
                Text(
                  text = "${appCapabilities.enabledCount()} enabled",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                  ),
                  color = TextSecondary
                )
              }
            }
            Icon(
              imageVector = Icons.Outlined.ChevronRight,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // =====================================================================
        // 5. CONNECTORS SECTION (MCP ARCHITECTURE)
        // =====================================================================
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showConnectorsDialog = true }
            .testTag("connectors_row")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = CardSecondary,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.Hub,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Column {
                Text(
                  text = "Connectors",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                  ),
                  color = TextPrimary
                )
                val activeCount = mcpServers.count { it.isEnabled }
                Text(
                  text = if (activeCount > 0) "$activeCount connected" else "0 connected",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                  ),
                  color = TextSecondary
                )
              }
            }
            Icon(
              imageVector = Icons.Outlined.ChevronRight,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // =====================================================================
        // 6. PERMISSIONS SECTION
        // =====================================================================
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showPermissionsDialog = true }
            .testTag("permissions_row")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = CardSecondary,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Column {
                Text(
                  text = "Permissions",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                  ),
                  color = TextPrimary
                )
                Text(
                  text = "${toolPermissions.grantedCount()} granted",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                  ),
                  color = TextSecondary
                )
              }
            }
            Icon(
              imageVector = Icons.Outlined.ChevronRight,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // =====================================================================
        // 6B. PROJECT MEMORY & CONTEXT MANAGEMENT
        // =====================================================================
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showMemoryDialog = true }
            .testTag("project_memory_row")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = CardSecondary,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.Hub,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Column {
                Text(
                  text = "Project Memory & Context",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                  ),
                  color = TextPrimary
                )
                Text(
                  text = "CLAUDE.md instructions & token compaction",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                  ),
                  color = TextSecondary
                )
              }
            }
            Icon(
              imageVector = Icons.Outlined.ChevronRight,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // =====================================================================
        // 7. AI CONFIGURATION ROW (DEVELOPER OPTION INSIDE)
        // =====================================================================
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAiConfigDialog = true }
            .testTag("ai_configuration_row")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              Surface(
                shape = CircleShape,
                color = CardSecondary,
                modifier = Modifier.size(40.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                  text = "AI Configuration",
                  style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                  ),
                  color = TextPrimary
                )
                Text(
                  text = "${currentConfig.providerType.displayName} • ${currentConfig.modelId}",
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp
                  ),
                  color = TextSecondary,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            Icon(
              imageVector = Icons.Outlined.ChevronRight,
              contentDescription = null,
              tint = TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // =====================================================================
        // 8. APPEARANCE SETTINGS
        // =====================================================================
        SettingsSectionHeader(title = "APPEARANCE")
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Theme Mode
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = TextPrimary
              )
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("DARK" to "Dark", "LIGHT" to "Light", "SYSTEM" to "System").forEach { (id, label) ->
                  val isSelected = generalPrefs.theme == id
                  Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) AccentWhite else CardSecondary,
                    border = BorderStroke(1.dp, if (isSelected) AccentWhite else BorderDark),
                    modifier = Modifier.clickable {
                      viewModel.saveGeneralPreferences(generalPrefs.copy(theme = id))
                      Toast.makeText(context, "$label Theme Selected", Toast.LENGTH_SHORT).show()
                    }
                  ) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) DarkBg else TextSecondary
                      ),
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                  }
                }
              }
            }

            Divider(color = DividerDark)

            // Font Size
            var tempFontSize by remember(editorPrefs) { mutableFloatStateOf(editorPrefs.fontSizeSp.toFloat()) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Code Font Size",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = TextPrimary
                )
                Text(
                  text = "${tempFontSize.toInt()} sp",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = AccentWhite
                )
              }
              Slider(
                value = tempFontSize,
                onValueChange = {
                  tempFontSize = it
                  viewModel.saveEditorPreferences(editorPrefs.copy(fontSizeSp = it.toInt()))
                },
                valueRange = 11f..20f,
                steps = 8,
                colors = SliderDefaults.colors(
                  thumbColor = AccentWhite,
                  activeTrackColor = AccentWhite,
                  inactiveTrackColor = BorderDark
                )
              )
            }

            Divider(color = DividerDark)

            // Line numbers switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Line Numbers", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("Show line index in code viewer", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              Switch(
                checked = editorPrefs.showLineNumbers,
                onCheckedChange = { viewModel.saveEditorPreferences(editorPrefs.copy(showLineNumbers = it)) },
                colors = darkSwitchColors()
              )
            }

            Divider(color = DividerDark)

            // Word wrap switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Word Wrapping", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("Prevent code horizontal overflow", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              Switch(
                checked = editorPrefs.wordWrap,
                onCheckedChange = { viewModel.saveEditorPreferences(editorPrefs.copy(wordWrap = it)) },
                colors = darkSwitchColors()
              )
            }
          }
        }

        // =====================================================================
        // 9. APP PREFERENCES
        // =====================================================================
        SettingsSectionHeader(title = "APP PREFERENCES")
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            // Notifications switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Notifications", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("Automation alerts and task completions", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              Switch(
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                colors = darkSwitchColors()
              )
            }

            Divider(color = DividerDark)

            // Haptic Feedback switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Haptic Feedback", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("Vibration feedback on tap and message send", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              Switch(
                checked = hapticEnabled,
                onCheckedChange = { viewModel.setHapticEnabled(it) },
                colors = darkSwitchColors()
              )
            }

            Divider(color = DividerDark)

            // Code completion switch
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Code Completion Chips", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("AI suggestion chips in code workspace", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              Switch(
                checked = generalPrefs.codeCompletionEnabled,
                onCheckedChange = { viewModel.saveGeneralPreferences(generalPrefs.copy(codeCompletionEnabled = it)) },
                colors = darkSwitchColors()
              )
            }
          }
        }

        // =====================================================================
        // 10. PRIVACY AND STORAGE
        // =====================================================================
        SettingsSectionHeader(title = "PRIVACY & STORAGE")
        var freeMb by remember { mutableStateOf(context.filesDir.freeSpace / (1024 * 1024)) }
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Local Device Storage", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("$freeMb MB available", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardSecondary,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.clickable {
                  viewModel.clearCache { updatedMb ->
                    freeMb = updatedMb
                    Toast.makeText(context, "Cache Cleared. $updatedMb MB Free", Toast.LENGTH_SHORT).show()
                  }
                }
              ) {
                Text(
                  text = "Clear Cache",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }

            Divider(color = DividerDark)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Conversation History", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("Clear active messages on device", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardSecondary,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.clickable {
                  viewModel.clearChat()
                  Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                }
              ) {
                Text(
                  text = "Clear Chat",
                  style = MaterialTheme.typography.labelSmall.copy(color = TextPrimary),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }

            Divider(color = DividerDark)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Reset All Settings", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFEF5350))
                Text("Restore credentials & preferences to defaults", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF3B1E1E),
                border = BorderStroke(1.dp, Color(0xFFEF5350)),
                modifier = Modifier.clickable { showResetConfirmDialog = true }
              ) {
                Text(
                  text = "Reset",
                  style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEF5350), fontWeight = FontWeight.Bold),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
              }
            }
          }
        }

        // =====================================================================
        // 11. ABOUT
        // =====================================================================
        SettingsSectionHeader(title = "ABOUT")
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = CardPrimary,
          border = BorderStroke(1.dp, BorderDark),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Application Version", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
              Text("NEU\\CODE v1.0.0", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Divider(color = DividerDark)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("OS Architecture", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
              Text("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Divider(color = DividerDark)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Local Privacy Sandboxing", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
              Icon(Icons.Outlined.Security, contentDescription = null, tint = AccentWhite, modifier = Modifier.size(18.dp))
            }
          }
        }

        Spacer(modifier = Modifier.height(32.dp))
      }
    }
  }
  }

  // =========================================================================
  // SUB-DIALOGS & INTERACTIVE SHEETS
  // =========================================================================

  // 1. PROFILE DIALOG
  if (showProfileDialog) {
    var emailInput by remember { mutableStateOf(userAccount.email) }
    var nameInput by remember { mutableStateOf(userAccount.displayName) }
    AlertDialog(
      onDismissRequest = { showProfileDialog = false },
      title = { Text("Profile Information", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Display Name", color = TextSecondary) },
            shape = RoundedCornerShape(12.dp),
            colors = darkTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email Address", color = TextSecondary) },
            shape = RoundedCornerShape(12.dp),
            colors = darkTextFieldColors(),
            modifier = Modifier.fillMaxWidth()
          )
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardSecondary,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Subscription Tier", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
              Text(userAccount.subscriptionPlan, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = AccentWhite)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.saveUserAccount(userAccount.copy(email = emailInput.trim(), displayName = nameInput.trim()))
          showProfileDialog = false
          Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
        }) {
          Text("Save", color = AccentWhite, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showProfileDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = CardPrimary,
      shape = RoundedCornerShape(24.dp)
    )
  }

  // 2. BILLING & SUBSCRIPTION UPGRADE DIALOG
  if (showBillingDialog) {
    AlertDialog(
      onDismissRequest = { showBillingDialog = false },
      title = { Text("Claude Subscriptions", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          // Free Plan
          val isFree = userAccount.subscriptionPlan.equals("Free", ignoreCase = true)
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isFree) CardSecondary else DarkBg,
            border = BorderStroke(1.dp, if (isFree) AccentWhite else BorderDark),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                viewModel.saveUserAccount(userAccount.copy(subscriptionPlan = "Free"))
                Toast.makeText(context, "Switched to Free plan", Toast.LENGTH_SHORT).show()
              }
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Claude Free", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Text("Standard agent reasoning and file editing", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              if (isFree) Icon(Icons.Outlined.Check, contentDescription = null, tint = AccentWhite)
            }
          }

          // Pro Plan
          val isProActive = userAccount.subscriptionPlan.equals("Pro", ignoreCase = true)
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isProActive) CardSecondary else DarkBg,
            border = BorderStroke(1.dp, if (isProActive) AccentWhite else BorderDark),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                viewModel.saveUserAccount(userAccount.copy(subscriptionPlan = "Pro"))
                Toast.makeText(context, "Subscribed to Claude Pro!", Toast.LENGTH_SHORT).show()
              }
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Claude Pro", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                Text("Extended thinking, unlimited MCP, priority speed", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              }
              if (isProActive) Icon(Icons.Outlined.Check, contentDescription = null, tint = AccentWhite)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showBillingDialog = false }) {
          Text("Done", color = AccentWhite, fontWeight = FontWeight.Bold)
        }
      },
      containerColor = CardPrimary,
      shape = RoundedCornerShape(24.dp)
    )
  }

  // 3. CAPABILITIES DIALOG
  if (showCapabilitiesDialog) {
    var caps by remember(appCapabilities) { mutableStateOf(appCapabilities) }
    AlertDialog(
      onDismissRequest = {
        viewModel.saveAppCapabilities(caps)
        showCapabilitiesDialog = false
      },
      title = { Text("AI Capabilities", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          CapabilityToggleItem("Code Generation & Execution", "Generate and run Kotlin, Java, and shell commands", caps.codeGeneration) {
            caps = caps.copy(codeGeneration = it)
            viewModel.saveAppCapabilities(caps)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Autonomous File Editing", "Apply search-and-replace patches to codebase", caps.fileEditing) {
            caps = caps.copy(fileEditing = it)
            viewModel.saveAppCapabilities(caps)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Claude Motion Animations", "Generate 1:1 motion graphics and animation previews", caps.claudeMotion) {
            caps = caps.copy(claudeMotion = it)
            viewModel.saveAppCapabilities(caps)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Project Diagnostics", "Syntax checking and structural code verification", caps.diagnostics) {
            caps = caps.copy(diagnostics = it)
            viewModel.saveAppCapabilities(caps)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Multi-format Tool Calling", "Parse JSON and XML tool calls from agent outputs", caps.toolCalling) {
            caps = caps.copy(toolCalling = it)
            viewModel.saveAppCapabilities(caps)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Automations & Webhooks", "Trigger background webhooks and event rules", caps.automations) {
            caps = caps.copy(automations = it)
            viewModel.saveAppCapabilities(caps)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.saveAppCapabilities(caps)
          showCapabilitiesDialog = false
          Toast.makeText(context, "Capabilities saved", Toast.LENGTH_SHORT).show()
        }) {
          Text("Done", color = AccentWhite, fontWeight = FontWeight.Bold)
        }
      },
      containerColor = CardPrimary,
      shape = RoundedCornerShape(24.dp)
    )
  }

  // 4. CONNECTORS / MCP DIALOG
  if (showConnectorsDialog) {
    var showAddServerDialog by remember { mutableStateOf(false) }
    AlertDialog(
      onDismissRequest = { showConnectorsDialog = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("MCP Connectors", color = TextPrimary, fontWeight = FontWeight.Bold)
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = AccentWhite,
            modifier = Modifier.clickable { showAddServerDialog = true }
          ) {
            Text(
              text = "+ Add",
              style = MaterialTheme.typography.labelSmall.copy(color = DarkBg, fontWeight = FontWeight.Bold),
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (mcpServers.isEmpty()) {
            Text(
              text = "No MCP servers configured yet. Tap '+ Add' to connect external tool integrations.",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
          } else {
            mcpServers.forEach { server ->
              Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardSecondary,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(server.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text("${server.transportType.name} • ${server.url}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                  }
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                      checked = server.isEnabled,
                      onCheckedChange = { isChecked ->
                        viewModel.addOrUpdateMcpServer(server.copy(isEnabled = isChecked))
                      },
                      colors = darkSwitchColors()
                    )
                    IconButton(onClick = { viewModel.deleteMcpServer(server.id) }) {
                      Icon(Icons.Outlined.Delete, contentDescription = "Delete server", tint = Color(0xFFEF5350), modifier = Modifier.size(18.dp))
                    }
                  }
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showConnectorsDialog = false }) {
          Text("Close", color = AccentWhite, fontWeight = FontWeight.Bold)
        }
      },
      containerColor = CardPrimary,
      shape = RoundedCornerShape(24.dp)
    )

    if (showAddServerDialog) {
      var serverName by remember { mutableStateOf("") }
      var serverUrl by remember { mutableStateOf("") }
      var selectedTransport by remember { mutableStateOf(McpTransportType.STREAMABLE_HTTP) }
      AlertDialog(
        onDismissRequest = { showAddServerDialog = false },
        title = { Text("New MCP Server", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = serverName,
              onValueChange = { serverName = it },
              label = { Text("Server Name (e.g. GitHub Tools)", color = TextSecondary) },
              colors = darkTextFieldColors(),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
              value = serverUrl,
              onValueChange = { serverUrl = it },
              label = { Text("Endpoint URL", color = TextSecondary) },
              placeholder = { Text("https://mcp.example.com/api") },
              colors = darkTextFieldColors(),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          TextButton(
            onClick = {
              if (serverName.isNotBlank() && serverUrl.isNotBlank()) {
                val newConfig = McpServerConfig(
                  id = "server_${System.currentTimeMillis()}",
                  name = serverName.trim(),
                  url = serverUrl.trim(),
                  transportType = selectedTransport,
                  authType = McpAuthType.NONE,
                  isEnabled = true,
                  isApprovedByUser = true
                )
                viewModel.addOrUpdateMcpServer(newConfig)
                showAddServerDialog = false
                Toast.makeText(context, "Server added", Toast.LENGTH_SHORT).show()
              }
            }
          ) {
            Text("Save Server", color = AccentWhite, fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showAddServerDialog = false }) {
            Text("Cancel", color = TextSecondary)
          }
        },
        containerColor = CardPrimary,
        shape = RoundedCornerShape(24.dp)
      )
    }
  }

  // 5. PERMISSIONS DIALOG
  if (showPermissionsDialog) {
    var perms by remember(toolPermissions) { mutableStateOf(toolPermissions) }
    AlertDialog(
      onDismissRequest = {
        viewModel.saveToolPermissions(perms)
        showPermissionsDialog = false
      },
      title = { Text("Agent Permissions", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          CapabilityToggleItem("Read File Access", "Allow agent to inspect project source code", perms.readFileAllowed) {
            perms = perms.copy(readFileAllowed = it)
            viewModel.saveToolPermissions(perms)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Review File Writes", "Prompt before overwriting or modifying files", perms.writeFileRequireReview) {
            perms = perms.copy(writeFileRequireReview = it)
            viewModel.saveToolPermissions(perms)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Review Patch Diffs", "Prompt before applying search-replace code diffs", perms.applyPatchRequireReview) {
            perms = perms.copy(applyPatchRequireReview = it)
            viewModel.saveToolPermissions(perms)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Inspect Workspace", "Allow agent to list project directories & structures", perms.inspectWorkspaceAllowed) {
            perms = perms.copy(inspectWorkspaceAllowed = it)
            viewModel.saveToolPermissions(perms)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Terminal Execution", "Allow running background testing & diagnostics", perms.terminalExecutionAllowed) {
            perms = perms.copy(terminalExecutionAllowed = it)
            viewModel.saveToolPermissions(perms)
          }
          Divider(color = DividerDark)
          CapabilityToggleItem("Outbound Network Access", "Allow connecting to MCP endpoints & webhooks", perms.networkAccessAllowed) {
            perms = perms.copy(networkAccessAllowed = it)
            viewModel.saveToolPermissions(perms)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = {
          viewModel.saveToolPermissions(perms)
          showPermissionsDialog = false
          Toast.makeText(context, "Permissions updated", Toast.LENGTH_SHORT).show()
        }) {
          Text("Done", color = AccentWhite, fontWeight = FontWeight.Bold)
        }
      },
      containerColor = CardPrimary,
      shape = RoundedCornerShape(24.dp)
    )
  }

  // 5B. PROJECT MEMORY & CONTEXT DIALOG
  if (showMemoryDialog) {
    val report = com.example.context.ContextUsageTracker.getInstance().latestReport
    val project = viewModel.selectedProject.collectAsState().value
    val projectId = project?.id ?: "Workspace"
    val memoryMgr = com.example.context.ProjectMemoryManager.getInstance(context)
    val memories = remember(projectId) { memoryMgr.getAllProjectMemories(projectId) }

    AlertDialog(
      onDismissRequest = { showMemoryDialog = false },
      title = {
        Text("Project Memory & Context", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Context Window Monitor Card
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = CardSecondary,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "TOKEN CONTEXT WINDOW",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                color = com.example.ui.theme.NeuCodeElectricViolet
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = report?.formatSummary() ?: "No active query report yet. Models will track capacity upon first prompt execution.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                color = TextSecondary
              )
            }
          }

          // Persistent Memories Count & Info
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = CardSecondary,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "PERSISTENT MEMORY RECORDS (${memories.size})",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
                color = com.example.ui.theme.NeuCodeElectricViolet
              )
              Spacer(modifier = Modifier.height(6.dp))
              if (memories.isEmpty()) {
                Text(
                  text = "No stored memory records yet. Memory records automatically build as you work on this project (architecture, decisions, and conventions).",
                  style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                  color = TextSecondary
                )
              } else {
                memories.take(6).forEach { m ->
                  Text(
                    text = "• [${m.category}] ${m.key}: ${m.content.take(80)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextPrimary,
                    modifier = Modifier.padding(vertical = 2.dp)
                  )
                }
              }
            }
          }

          // Actions: Manual Compaction & Clear
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = {
                viewModel.sendChatMessage("/compact")
                showMemoryDialog = false
                Toast.makeText(context, "Manual context compaction triggered", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Compact Context", fontSize = 11.sp, color = AccentWhite)
            }
            OutlinedButton(
              onClick = {
                memoryMgr.clearProjectMemories(projectId)
                showMemoryDialog = false
                Toast.makeText(context, "Project memories cleared", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Clear Memory", fontSize = 11.sp, color = com.example.ui.theme.NeuCodeError)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showMemoryDialog = false }) {
          Text("Done", color = AccentWhite, fontWeight = FontWeight.Bold)
        }
      },
      containerColor = CardPrimary,
      shape = RoundedCornerShape(24.dp)
    )
  }

  // 6. AI CONFIGURATION MODAL (PRESERVING ALL CONFIGURATIONS)
  if (showAiConfigDialog) {
    var providerType by remember(currentConfig) { mutableStateOf(currentConfig.providerType) }
    var baseUrl by remember(currentConfig) { mutableStateOf(currentConfig.baseUrl) }
    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var modelId by remember(currentConfig) { mutableStateOf(currentConfig.modelId) }
    var temperature by remember(currentConfig) { mutableFloatStateOf(currentConfig.temperature) }
    var maxSteps by remember(currentConfig) { mutableFloatStateOf(currentConfig.maxAgentSteps.toFloat()) }
    var showApiKey by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val fetchedModels by viewModel.fetchedModels.collectAsState()
    val isFetchingModels by viewModel.isFetchingModels.collectAsState()
    val modelsFetchStatus by viewModel.modelsFetchStatus.collectAsState()

    AlertDialog(
      onDismissRequest = { showAiConfigDialog = false },
      title = { Text("AI Provider & Credentials", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Provider Dropdown
          ExposedDropdownMenuBox(
            expanded = providerExpanded,
            onExpandedChange = { providerExpanded = !providerExpanded }
          ) {
            OutlinedTextField(
              value = providerType.displayName,
              onValueChange = {},
              readOnly = true,
              label = { Text("Provider Protocol", color = TextSecondary) },
              trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.menuAnchor().fillMaxWidth(),
              colors = darkTextFieldColors()
            )
            ExposedDropdownMenu(
              expanded = providerExpanded,
              onDismissRequest = { providerExpanded = false },
              modifier = Modifier.background(CardSecondary)
            ) {
              ProviderType.values().forEach { type ->
                DropdownMenuItem(
                  text = { Text(type.displayName, color = TextPrimary) },
                  onClick = {
                    providerType = type
                    baseUrl = type.defaultBaseUrl
                    modelId = type.defaultModel
                    providerExpanded = false
                  }
                )
              }
            }
          }

          // Base URL
          OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("API Endpoint URL (Base URL)", color = TextSecondary) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = darkTextFieldColors()
          )

          // Quick Presets
          LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
              DarkModelPill("Anthropic") {
                baseUrl = "https://api.anthropic.com/v1"
                providerType = ProviderType.ANTHROPIC
                modelId = "claude-3-7-sonnet-20250219"
              }
            }
            item {
              DarkModelPill("OpenAI (/v1)") {
                baseUrl = "https://api.openai.com/v1"
                providerType = ProviderType.OPENAI
                modelId = "gpt-4o"
              }
            }
            item {
              DarkModelPill("OpenRouter") {
                baseUrl = "https://openrouter.ai/api/v1"
                providerType = ProviderType.OPENAI
                modelId = "anthropic/claude-3.7-sonnet"
              }
            }
            item {
              DarkModelPill("Groq") {
                baseUrl = "https://api.groq.com/openai/v1"
                providerType = ProviderType.OPENAI
                modelId = "llama-3.3-70b-versatile"
              }
            }
            item {
              DarkModelPill("Ollama") {
                baseUrl = "http://10.0.2.2:11434/v1"
                providerType = ProviderType.CUSTOM
                modelId = "llama3.2"
              }
            }
          }

          // API Key
          OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key", color = TextSecondary) },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
              IconButton(onClick = { showApiKey = !showApiKey }) {
                Icon(
                  if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                  contentDescription = null,
                  tint = TextSecondary
                )
              }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = darkTextFieldColors()
          )

          // Fetch Models Button
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardSecondary,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier
              .fillMaxWidth()
              .clickable(enabled = !isFetchingModels && baseUrl.isNotBlank()) {
                val updated = currentConfig.copy(
                  providerType = providerType,
                  baseUrl = baseUrl.trim(),
                  apiKey = apiKey.trim(),
                  modelId = modelId.trim()
                )
                viewModel.saveProviderConfig(updated)
                viewModel.fetchModelsList()
              }
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (isFetchingModels) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentWhite)
              } else {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = AccentWhite, modifier = Modifier.size(16.dp))
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text("Fetch Models from Endpoint", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = AccentWhite)
            }
          }

          if (modelsFetchStatus != null) {
            Text(
              text = modelsFetchStatus!!,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
              color = if (modelsFetchStatus!!.startsWith("✓")) Color(0xFF81C784) else Color(0xFFEF5350)
            )
          }

          // Model ID
          OutlinedTextField(
            value = modelId,
            onValueChange = { modelId = it },
            label = { Text("Active Model ID", color = TextSecondary) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = darkTextFieldColors()
          )

          // Sliders
          Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Max Reasoning Steps", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
              Text("${maxSteps.toInt()}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AccentWhite)
            }
            Slider(
              value = maxSteps,
              onValueChange = { maxSteps = it },
              valueRange = 1f..20f,
              steps = 18,
              colors = SliderDefaults.colors(thumbColor = AccentWhite, activeTrackColor = AccentWhite, inactiveTrackColor = BorderDark)
            )
          }

          Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Temperature", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
              Text(String.format(java.util.Locale.US, "%.2f", temperature), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = AccentWhite)
            }
            Slider(
              value = temperature,
              onValueChange = { temperature = it },
              valueRange = 0.0f..1.0f,
              steps = 10,
              colors = SliderDefaults.colors(thumbColor = AccentWhite, activeTrackColor = AccentWhite, inactiveTrackColor = BorderDark)
            )
          }

          // Test Connection Button
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = CardSecondary,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier
              .fillMaxWidth()
              .clickable {
                val updated = currentConfig.copy(
                  providerType = providerType,
                  baseUrl = baseUrl.trim(),
                  apiKey = apiKey.trim(),
                  modelId = modelId.trim(),
                  temperature = temperature,
                  maxAgentSteps = maxSteps.toInt()
                )
                viewModel.saveProviderConfig(updated)
                viewModel.testConnection()
              }
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Outlined.NetworkCheck, contentDescription = null, tint = AccentWhite, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Test Connection Latency", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
            }
          }

          if (connectionStatus != null) {
            Text(
              text = connectionStatus!!,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
              color = if (connectionStatus!!.contains("successful")) Color(0xFF81C784) else Color(0xFFEF5350)
            )
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            val updated = currentConfig.copy(
              providerType = providerType,
              baseUrl = baseUrl.trim(),
              apiKey = apiKey.trim(),
              modelId = modelId.trim(),
              temperature = temperature,
              maxAgentSteps = maxSteps.toInt()
            )
            viewModel.saveProviderConfig(updated)
            showAiConfigDialog = false
            Toast.makeText(context, "AI credentials saved!", Toast.LENGTH_SHORT).show()
          }
        ) {
          Text("Save & Apply", color = AccentWhite, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAiConfigDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = CardPrimary,
      shape = RoundedCornerShape(24.dp)
    )
  }

  // 7. APP INFO DIALOG
  if (showInfoDialog) {
    AlertDialog(
      onDismissRequest = { showInfoDialog = false },
      title = { Text("About NEU\\CODE", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "NEU\\CODE is an advanced AI-powered developer environment and autonomous engineering system on Android. It interfaces directly with frontier neural models, executes native MCP tools, and orchestrates tasks across your local codebase.",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = TextSecondary
          )
          Divider(color = DividerDark)
          Text("Security & Privacy", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
          Text(
            text = "Your files and workspace remain strictly local to your device. API credentials are encrypted with Android Keystore and isolated DataStore.",
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
            color = TextSecondary
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showInfoDialog = false }) {
          Text("Close", color = AccentWhite, fontWeight = FontWeight.Bold)
        }
      },
      containerColor = CardPrimary,
      shape = RoundedCornerShape(24.dp)
    )
  }

  // 8. RESET CONFIRMATION DIALOG
  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      title = { Text("Reset All Settings?", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "This will reset all AI provider configurations, editor settings, and preferences to defaults. This action cannot be undone.",
          color = TextSecondary,
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.resetAllSettings()
            showResetConfirmDialog = false
            Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
          }
        ) {
          Text("Reset", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetConfirmDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      },
      containerColor = CardPrimary,
      shape = RoundedCornerShape(24.dp)
    )
  }
}

// =========================================================================
// HELPER UI COMPOSABLES
// =========================================================================

@Composable
private fun SettingsSectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelSmall.copy(
      fontWeight = FontWeight.SemiBold,
      fontSize = 11.sp,
      letterSpacing = 1.sp
    ),
    color = TextSecondary,
    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
  )
}

@Composable
private fun CapabilityToggleItem(
  title: String,
  description: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
      Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = darkSwitchColors()
    )
  }
}

@Composable
private fun DarkModelPill(name: String, onClick: () -> Unit) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = CardSecondary,
    border = BorderStroke(1.dp, BorderDark),
    modifier = Modifier.clickable { onClick() }
  ) {
    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      ),
      color = AccentWhite,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
    )
  }
}

@Composable
private fun darkSwitchColors() = SwitchDefaults.colors(
  checkedThumbColor = DarkBg,
  checkedTrackColor = AccentWhite,
  uncheckedThumbColor = TextSecondary,
  uncheckedTrackColor = CardSecondary,
  uncheckedBorderColor = BorderDark
)

@Composable
private fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
  focusedBorderColor = AccentWhite,
  unfocusedBorderColor = BorderDark,
  focusedTextColor = TextPrimary,
  unfocusedTextColor = TextPrimary,
  focusedContainerColor = CardSecondary,
  unfocusedContainerColor = CardSecondary,
  cursorColor = AccentWhite
)
