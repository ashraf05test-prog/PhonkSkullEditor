package com.phonkskull.editor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonkskull.editor.data.ColorPreset
import com.phonkskull.editor.data.SecureSettings
import com.phonkskull.editor.data.TextGlowColor

// ── Colors ──────────────────────────────────────────────────────────────────
val BgDark = Color(0xFF080810)
val CardBg = Color(0xFF12121E)
val AccentRed = Color(0xFFFF2D55)
val AccentGreen = Color(0xFF00FF88)
val AccentPurple = Color(0xFF8B5CF6)
val TextPrimary = Color(0xFFEEEEEE)
val TextSub = Color(0xFF8888AA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showDrivePicker by remember { mutableStateOf(false) }

    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.attachVideoUri(context, it) }
    }
    val pickSkull = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.attachSkullUri(context, it) }
    }
    val pickPhonkLocal = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.attachPhonkUri(context, it) }
    }
    val pickPhonkUpload = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { vm.uploadPhonkToDrive(context, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "💀 Phonk Skull Editor",
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 18.sp,
                    )
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSub)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark),
            )
        },
        containerColor = BgDark,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── 1. Video ─────────────────────────────────────────────────────
            Section("① Video") {
                PhonkButton(
                    if (vm.videoLocalPath != null) "✓ Video selected" else "Pick video from gallery",
                    icon = Icons.Default.VideoFile,
                ) { pickVideo.launch(arrayOf("video/*")) }
            }

            // ── 2. Glow Text ─────────────────────────────────────────────────
            Section("② Top Text") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable glow text", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(vm.glowTextEnabled, { vm.glowTextEnabled = it }, colors = switchColors())
                }
                if (vm.glowTextEnabled) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = vm.glowText,
                        onValueChange = { vm.glowText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Text", color = TextSub) },
                        colors = outlinedFieldColors(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Glow color", color = TextSub, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlowColorChip("🔴 RED", vm.glowColor == TextGlowColor.RED, AccentRed) {
                            vm.glowColor = TextGlowColor.RED
                        }
                        GlowColorChip("🟢 GREEN", vm.glowColor == TextGlowColor.GREEN, AccentGreen) {
                            vm.glowColor = TextGlowColor.GREEN
                        }
                    }
                }
            }

            // ── 3. Color Grading ─────────────────────────────────────────────
            Section("③ Color Grading") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable grading", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(vm.colorGradingEnabled, { vm.colorGradingEnabled = it }, colors = switchColors())
                }
                if (vm.colorGradingEnabled) {
                    Spacer(Modifier.height(8.dp))
                    val presets = listOf(
                        ColorPreset.MOODY to "🌑 Moody",
                        ColorPreset.DARK_PHONK to "💀 Dark Phonk",
                        ColorPreset.CINEMATIC to "🎬 Cinematic",
                        ColorPreset.COLD_BLUE to "❄️ Cold Blue",
                        ColorPreset.VINTAGE to "📼 Vintage",
                        ColorPreset.HIGH_CONTRAST to "⚡ High Contrast",
                    )
                    presets.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { (preset, label) ->
                                PresetChip(label, vm.colorPreset == preset, Modifier.weight(1f)) {
                                    vm.colorPreset = preset
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            // ── 4. Skull ─────────────────────────────────────────────────────
            Section("④ Skull") {
                PhonkButton(
                    if (vm.skullPath != null) "✓ Skull PNG selected" else "Pick skull PNG",
                    icon = Icons.Default.Image,
                ) { pickSkull.launch(arrayOf("image/*")) }

                if (vm.skullPath != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Size: ${(vm.skullSizePct * 100).toInt()}%", color = TextSub, fontSize = 13.sp)
                    Slider(
                        value = vm.skullSizePct,
                        onValueChange = { vm.skullSizePct = it },
                        valueRange = 0.2f..0.9f,
                        colors = sliderColors(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Climax time (when skull appears)", color = TextSub, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = vm.climaxTimeText,
                        onValueChange = {
                            vm.climaxTimeText = it
                            vm.climaxTimeSec = parseTime(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("hh:mm:ss", color = TextSub) },
                        colors = outlinedFieldColors(),
                        singleLine = true,
                    )
                    Text("Skull appears here → 3 sec → video ends", color = TextSub, fontSize = 12.sp)
                }
            }

            // ── 5. Phonk ─────────────────────────────────────────────────────
            Section("⑤ Phonk Track") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhonkButton("From Drive", icon = Icons.Default.CloudDownload, modifier = Modifier.weight(1f)) {
                        showDrivePicker = true
                        vm.loadDriveTracks(context)
                    }
                    PhonkButton("Local file", icon = Icons.Default.AudioFile, modifier = Modifier.weight(1f)) {
                        pickPhonkLocal.launch(arrayOf("audio/*"))
                    }
                }

                if (vm.phonkLocalPath != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("✓ Local phonk selected", color = AccentGreen, fontSize = 13.sp)
                }
                if (vm.selectedPhonkTrack != null) {
                    Spacer(Modifier.height(4.dp))
                    Text("✓ ${vm.selectedPhonkTrack!!.name}", color = AccentGreen, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(Modifier.height(8.dp))
                Text("Volume: ${vm.phonkVolume}%", color = TextSub, fontSize = 13.sp)
                Slider(
                    value = vm.phonkVolume.toFloat(),
                    onValueChange = { vm.phonkVolume = it.toInt() },
                    valueRange = 0f..100f,
                    colors = sliderColors(),
                )
            }

            // ── 6. Shake ─────────────────────────────────────────────────────
            Section("⑥ Freeze Shake (optional)") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Screen shake on freeze", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(vm.shakeEnabled, { vm.shakeEnabled = it }, colors = switchColors())
                }
                if (vm.shakeEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Text("Intensity: ${vm.shakeIntensity.toInt()}px", color = TextSub, fontSize = 13.sp)
                    Slider(
                        value = vm.shakeIntensity,
                        onValueChange = { vm.shakeIntensity = it },
                        valueRange = 2f..20f,
                        colors = sliderColors(),
                    )
                }
            }

            // ── 7. Export ────────────────────────────────────────────────────
            Section("⑦ Export") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Save to gallery", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(vm.saveToGallery, { vm.saveToGallery = it }, colors = switchColors())
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Upload to YouTube", color = TextPrimary, modifier = Modifier.weight(1f))
                    Switch(vm.uploadToYouTube, { vm.uploadToYouTube = it }, colors = switchColors())
                }
                if (vm.uploadToYouTube) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = vm.ytTitle,
                        onValueChange = { vm.ytTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("YouTube title", color = TextSub) },
                        colors = outlinedFieldColors(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("private", "unlisted", "public").forEach { p ->
                            PresetChip(p.replaceFirstChar { it.uppercase() }, vm.ytPrivacy == p, Modifier.weight(1f)) {
                                vm.ytPrivacy = p
                            }
                        }
                    }
                }
            }

            // ── Progress ─────────────────────────────────────────────────────
            if (vm.working) {
                Spacer(Modifier.height(4.dp))
                Text(vm.progressMsg, color = AccentPurple, fontSize = 13.sp)
                LinearProgressIndicator(
                    progress = { vm.progress / 100f },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                    color = AccentPurple,
                    trackColor = CardBg,
                )
            }

            vm.errorMsg?.let {
                Text("⚠️ $it", color = AccentRed, fontSize = 13.sp)
            }

            vm.outputPath?.let {
                Text("✅ Saved! ${it.substringAfterLast("/")}", color = AccentGreen, fontSize = 13.sp)
            }

            // ── Process Button ────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { vm.process(context) },
                enabled = !vm.working && vm.videoLocalPath != null,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("PROCESS VIDEO", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Drive Picker Sheet ───────────────────────────────────────────────────
    if (showDrivePicker) {
        ModalBottomSheet(
            onDismissRequest = { showDrivePicker = false },
            containerColor = CardBg,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Drive Phonk Library", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { pickPhonkUpload.launch(arrayOf("audio/*")) }) {
                        Icon(Icons.Default.Upload, contentDescription = "Upload", tint = AccentGreen)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (vm.driveTracksLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentPurple)
                    }
                } else if (vm.phonkTracks.isEmpty()) {
                    Text("No tracks found. Upload some phonk! ⬆️", color = TextSub)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(vm.phonkTracks) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vm.selectedPhonkTrack = track
                                        vm.phonkLocalPath = null
                                        showDrivePicker = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(track.name, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (vm.selectedPhonkTrack?.id == track.id) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = Color(0xFF222235))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ── Settings Sheet ───────────────────────────────────────────────────────
    if (showSettings) {
        SettingsSheet(
            settings = vm.secureSettings,
            onSave = { vm.saveSettings(it); showSettings = false },
            onDismiss = { showSettings = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: SecureSettings,
    onSave: (SecureSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    var ytToken by remember { mutableStateOf(settings.youtubeAccessToken) }
    var driveToken by remember { mutableStateOf(settings.driveAccessToken) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(ytToken, { ytToken = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("YouTube Access Token", color = TextSub) }, colors = outlinedFieldColors())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(driveToken, { driveToken = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Google Drive Access Token", color = TextSub) }, colors = outlinedFieldColors())
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(settings.copy(youtubeAccessToken = ytToken, driveAccessToken = driveToken)) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
            ) { Text("Save", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(14.dp),
    ) {
        Text(title, color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
fun PhonkButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(alpha = 0.5f)),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun GlowColorChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) color.copy(alpha = 0.2f) else Color.Transparent)
            .border(1.dp, if (selected) color else TextSub.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) color else TextSub, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
    }
}

@Composable
fun PresetChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) AccentPurple.copy(alpha = 0.25f) else Color.Transparent)
            .border(1.dp, if (selected) AccentPurple else TextSub.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) AccentPurple else TextSub, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable fun switchColors() = SwitchDefaults.colors(checkedThumbColor = AccentPurple, checkedTrackColor = AccentPurple.copy(alpha = 0.3f))
@Composable fun sliderColors() = SliderDefaults.colors(thumbColor = AccentPurple, activeTrackColor = AccentPurple)

@Composable
fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentPurple,
    unfocusedBorderColor = TextSub.copy(alpha = 0.3f),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
)

fun parseTime(s: String): Double {
    val parts = s.split(":").map { it.toDoubleOrNull() ?: 0.0 }
    return when (parts.size) {
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        2 -> parts[0] * 60 + parts[1]
        else -> parts.firstOrNull() ?: 0.0
    }
}
