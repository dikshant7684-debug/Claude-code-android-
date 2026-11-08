package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeuCodeBg
import com.example.ui.theme.NeuCodeBgSecondary
import com.example.ui.theme.NeuCodeBorder
import com.example.ui.theme.NeuCodeBorderActive
import com.example.ui.theme.NeuCodeElectricViolet
import com.example.ui.theme.NeuCodeError
import com.example.ui.theme.NeuCodeMutedPurple
import com.example.ui.theme.NeuCodePurpleContainer
import com.example.ui.theme.NeuCodePurpleDeep
import com.example.ui.theme.NeuCodePurplePrimary
import com.example.ui.theme.NeuCodeSoftLavender
import com.example.ui.theme.NeuCodeSuccess
import com.example.ui.theme.NeuCodeSurfaceElevated
import com.example.ui.theme.NeuCodeSurfacePrimary
import com.example.ui.theme.NeuCodeSurfaceSecondary
import com.example.ui.theme.NeuCodeTextMuted
import com.example.ui.theme.NeuCodeTextPrimary
import com.example.ui.theme.NeuCodeTextSecondary
import com.example.ui.theme.NeuCodeWarning
import com.example.ui.theme.NeuUserBubble
import com.example.ui.theme.NeuUserBubbleBorder

// =========================================================================
// NEU\CODE COMPONENT ARCHITECTURE
// =========================================================================

/**
 * 1. NeuCodeButton - Primary, Secondary, Outlined, and Ghost states with loading & disabled support.
 */
@Composable
fun NeuCodeButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  isLoading: Boolean = false,
  isSecondary: Boolean = false,
  isOutlined: Boolean = false,
  icon: ImageVector? = null,
  testTag: String = "neucode_button"
) {
  val containerColor = when {
    !enabled -> NeuCodeBgSecondary
    isOutlined -> Color.Transparent
    isSecondary -> NeuCodeSurfaceSecondary
    else -> NeuCodePurplePrimary
  }

  val contentColor = when {
    !enabled -> NeuCodeTextMuted
    isOutlined -> NeuCodeElectricViolet
    isSecondary -> NeuCodeTextPrimary
    else -> Color.White
  }

  val border = when {
    isOutlined -> BorderStroke(1.dp, if (enabled) NeuCodeBorderActive else NeuCodeBorder)
    isSecondary -> BorderStroke(1.dp, NeuCodeBorder)
    else -> null
  }

  Button(
    onClick = { if (!isLoading && enabled) onClick() },
    enabled = enabled,
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor,
      disabledContainerColor = NeuCodeBgSecondary,
      disabledContentColor = NeuCodeTextMuted
    ),
    border = border,
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    modifier = modifier.testTag(testTag)
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        color = contentColor,
        strokeWidth = 2.dp,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
    } else if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(8.dp))
    }
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
      )
    )
  }
}

/**
 * 2. NeuCodeIconButton - 48dp touch-target with hover/press animation
 */
@Composable
fun NeuCodeIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  tint: Color = NeuCodeTextSecondary,
  activeTint: Color = NeuCodeElectricViolet,
  isSelected: Boolean = false,
  testTag: String = "neucode_icon_button"
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val bg by animateColorAsState(
    targetValue = when {
      isPressed -> NeuCodePurpleContainer
      isSelected -> NeuCodeSurfaceSecondary
      else -> Color.Transparent
    },
    animationSpec = tween(150),
    label = "btn_bg"
  )

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bg,
    border = if (isSelected) BorderStroke(1.dp, NeuCodeBorderActive) else null,
    modifier = modifier
      .size(48.dp)
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick
      )
      .testTag(testTag)
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = if (!enabled) NeuCodeTextMuted else if (isSelected) activeTint else tint,
        modifier = Modifier.size(22.dp)
      )
    }
  }
}

/**
 * 3. NeuCodeCard - Charcoal surface card with subtle cosmic border & corner radii
 */
@Composable
fun NeuCodeCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(20.dp),
  isElevated: Boolean = false,
  isActive: Boolean = false,
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit
) {
  val borderCol = if (isActive) NeuCodeBorderActive else NeuCodeBorder
  val bgCol = if (isElevated) NeuCodeSurfaceElevated else NeuCodeSurfacePrimary

  Surface(
    shape = shape,
    color = bgCol,
    border = BorderStroke(1.dp, borderCol),
    modifier = modifier.then(
      if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    )
  ) {
    content()
  }
}

/**
 * 4. NeuCodeDialog - Consistent modal dialog with charcoal container & violet actions
 */
