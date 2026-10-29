package com.moviebox.app.ui.user

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moviebox.app.BuildConfig
import com.moviebox.app.R
import com.moviebox.app.data.model.AppPreferences
import com.moviebox.app.data.model.ThemeVariant
import com.moviebox.app.data.model.UserPresenceRecord
import com.moviebox.app.data.model.VipAccessConfig
import com.moviebox.app.data.repository.UserPresenceManager
import com.moviebox.app.security.NhNativeSecurityVault
import com.moviebox.app.ui.theme.CrimsonLight
import com.moviebox.app.ui.theme.CrimsonRed
import com.moviebox.app.ui.theme.StarGold
import com.moviebox.app.ui.theme.StatusBlue
import com.moviebox.app.ui.theme.StatusGreen
import com.moviebox.app.ui.theme.TextSecondary
import com.moviebox.app.ui.theme.TextWhite

@Composable
fun UserSettingsScreen(
    preferences: AppPreferences,
    userRecord: UserPresenceRecord = UserPresenceRecord(),
    vipConfig: VipAccessConfig = VipAccessConfig(),
    onSelectTheme: (ThemeVariant) -> Unit,
    onSelectQuality: (String) -> Unit,
    onToggleAutoPlay: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onRedeemShareCode: (String) -> Unit = {},
    onOpenOfflineDownloads: () -> Unit = {}
) {
    val context = LocalContext.current
    var qualityMenu by remember { mutableStateOf(false) }
    val qualities = listOf("Auto", "HD (1080p)", "HD (720p)", "SD (480p)")

    var redeemCodeInput by remember { mutableStateOf("") }
    var redeemStatusNote by remember { mutableStateOf<String?>(null) }

    val resolvedDeviceId = remember(userRecord.deviceId, context) {
        userRecord.deviceId.ifBlank { UserPresenceManager.getRealDeviceId(context) }
    }
    val resolvedShareCode = remember(userRecord.shareCode, resolvedDeviceId, context) {
        userRecord.shareCode.ifBlank { UserPresenceManager.getFiveDigitShareCode(context) }
    }
    val requiredVipPoints = vipConfig.vipPointsToAccess.coerceAtLeast(1)
    val isVipUnlocked = userRecord.hasGlobalVip(requiredVipPoints)
    val progressFraction = (userRecord.points.toFloat() / requiredVipPoints.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Profile",
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )

        // =========================================================================
        // 1. VIP POINTS, 5-DIGIT SHARE CODE & REDEEM CODE CARD (Clean Mobile Layout)
        // =========================================================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1C1726),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(
                    width = 1.2.dp,
                    color = if (isVipUnlocked) StarGold.copy(alpha = 0.75f) else CrimsonRed.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Crown + VIP Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isVipUnlocked) StarGold.copy(alpha = 0.22f) else CrimsonRed.copy(alpha = 0.2f))
                            .border(1.dp, if (isVipUnlocked) StarGold else CrimsonRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = if (isVipUnlocked) StarGold else CrimsonLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isVipUnlocked) "VIP Access Active" else "VIP Points & Share Code",
                            color = TextWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (isVipUnlocked) {
                                "All VIP Movies & Web Series are unlocked!"
                            } else {
                                "Redeem a 5-digit Share Code to earn points & unlock VIP"
                            },
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = if (isVipUnlocked) StarGold.copy(alpha = 0.2f) else CrimsonRed.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, if (isVipUnlocked) StarGold else CrimsonLight)
                ) {
                    Text(
                        text = if (isVipUnlocked) "👑 VIP" else "FREE",
                        color = if (isVipUnlocked) StarGold else CrimsonLight,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 2-Column Balanced Stat Cards: My Points | Share Counting
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = StarGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StarGold.copy(alpha = 0.45f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(StarGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = StarGold, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "${userRecord.points} Pts",
                                color = StarGold,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "My Points",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Surface(
                    color = CrimsonRed.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CrimsonRed.copy(alpha = 0.45f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CrimsonRed.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.People, contentDescription = null, tint = CrimsonLight, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text(
                                text = "${userRecord.shareCount}",
                                color = CrimsonLight,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "Share Count",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Progress Bar toward VIP Access
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isVipUnlocked) "VIP Access Unlocked!"
                        else "${userRecord.points} / $requiredVipPoints Pts to Unlock VIP",
                        color = if (isVipUnlocked) StatusGreen else TextWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "+${vipConfig.pointsPerReferral} Pts / Code Redeem",
                        color = StarGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = if (isVipUnlocked) StatusGreen else StarGold,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                )
            }

            // My 5-Digit Share Code & Real Device ID Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, StatusBlue.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MY 5-DIGIT SHARE CODE",
                                color = TextSecondary,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = resolvedShareCode.toCharArray().joinToString(" "),
                                color = StatusBlue,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("5-Digit Share Code", resolvedShareCode))
                                Toast.makeText(context, "Copied 5-Digit Code: $resolvedShareCode", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, StatusBlue.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Share Code",
                                tint = StatusBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Code", color = StatusBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Smartphone,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Device ID: $resolvedDeviceId",
                                color = TextSecondary,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Device ID", resolvedDeviceId))
                                Toast.makeText(context, "Device ID copied: $resolvedDeviceId", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy Device ID",
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            // Redeem 5-Digit Share Code Option (Full-width Input + Full-width Button)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        tint = StarGold,
                        modifier = Modifier.size(19.dp)
                    )
                    Column {
                        Text(
                            text = "Redeem 5-Digit Share Code",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                        Text(
                            text = "Enter a friend's 5-digit Share Code to add +${vipConfig.pointsPerReferral} VIP Points",
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = redeemCodeInput,
                    onValueChange = { input ->
                        redeemCodeInput = input.filter { it.isDigit() }.take(5)
                        redeemStatusNote = null
                    },
                    label = { Text("Enter 5-Digit Share Code", maxLines = 1) },
                    placeholder = { Text("e.g. 48291", fontSize = 13.sp, maxLines = 1) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val clean = redeemCodeInput.trim()
                        if (clean.length != 5) {
                            redeemStatusNote = "Please enter a valid 5-digit numeric Share Code."
                        } else {
                            onRedeemShareCode(clean)
                            redeemCodeInput = ""
                        }
                    },
                    enabled = redeemCodeInput.length == 5,
                    colors = ButtonDefaults.buttonColors(containerColor = StarGold),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Redeem 5-Digit Code (+${vipConfig.pointsPerReferral} Pts)",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        maxLines = 1
                    )
                }

                redeemStatusNote?.let { note ->
                    Text(
                        text = note,
                        color = CrimsonLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (userRecord.redeemedCodes.isNotEmpty() || userRecord.redeemedCode.isNotBlank()) {
                    val redeemedDisplay = userRecord.redeemedCodes.joinToString(", ").ifBlank { userRecord.redeemedCode }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = StatusGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Redeemed Share Code(s): $redeemedDisplay",
                            color = StatusGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Quick Access to Offline Downloads
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, CrimsonRed.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenOfflineDownloads)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Filled.DownloadForOffline,
                    contentDescription = null,
                    tint = CrimsonRed,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "My Offline Downloads",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Watch downloaded Movies & Web Series without internet",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onOpenOfflineDownloads,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Open", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // App Settings Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "App Settings",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("App Theme", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onSelectTheme(ThemeVariant.DEEP_BLACK) }
                    ) {
                        RadioButton(
                            selected = preferences.themeVariant == ThemeVariant.DEEP_BLACK,
                            onClick = { onSelectTheme(ThemeVariant.DEEP_BLACK) },
                            colors = RadioButtonDefaults.colors(selectedColor = CrimsonRed)
                        )
                        Text("Dark", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onSelectTheme(ThemeVariant.DEEP_RED) }
                    ) {
                        RadioButton(
                            selected = preferences.themeVariant == ThemeVariant.DEEP_RED,
                            onClick = { onSelectTheme(ThemeVariant.DEEP_RED) },
                            colors = RadioButtonDefaults.colors(selectedColor = CrimsonRed)
                        )
                        Text("Red", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Preferred Quality", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                    Text(
                        "Applied when the video source provides multiple tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { qualityMenu = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(preferences.defaultQuality, style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    }
                    DropdownMenu(
                        expanded = qualityMenu,
                        onDismissRequest = { qualityMenu = false }
                    ) {
                        qualities.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q) },
                                onClick = {
                                    onSelectQuality(q)
                                    qualityMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto Play Next Episode", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                Switch(
                    checked = preferences.autoPlayNextEpisode,
                    onCheckedChange = onToggleAutoPlay,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CrimsonRed
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Notifications", style = MaterialTheme.typography.bodyMedium, color = TextWhite)
                Switch(
                    checked = preferences.notificationsEnabled,
                    onCheckedChange = onToggleNotifications,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = CrimsonRed
                    )
                )
            }
        }

        // About & Developer Card
        val nativeDevInfo = remember(context) {
            NhNativeSecurityVault.getDeveloperInfo(context)
        }
        val openTelegramLink: () -> Unit = {
            if (nativeDevInfo.telegramUrl.isNotBlank()) {
                runCatching {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(nativeDevInfo.telegramUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "About & Developer",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Version", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text(
                    text = BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CrimsonRed.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Code,
                            contentDescription = null,
                            tint = CrimsonRed,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = "About Developer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = nativeDevInfo.developerName,
                    color = TextWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, Color(0xFF29B6F6).copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .clickable(onClick = openTelegramLink)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF29B6F6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Telegram",
                            tint = Color(0xFF29B6F6),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Official Telegram Channel",
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = nativeDevInfo.telegramUrl,
                            color = Color(0xFF29B6F6),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Button(
                    onClick = openTelegramLink,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "Join",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                Text(
                    text = stringResource(R.string.tmdb_attribution),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
