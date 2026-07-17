package com.maxdot.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxdot.app.ImportStatus
import com.maxdot.app.MainViewModel
import com.maxdot.app.data.Book
import com.maxdot.core.game.Leveling

@Composable
fun LibraryScreen(
    mainViewModel: MainViewModel,
    onPlay: (Book) -> Unit,
    onOpenRewards: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val profile by mainViewModel.profiles.profile.collectAsStateWithLifecycle()
    val settings by mainViewModel.profiles.settings.collectAsStateWithLifecycle()
    val books by mainViewModel.books.books.collectAsStateWithLifecycle()
    val importStatus by mainViewModel.importStatus.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) mainViewModel.importBook(uri)
    }

    LaunchedEffect(importStatus) {
        when (val status = importStatus) {
            is ImportStatus.Done -> {
                snackbarHostState.showSnackbar("Imported \"${status.book.title}\"")
                mainViewModel.clearImportStatus()
            }
            is ImportStatus.Failed -> {
                snackbarHostState.showSnackbar(status.message)
                mainViewModel.clearImportStatus()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    importLauncher.launch(
                        arrayOf(
                            "application/epub+zip",
                            "application/pdf",
                            "application/x-mobipocket-ebook",
                            "application/octet-stream",
                            "text/plain",
                        ),
                    )
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Import book") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "MaxDot",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onOpenRewards) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = "Rewards")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            }

            item {
                ProfileHeaderCard(
                    level = profile.level,
                    xpIntoLevel = Leveling.xpIntoLevel(profile.xp),
                    xpForNext = Leveling.xpForNextLevel(profile.level),
                    accuracy = profile.accuracy,
                    totalCorrect = profile.totalCorrect,
                    dailyXp = profile.dailyXp,
                    dailyGoal = settings.dailyGoalXp,
                    dayStreak = profile.dayStreak,
                )
            }

            item {
                Text(
                    "Your library",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            items(books, key = { it.id }) { book ->
                val progress = mainViewModel.books.progress(book.id)
                BookCard(
                    book = book,
                    progressPercent = progress.percent,
                    accuracy = if (progress.answered == 0) null else (progress.correct * 100) / progress.answered,
                    timesFinished = progress.timesFinished,
                    onPlay = { onPlay(book) },
                    onDelete = if (book.isBundled) null else ({ bookToDelete = book }),
                )
            }

            item { Spacer(Modifier.height(80.dp)) } // keep FAB clear of last card
        }
    }

    if (importStatus is ImportStatus.Working) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Importing book…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(16.dp))
                    Text("Reading and preparing the text")
                }
            },
        )
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Remove book?") },
            text = { Text("\"${book.title}\" and its progress will be removed from MaxDot. The original file on your device is not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    mainViewModel.books.deleteImported(book)
                    bookToDelete = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    level: Int,
    xpIntoLevel: Int,
    xpForNext: Int,
    accuracy: Int,
    totalCorrect: Int,
    dailyXp: Int,
    dailyGoal: Int,
    dayStreak: Int,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Level $level",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.weight(1f))
                if (dayStreak > 1) {
                    Icon(
                        Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        "$dayStreak-day streak",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (xpIntoLevel.toFloat() / xpForNext).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$xpIntoLevel / $xpForNext XP to level ${level + 1}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatChip("Correct", "$totalCorrect")
                StatChip("Accuracy", if (totalCorrect + accuracy == 0) "—" else "$accuracy%")
                StatChip("Today", "$dailyXp/$dailyGoal XP")
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun BookCard(
    book: Book,
    progressPercent: Int,
    accuracy: Int?,
    timesFinished: Int,
    onPlay: () -> Unit,
    onDelete: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onPlay,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    book.author + if (book.isBundled) "  ·  Classic" else "  ·  Imported",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progressPercent / 100f },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        buildString {
                            append("$progressPercent%")
                            if (accuracy != null) append("  ·  $accuracy% right")
                            if (timesFinished > 0) append("  ·  ✓ finished")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove book",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