@Composable
fun NeuCodeDialog(
  title: String,
  onDismiss: () -> Unit,
  confirmButtonText: String = "Done",
  onConfirm: () -> Unit = onDismiss,
  dismissButtonText: String? = null,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
        color = NeuCodeTextPrimary
      )
    },
    text = content,
    confirmButton = {
      TextButton(onClick = onConfirm) {
        Text(confirmButtonText, color = NeuCodeElectricViolet, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = dismissButtonText?.let {
      {
        TextButton(onClick = onDismiss) {
          Text(it, color = NeuCodeTextSecondary)
        }
      }
    },
    containerColor = NeuCodeSurfacePrimary,
    shape = RoundedCornerShape(24.dp),
    modifier = modifier
  )
}

/**
 * 5. NeuCodeBottomSheet - Charcoal bottom sheet with drag handle
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuCodeBottomSheet(
  onDismiss: () -> Unit,
  title: String? = null,
  content: @Composable () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    containerColor = NeuCodeSurfacePrimary,
    tonalElevation = 0.dp,
    scrimColor = Color.Black.copy(alpha = 0.65f),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 10.dp, bottom = 4.dp)
          .size(width = 38.dp, height = 4.dp)
          .clip(CircleShape)
          .background(NeuCodeBorderActive)
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      if (title != null) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
          ),
          color = NeuCodeTextPrimary,
          modifier = Modifier.padding(bottom = 14.dp)
        )
      }
      content()
    }
  }
}

/**
 * 6. NeuCodeTextField - High-contrast charcoal text input with subtle violet focus
 */
@Composable
fun NeuCodeTextField(
  value: String,
  onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  placeholder: String? = null,
  isError: Boolean = false,
  errorMessage: String? = null,
  maxLines: Int = 1,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  leadingIcon: @Composable (() -> Unit)? = null,
  trailingIcon: @Composable (() -> Unit)? = null
) {
  Column(modifier = modifier) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = label?.let { { Text(it, color = NeuCodeTextSecondary) } },
      placeholder = placeholder?.let { { Text(it, color = NeuCodeTextMuted) } },
      isError = isError,
      maxLines = maxLines,
      shape = RoundedCornerShape(14.dp),
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      leadingIcon = leadingIcon,
      trailingIcon = trailingIcon,
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = NeuCodeSurfaceSecondary,
        unfocusedContainerColor = NeuCodeSurfacePrimary,
        disabledContainerColor = NeuCodeBgSecondary,
        focusedBorderColor = if (isError) NeuCodeError else NeuCodeBorderActive,
        unfocusedBorderColor = if (isError) NeuCodeError else NeuCodeBorder,
        focusedTextColor = NeuCodeTextPrimary,
        unfocusedTextColor = NeuCodeTextPrimary,
        cursorColor = NeuCodeElectricViolet
      ),
      modifier = Modifier.fillMaxWidth()
    )
    if (isError && !errorMessage.isNullOrBlank()) {
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.labelSmall,
        color = NeuCodeError,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp)
      )
    }
  }
}

/**
 * 7. NeuCodeDropdown - Selection pill with popup menu
 */
@Composable
fun NeuCodeDropdown(
  selectedItem: String,
  items: List<String>,
  onItemSelected: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier) {
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = NeuCodeSurfaceSecondary,
      border = BorderStroke(1.dp, NeuCodeBorder),
      modifier = Modifier.clickable { expanded = true }
    ) {
      Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = selectedItem,
          style = MaterialTheme.typography.labelMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          ),
          color = NeuCodeElectricViolet
        )
        Icon(
          imageVector = Icons.Outlined.KeyboardArrowDown,
          contentDescription = null,
          tint = NeuCodeTextSecondary,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      modifier = Modifier.background(NeuCodeSurfacePrimary).border(1.dp, NeuCodeBorder)
    ) {
      items.forEach { item ->
        DropdownMenuItem(
          text = {
            Text(
              text = item,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
              ),
              color = if (item == selectedItem) NeuCodeElectricViolet else NeuCodeTextPrimary
            )
          },
          onClick = {
            onItemSelected(item)
            expanded = false
          }
        )
      }
    }
  }
}

/**
 * 8. NeuCodeToolbar - Header bar with brand emblem, title, and action buttons
 */
@Composable
fun NeuCodeToolbar(
  title: String,
  onNavigationClick: () -> Unit,
  modifier: Modifier = Modifier,
  navigationIcon: ImageVector? = null,
  actions: @Composable () -> Unit = {}
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(NeuCodeBg)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (navigationIcon != null) {
        NeuCodeIconButton(
          icon = navigationIcon,
          contentDescription = "Navigate",
          onClick = onNavigationClick
        )
      }

      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          letterSpacing = 0.5.sp
        ),
        color = NeuCodeTextPrimary
      )
    }

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      actions()
    }
  }
}

