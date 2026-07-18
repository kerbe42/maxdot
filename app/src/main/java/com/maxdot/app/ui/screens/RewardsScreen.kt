package com.maxdot.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxdot.app.MainViewModel
import com.maxdot.core.game.Achievements
import com.maxdot.core.game.Leveling

@Composable
fun RewardsScreen(
    mainViewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val profile by mainViewModel.profiles.profile.collectAsStateWithLifecycle()

    Scaffold(containerColor = Color.Transparent) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        "Rewards & progress",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Level ${profile.level}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                (Leveling.xpIntoLevel(profile.xp).toFloat() /
                                    Leveling.xpForNextLevel(profile.level)).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${profile.xp} XP total  ·  best streak ${profile.bestStreak}  ·  " +
                                "${profile.perfectPassages} perfect passages",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            item {
                Text(
                    "Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            items(Achievements.ALL, key = { it.id }) { achievement ->
                val unlocked = achievement.id in profile.unlockedAchievements
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (unlocked) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                        },
                    ),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            achievement.emoji,
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                achievement.title,
                                fontWeight = FontWeight.SemiBold,
                                color = if (unlocked) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                achievement.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (unlocked) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Unlocked",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
