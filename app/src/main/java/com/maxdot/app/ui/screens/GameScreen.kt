package com.maxdot.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maxdot.app.GameViewModel
import com.maxdot.app.MainViewModel
import com.maxdot.app.data.Book
import com.maxdot.core.model.Challenge
import com.maxdot.core.model.ChallengeType
import com.maxdot.app.ui.theme.feedbackColors
import com.maxdot.app.ui.theme.isAppInDarkTheme
import com.maxdot.app.ui.theme.toFamily

@Composable
fun GameScreen(
    book: Book,
    viewModel: GameViewModel,
    mainViewModel: MainViewModel,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile by mainViewModel.profiles.profile.collectAsStateWithLifecycle()
    val settings by mainViewModel.profiles.settings.collectAsStateWithLifecycle()
    val celebrations by viewModel.celebrations.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val dark = isAppInDarkTheme(settings)
    val feedback = feedbackColors(dark)

    LaunchedEffect(celebrations.firstOrNull()) {
        celebrations.firstOrNull()?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.consumeCelebration()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExit) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                HowToPlayButton()
            }

            ScoreBar(
                sessionCorrect = state.sessionCorrect,
                sessionAnswered = state.sessionAnswered,
                streak = profile.currentStreak,
                hints = profile.hints,
                bookPercent = state.progress.percent,
            )

            Spacer(Modifier.height(12.dp))

            when {
                state.loading -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Preparing passage…")
                    }
                }

                state.error != null -> {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Something went wrong", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(state.error ?: "")
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = onExit) { Text("Back to library") }
                        }
                    }
                }

                state.bookFinished -> {
                    BookFinishedCard(
                        onRestart = { viewModel.restartBook() },
                        onExit = onExit,
                    )
                }

                else -> {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        PassageCard(
                            state = state,
                            fontScale = settings.fontScale,
                            fontFamilyKey = settings.font,
                            feedback = feedback,
                            onTapChallenge = { id -> viewModel.openChallenge(id) },
                        )
                        Spacer(Modifier.height(12.dp))
                        if (state.passageComplete) {
                            PassageSummaryCard(
                                correct = state.passage?.challenges?.count {
                                    state.answers[it.id]?.correct == true
                                } ?: 0,
                                total = state.passage?.challenges?.size ?: 0,
                                xp = state.passageXp,
                                perfect = state.passagePerfect,
                                onNext = { viewModel.nextPassage() },
                            )
                        } else {
                            Text(
                                "Tap the highlighted words and [?] marks to fix the text.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    state.openChallenge?.let { challenge ->
        AnswerDialog(
            challenge = challenge,
            answer = state.answers[challenge.id],
            eliminated = state.eliminatedOptions,
            hintsAvailable = profile.hints,
            onUseHint = { viewModel.useHint() },
            onSelect = { index ->
                viewModel.answer(index)
                if (settings.hapticsEnabled) {
                    val correct = index == challenge.correctIndex
                    haptics.performHapticFeedback(
                        if (correct) HapticFeedbackType.Confirm else HapticFeedbackType.Reject,
                    )
                }
            },
            onDismiss = { viewModel.dismissChallenge() },
        )
    }
}

@Composable
private fun HowToPlayButton() {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.HelpOutline, contentDescription = "How to play")
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("How to play") },
            text = {
                Text(
                    "This passage comes from the book, but punctuation has been removed " +
                        "and some words swapped for common mistakes.\n\n" +
                        "• Tap a [?] mark to choose the missing punctuation (sometimes " +
                        "nothing belongs there!)\n" +
                        "• Tap a highlighted word to pick the correct spelling or word\n" +
                        "• Build streaks for bonus XP, and spend hints (💡) when stuck\n\n" +
                        "Finish every challenge in a passage to move deeper into the book.",
                )
            },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text("Got it") }
            },
        )
    }
}