/**
 * 9. NeuCodeNavigation - Consistent pill tab
 */
@Composable
fun NeuCodeNavigationItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val bgCol = if (isSelected) NeuCodePurpleContainer else Color.Transparent
  val tint = if (isSelected) NeuCodeElectricViolet else NeuCodeTextSecondary

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bgCol,
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
          fontSize = 13.sp
        ),
        color = tint
      )
    }
  }
}

/**
 * 10. NeuCodeCodeBlock - Premium monospace syntax container with header and copy button
 */
@Composable
fun NeuCodeCodeBlock(
  code: String,
  language: String = "",
  onCopy: () -> Unit,
  modifier: Modifier = Modifier
) {
  var copied by remember { mutableStateOf(false) }

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = NeuCodeBgSecondary,
    border = BorderStroke(1.dp, NeuCodeBorder),
    modifier = modifier.fillMaxWidth()
  ) {
    Column {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(NeuCodeSurfacePrimary)
          .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = language.ifBlank { "code" },
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          ),
          color = NeuCodeSoftLavender
        )

        NeuCodeIconButton(
          icon = if (copied) Icons.Default.Check else Icons.Outlined.ContentCopy,
          contentDescription = "Copy code",
          onClick = {
            onCopy()
            copied = true
          },
          tint = if (copied) NeuCodeSuccess else NeuCodeTextSecondary,
          modifier = Modifier.size(32.dp)
        )
      }

      Text(
        text = code,
        style = MaterialTheme.typography.bodySmall.copy(
          fontFamily = FontFamily.Monospace,
          fontSize = 12.sp,
          lineHeight = 18.sp
        ),
        color = NeuCodeTextPrimary,
        modifier = Modifier.padding(14.dp)
      )
    }
  }
}

/**
 * 11. NeuCodeToolCard - Card showing tool name, arguments, and execution result
 */
@Composable
fun NeuCodeToolCard(
  toolName: String,
  parameters: String,
  result: String? = null,
  isExecuting: Boolean = false,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = NeuCodeSurfacePrimary,
    border = BorderStroke(1.dp, NeuCodeBorderActive),
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "TOOL • $toolName",
          style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          ),
          color = NeuCodeElectricViolet
        )

        if (isExecuting) {
          CircularProgressIndicator(
            color = NeuCodeElectricViolet,
            strokeWidth = 2.dp,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      if (parameters.isNotBlank()) {
        Text(
          text = parameters,
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
          ),
          color = NeuCodeTextSecondary,
          maxLines = 3
        )
      }

      if (!result.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = NeuCodeBgSecondary,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = result,
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 10.sp
            ),
            color = NeuCodeTextPrimary,
            modifier = Modifier.padding(8.dp),
            maxLines = 4
          )
        }
      }
    }
  }
}

/**
 * 12. NeuCodeProgressIndicator - Nebula purple styled progress
 */
@Composable
fun NeuCodeProgressIndicator(
  progress: Float? = null,
  modifier: Modifier = Modifier
) {
  if (progress != null) {
    androidx.compose.material3.LinearProgressIndicator(
      progress = { progress },
      modifier = modifier
        .fillMaxWidth()
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp)),
      color = NeuCodePurplePrimary,
      trackColor = NeuCodeSurfaceSecondary
    )
  } else {
    androidx.compose.material3.LinearProgressIndicator(
      modifier = modifier
        .fillMaxWidth()
        .height(4.dp)
        .clip(RoundedCornerShape(2.dp)),
      color = NeuCodePurplePrimary,
      trackColor = NeuCodeSurfaceSecondary
    )
  }
}

/**
 * 13. NeuCodeStatusIndicator - Status badge (Success, Warning, Error, Info)
 */
@Composable
fun NeuCodeStatusIndicator(
  label: String,
  status: NeuCoreState,
  modifier: Modifier = Modifier
) {
  val (color, bg) = when (status) {
    NeuCoreState.COMPLETED -> Pair(NeuCodeSuccess, Color(0xFF132A1C))
    NeuCoreState.ERROR -> Pair(NeuCodeError, Color(0xFF331416))
    NeuCoreState.TOOL_EXECUTION -> Pair(NeuCodeWarning, Color(0xFF332612))
    NeuCoreState.SEARCHING -> Pair(NeuCodeSoftLavender, NeuCodePurpleContainer)
    else -> Pair(NeuCodeElectricViolet, NeuCodePurpleContainer)
  }

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = bg,
    border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
    modifier = modifier
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold
      ),
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
    )
  }
}
