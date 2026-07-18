package com.maxdot.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxdot.app.MainViewModel
import com.maxdot.app.data.AppFont
import com.maxdot.core.model.Difficulty
import com.maxdot.core.model.GameMode
import com.maxdot.app.ui.theme.GameTheme
import com.maxdot.app.ui.theme.GameThemes
import com.maxdot.app.ui.theme.toFamily

@Composable
fun SettingsScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val settings by mainViewModel.profiles.settings.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            SettingsCard("Appearance") {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    GameThemes.ALL.forEach { theme ->
                        ThemePickerTile(
                            theme = theme,
                            selected = settings.selectedTheme == theme.id,
                            onSelect = { mainViewModel.profiles.setTheme(theme.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Reading font", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFont.entries.forEach { font ->
                        FilterChip(
                            selected = settings.font == font,
                            onClick = { mainViewModel.profiles.setFont(font) },
                            label = { Text(font.label, fontFamily = font.toFamily()) },
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Text size  ·  ${(settings.fontScale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = settings.fontScale,
                    onValueChange = { mainViewModel.profiles.setFontScale(it) },
                    valueRange = 0.8f..1.6f,
                    steps = 7,
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Text(
                        "It is a truth universally acknowledged…",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 18.sp * settings.fontScale,
                        lineHeight = 28.sp * settings.fontScale,
                        fontFamily = settings.font.toFamily(),
                    )
                }
            }

            SettingsCard("Gameplay") {
                Text("Mode", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    GameMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.gameMode == mode,
                            onClick = { mainViewModel.profiles.setGameMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, GameMode.entries.size),
                        ) { Text(mode.label) }
                    }
                }
                Text(
                    when (settings.gameMode) {
                        GameMode.GUIDED ->
                            "Mistakes are highlighted — tap them and pick the fix from a list."
                        GameMode.ADVANCED ->
                            "Mistakes are hidden. You're told how many to find, then you type each fix in place."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(Modifier.height(14.dp))
                Text("Difficulty", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Difficulty.entries.forEachIndexed { index, d ->
                        SegmentedButton(
                            selected = settings.difficulty == d,
                            onClick = { mainViewModel.profiles.setDifficulty(d) },
                            shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size),
                        ) { Text(d.label) }
                    }
                }
                Text(
                    when (settings.difficulty) {
                        Difficulty.EASY -> "Fewer mistakes per passage, simpler choices."
                        Difficulty.MEDIUM -> "More mistakes, tricky word mix-ups included."
                        Difficulty.HARD -> "Most mistakes, extra options, and decoy slots where nothing is missing."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )

                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Haptic feedback", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Vibrate on correct and wrong answers",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.hapticsEnabled,
                        onCheckedChange = { mainViewModel.profiles.setHaptics(it) },
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text("Daily goal", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(50, 100, 200, 300).forEach { goal ->
                        FilterChip(
                            selected = settings.dailyGoalXp == goal,
                            onClick = { mainViewModel.profiles.setDailyGoal(goal) },
                            label = { Text("$goal XP") },
                        )
                    }
                }
            }

            SettingsCard("Data") {
                TextButton(onClick = { showResetDialog = true }) {
                    Text("Reset all progress", color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all progress?") },
            text = { Text("XP, levels, achievements, streaks, and book positions will be erased. Imported books are kept. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    mainViewModel.profiles.resetProgress()
                    mainViewModel.books.clearAllProgress()
                    showResetDialog = false
                }) { Text("Reset", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ThemePickerTile(
    theme: GameTheme,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stops = theme.colors.bg.let { if (it.size == 1) it + it else it }
    val outline = if (selected) theme.colors.primary
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(BorderStroke(if (selected) 3.dp else 1.dp, outline), RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.verticalGradient(stops)),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(16.dp).clip(CircleShape).background(theme.colors.primary))
                Box(
                    Modifier
                        .size(width = 22.dp, height = 10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(theme.colors.accent),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            theme.name,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = theme.type.hud,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