@Composable
private fun ScoreBar(
    sessionCorrect: Int,
    sessionAnswered: Int,
    streak: Int,
    hints: Int,
    bookPercent: Int,
) {
    val accuracy = if (sessionAnswered == 0) null else (sessionCorrect * 100) / sessionAnswered
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "$sessionCorrect/$sessionAnswered" + (accuracy?.let { "  ·  $it%" } ?: ""),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = if (streak > 0) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text("$streak", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Filled.Lightbulb,
                    contentDescription = "Hints",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
                Text("$hints", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Book $bookPercent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PassageCard(
    state: com.maxdot.app.GameUiState,
    fontScale: Float,
    fontFamilyKey: com.maxdot.app.data.AppFont,
    feedback: com.maxdot.app.ui.theme.FeedbackColors,
    onTapChallenge: (Int) -> Unit,
) {
    val passage = state.passage ?: return
    val pendingStyle = SpanStyle(
        background = MaterialTheme.colorScheme.tertiaryContainer,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        fontWeight = FontWeight.Bold,
    )

    val annotated = buildAnnotatedString {
        for (segment in passage.segments) {
            val id = segment.challengeId
            if (id == null) {
                append(segment.text)
                continue
            }
            val answer = state.answers[id]
            val challenge = passage.challenge(id)
            when {
                answer == null -> {
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "challenge_$id",
                            linkInteractionListener = { onTapChallenge(id) },
                        ),
                    ) {
                        withStyle(pendingStyle) {
                            append(" ${segment.text} ")
                        }
                    }
                }

                answer.correct -> {
                    withStyle(SpanStyle(color = feedback.correct, fontWeight = FontWeight.Bold)) {
                        append(challenge.resolvedText)
                    }
                }

                else -> {
                    withStyle(
                        SpanStyle(
                            color = feedback.wrong,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                        ),
                    ) {
                        append(challenge.resolvedText)
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        ),
    ) {
        Text(
            annotated,
            modifier = Modifier.padding(18.dp),
            fontSize = 18.sp * fontScale,
            lineHeight = 30.sp * fontScale,
            fontFamily = fontFamilyKey.toFamily(),
        )
    }
}

@Composable
private fun AnswerDialog(
    challenge: Challenge,
    answer: com.maxdot.app.AnswerState?,
    eliminated: Set<Int>,
    hintsAvailable: Int,
    onUseHint: () -> Unit,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (challenge.type) {
        ChallengeType.PUNCTUATION -> "What belongs here?"
        ChallengeType.GRAMMAR -> "Which word is correct?"
        ChallengeType.APOSTROPHE -> "Fix the apostrophe"
        ChallengeType.CAPITALIZATION -> "Fix the capitalization"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (challenge.type != ChallengeType.PUNCTUATION) {
                    Text(
                        "Shown in the text: \"${challenge.shownText}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                challenge.options.forEachIndexed { index, option ->
                    val isEliminated = index in eliminated
                    val display = if (option == Challenge.NO_PUNCTUATION) "(nothing — leave as is)" else option
                    val revealed = answer != null
                    val isCorrectOption = index == challenge.correctIndex
                    val isPicked = answer?.selectedIndex == index

                    OutlinedButton(
                        onClick = { if (!revealed && !isEliminated) onSelect(index) },
                        enabled = !isEliminated && !revealed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        border = when {
                            revealed && isCorrectOption -> BorderStroke(2.dp, feedbackColors(false).correct)
                            revealed && isPicked -> BorderStroke(2.dp, feedbackColors(false).wrong)
                            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        },
                    ) {
                        Text(
                            display + when {
                                revealed && isCorrectOption -> "  ✓"
                                revealed && isPicked -> "  ✗"
                                else -> ""
                            },
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                if (answer != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (answer.correct) "Correct! ✨" else "Not quite.",
                        fontWeight = FontWeight.Bold,
                        color = if (answer.correct) feedbackColors(false).correct else feedbackColors(false).wrong,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(challenge.explanation, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (answer != null) {
                TextButton(onClick = onDismiss) { Text("Continue") }
            }
        },
        dismissButton = {
            if (answer == null) {
                Row {
                    TextButton(
                        onClick = onUseHint,
                        enabled = hintsAvailable > 0 &&
                            challenge.options.size - eliminated.size > 2,
                    ) {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hint ($hintsAvailable)")
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        },
    )
}

@Composable
private fun PassageSummaryCard(
    correct: Int,
    total: Int,
    xp: Int,
    perfect: Boolean,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
        ),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (perfect) "Perfect passage! 🌟" else "Passage complete",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text("$correct of $total correct  ·  +$xp XP" + if (perfect) " (incl. bonus)" else "")
            Spacer(Modifier.height(12.dp))
            Button(onClick = onNext) { Text("Next passage →") }
        }
    }
}

@Composable
private fun BookFinishedCard(onRestart: () -> Unit, onExit: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Celebration,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "You finished the book! 🎉",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Every passage has been proofread. Play it again or pick another book.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onExit) { Text("Library") }
                Button(onClick = onRestart) { Text("Play again") }
            }
        }
    }
}
