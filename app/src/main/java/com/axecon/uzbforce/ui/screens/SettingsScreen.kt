package com.axecon.uzbforce.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axecon.uzbforce.model.AppColorProfile
import com.axecon.uzbforce.model.CursorAction
import com.axecon.uzbforce.model.CustomAppShortcut
import com.axecon.uzbforce.model.NavigationBarStyle
import com.axecon.uzbforce.model.ThemeMode
import com.axecon.uzbforce.ui.components.AdaptiveSettingSwitchRow
import com.axecon.uzbforce.ui.viewmodel.AirCursorViewModel
import com.axecon.uzbforce.ui.viewmodel.UiState
import kotlinx.coroutines.launch

data class AppInfoItem(
    val appName: String,
    val packageName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AirCursorViewModel,
    uiState: UiState
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showTipJarDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showAppShortcutsSheet by remember { mutableStateOf(false) }
    var showCustomActionMenuDialog by remember { mutableStateOf(false) }
    var showPowerButtonInfoDialog by remember { mutableStateOf(false) }
    var showPasteBackupDialog by remember { mutableStateOf(false) }
    var pasteBackupText by remember { mutableStateOf("") }

    val backupStatus by viewModel.backupStatus.collectAsState()

    LaunchedEffect(backupStatus) {
        backupStatus?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearBackupStatus()
        }
    }

    // Export .axcn Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(context, it) }
    }

    // Import .axcn Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBackup(context, it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header & Quick Info
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Appearance, navigation, quick shortcuts and app preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Appearance & Theme Section
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionTitle(
                        title = "Appearance & Themes",
                        subtitle = "Choose light or dark mode and bar layout",
                        icon = Icons.Default.Palette
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "App Theme",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = uiState.config.themeMode == mode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.updateConfig { it.copy(themeMode = mode) }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            ThemeMode.SYSTEM -> Icons.Default.Tune
                                            ThemeMode.LIGHT -> Icons.Default.LightMode
                                            ThemeMode.DARK -> Icons.Default.DarkMode
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = when (mode) {
                                            ThemeMode.SYSTEM -> "System"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "App Color Theme",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize the UI accent color across the app (Pink, Green, Purple, etc.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppColorProfile.entries.chunked(3).forEach { rowProfiles ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowProfiles.forEach { profile ->
                                    val isSelected = uiState.config.appColorProfile == profile
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                viewModel.updateConfig { it.copy(appColorProfile = profile) }
                                            }
                                            .testTag("color_profile_${profile.name.lowercase()}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Color(profile.primaryHex)) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(profile.primaryHex))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = when (profile) {
                                                    AppColorProfile.OCEAN_BLUE -> "Blue"
                                                    AppColorProfile.EMERALD_GREEN -> "Green"
                                                    AppColorProfile.NEON_PINK -> "Pink"
                                                    AppColorProfile.CYBER_PURPLE -> "Purple"
                                                    AppColorProfile.SUNSET_AMBER -> "Amber"
                                                    AppColorProfile.SYSTEM_DYNAMIC -> "Dynamic"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Navigation Bar Style",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NavigationBarStyle.entries.forEach { style ->
                            val isSelected = uiState.config.navBarStyle == style
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.updateConfig { it.copy(navBarStyle = style) }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (style == NavigationBarStyle.PILL_IOS) "Floating Pill (iOS)" else "Classic Bottom Bar",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (style == NavigationBarStyle.PILL_IOS) "Draggable elastic pill" else "Standard bottom bar",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Quick Menu & App Shortcuts Customization
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionTitle(
                        title = "Shortcuts & Quick Menu",
                        subtitle = "Customize buttons in the floating action menu and quick launch apps",
                        icon = Icons.Default.Widgets
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCustomActionMenuDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Edit Quick Menu", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = { showAppShortcutsSheet = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Favorite Apps", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 4. Permissions & System Access
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionTitle(
                        title = "App Permissions",
                        subtitle = "System permissions needed for touchless pointer control",
                        icon = Icons.Default.Security
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingsLinkRow(
                        title = "Accessibility Service",
                        subtitle = if (uiState.permissions.isAccessibilityEnabled) "Active and running" else "Required for cursor clicks and button keys",
                        isGranted = uiState.permissions.isAccessibilityEnabled,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open Accessibility Settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsLinkRow(
                        title = "Display Over Other Apps",
                        subtitle = if (uiState.permissions.isOverlayPermissionGranted) "Granted" else "Required to show pointer on screen",
                        isGranted = uiState.permissions.isOverlayPermissionGranted,
                        onClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open Overlay Settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsLinkRow(
                        title = "Battery Optimization",
                        subtitle = "Keeps the pointer service alive in the background",
                        isGranted = true,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open Battery Settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // 5. Hardware Info & Power Button Explanation
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionTitle(
                        title = "Physical Button FAQ",
                        subtitle = "How hardware buttons work with Android accessibility",
                        icon = Icons.Default.HelpOutline
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPowerButtonInfoDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Can Axecon control the physical Power button?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Learn why Android OS restricts power button interception",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Backup, Restore (.axcn) & Reset
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_backup_restore"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Backup & Restore (.axcn)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Export or import all button mappings & tuning",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val timestamp = System.currentTimeMillis()
                                exportLauncher.launch("axecon_backup_$timestamp.axcn")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_export_axcn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export File", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = {
                                importLauncher.launch(arrayOf("*/*", "application/json", "application/octet-stream", "text/*"))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("btn_import_axcn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore File", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val json = viewModel.getBackupJsonString(context)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Axecon Backup", json)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "Full settings backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_copy_backup_clipboard"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Copy JSON", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                pasteBackupText = clipText
                                showPasteBackupDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("btn_paste_backup_restore"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Paste & Restore", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            viewModel.resetAllSettings()
                            Toast.makeText(context, "Reset all settings to default", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset All Settings to Default", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // 7. Developer Feedback, Updates & Support
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SectionTitle(
                        title = "About & Developer",
                        subtitle = "Version v2.4.0 • Built with Kotlin and Compose",
                        icon = Icons.Default.Info
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showFeedbackDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Feedback", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = { showTipJarDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Support", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showUpdateDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check for Updates", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(if (uiState.config.navBarStyle == NavigationBarStyle.PILL_IOS) 96.dp else 24.dp))
        }
    }

    // Modal Dialog: Power Button Explanation
    if (showPowerButtonInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPowerButtonInfoDialog = false },
            title = { Text("Physical Power Button Info", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Why can't Axecon intercept the physical Power button?",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Android OS security strictly prevents third-party apps and Accessibility Services from intercepting the physical Power button (such as long-press or double-press). This is a built-in Android safeguard for Emergency SOS, force restarts, and the hardware power menu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "💡 Alternative: You can trigger the on-screen Power Menu or lock your phone anytime using Volume button combinations or the Quick Action Menu!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showPowerButtonInfoDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }

    // Modal Dialog: Paste Backup & Restore
    if (showPasteBackupDialog) {
        AlertDialog(
            onDismissRequest = { showPasteBackupDialog = false },
            title = { Text("Restore Settings from Text", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Paste your Axecon backup JSON below to restore all profiles, button mappings, custom sequences, and pointer tuning:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pasteBackupText,
                        onValueChange = { pasteBackupText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("Paste .axcn / JSON backup content here...") },
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pasteBackupText.isNotBlank()) {
                            viewModel.restoreFromText(context, pasteBackupText.trim())
                            showPasteBackupDialog = false
                        } else {
                            Toast.makeText(context, "Please paste valid backup text", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dialog: Send Feedback
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text("Send Developer Feedback", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Have an idea, bug report, or suggestion for Axecon? We'd love to hear from you!",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Developer: Azizbek Oktamov\nEmail: oktamovazizbek2010@gmail.com",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:oktamovazizbek2010@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Axecon Feedback (v2.4.0)")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Device: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\n\nFeedback / Suggestion:\n"
                            )
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "Send Feedback via Email"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email client installed", Toast.LENGTH_SHORT).show()
                        }
                        showFeedbackDialog = false
                    }
                ) {
                    Text("Open Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dialog: Tip Jar / Donation
    if (showTipJarDialog) {
        AlertDialog(
            onDismissRequest = { showTipJarDialog = false },
            title = { Text("Support Development", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Axecon is completely free, open, and ad-free. If you enjoy using it, consider supporting future development!",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("$1", "$3", "$5", "$10").forEach { amount ->
                            FilledTonalButton(
                                onClick = {
                                    Toast.makeText(context, "Thank you so much for your support! ❤️", Toast.LENGTH_LONG).show()
                                    showTipJarDialog = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(amount, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTipJarDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal Dialog: Check Updates
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("App Version & Updates", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Axecon v2.4.0", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("You are on the latest stable version", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Recent Improvements:\n• Smooth, lag-free motion sensor telemetry\n• Simplified, human-readable settings\n• Draggable iOS-style elastic pill navigation\n• Physical button sequence recording\n• Customizable quick action menu items",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showUpdateDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Action Menu Customization Dialog
    if (showCustomActionMenuDialog) {
        CustomActionMenuEditorDialog(
            config = uiState.config,
            onDismiss = { showCustomActionMenuDialog = false },
            onSave = { navItems, toolItems ->
                viewModel.updateConfig {
                    it.copy(
                        enabledActionMenuNavItems = navItems,
                        enabledActionMenuToolItems = toolItems
                    )
                }
                showCustomActionMenuDialog = false
            }
        )
    }

    // App Shortcuts Bottom Sheet
    if (showAppShortcutsSheet) {
        AppShortcutsBottomSheet(
            shortcuts = uiState.customShortcuts,
            onDismiss = { showAppShortcutsSheet = false },
            onAddShortcut = { label, pkg ->
                viewModel.addCustomShortcut(
                    CustomAppShortcut(
                        label = label,
                        packageName = pkg,
                        colorArgb = 0xFF2563EB
                    )
                )
            },
            onRemoveShortcut = { id ->
                viewModel.removeCustomShortcut(id)
            }
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isGranted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = if (isGranted) "Enabled" else "Fix",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomActionMenuEditorDialog(
    config: com.axecon.uzbforce.model.CursorConfig,
    onDismiss: () -> Unit,
    onSave: (List<String>, List<String>) -> Unit
) {
    var navItems by remember { mutableStateOf(config.enabledActionMenuNavItems.toSet()) }
    var toolItems by remember { mutableStateOf(config.enabledActionMenuToolItems.toSet()) }

    val allNavOptions = listOf(
        "GLOBAL_BACK" to "Back",
        "GLOBAL_HOME" to "Home",
        "GLOBAL_RECENTS" to "Recent Apps",
        "GLOBAL_NOTIFICATIONS" to "Notifications",
        "GLOBAL_QUICK_SETTINGS" to "Quick Settings",
        "GLOBAL_LOCK_SCREEN" to "Lock Screen",
        "GLOBAL_SCREENSHOT" to "Screenshot",
        "GLOBAL_POWER_DIALOG" to "Power Menu"
    )

    val allToolOptions = listOf(
        "RECENTER_CALIBRATE" to "Recenter Pointer",
        "TOGGLE_PRECISION_MODE" to "Sniper Mode (0.25x)",
        "TOGGLE_TRACKING" to "Freeze / Resume",
        "HOLD_TO_DRAG" to "Hold-to-Drag Mode",
        "DRAG_TOGGLE" to "Toggle Drag Mode",
        "TOGGLE_FLASHLIGHT" to "Flashlight"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Quick Menu", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "System Navigation Buttons",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(allNavOptions) { (key, label) ->
                    val isChecked = navItems.contains(key)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navItems = if (isChecked) navItems - key else navItems + key
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    navItems = if (checked) navItems + key else navItems - key
                                }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cursor Tools",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(allToolOptions) { (key, label) ->
                    val isChecked = toolItems.contains(key)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                toolItems = if (isChecked) toolItems - key else toolItems + key
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    toolItems = if (checked) toolItems + key else toolItems - key
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(navItems.toList(), toolItems.toList()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppShortcutsBottomSheet(
    shortcuts: List<CustomAppShortcut>,
    onDismiss: () -> Unit,
    onAddShortcut: (String, String) -> Unit,
    onRemoveShortcut: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember {
        mutableStateOf(
            try {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
                val resolveInfos = pm.queryIntentActivities(intent, 0)
                resolveInfos.map {
                    AppInfoItem(
                        appName = it.loadLabel(pm).toString(),
                        packageName = it.activityInfo.packageName
                    )
                }.sortedBy { it.appName.lowercase() }
            } catch (e: Exception) {
                listOf(
                    AppInfoItem("Phone", "com.google.android.dialer"),
                    AppInfoItem("Messages", "com.google.android.apps.messaging"),
                    AppInfoItem("Chrome", "com.android.chrome"),
                    AppInfoItem("Settings", "com.android.settings"),
                    AppInfoItem("Camera", "com.google.android.GoogleCamera"),
                    AppInfoItem("YouTube", "com.google.android.youtube"),
                    AppInfoItem("Maps", "com.google.android.apps.maps"),
                    AppInfoItem("Photos", "com.google.android.apps.photos")
                )
            }
        )
    }

    val filteredApps = installedApps.filter {
        searchQuery.isBlank() || it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Favorite Quick Launch Apps",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap to add apps to the Quick Action menu",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Current Shortcuts List
            if (shortcuts.isNotEmpty()) {
                Text(
                    text = "Current Shortcuts (${shortcuts.size}):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    shortcuts.forEach { sc ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sc.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onRemoveShortcut(sc.id) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search installed apps...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredApps) { app ->
                    val isAlreadyAdded = shortcuts.any { it.packageName == app.packageName }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isAlreadyAdded) {
                                    val match = shortcuts.find { it.packageName == app.packageName }
                                    if (match != null) onRemoveShortcut(match.id)
                                } else {
                                    onAddShortcut(app.appName, app.packageName)
                                }
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAlreadyAdded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (isAlreadyAdded) "Added" else "+ Add",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAlreadyAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Done")
            }
        }
    }
}
