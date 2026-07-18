package com.maxdot.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maxdot.app.ProofUiState
import com.maxdot.app.data.AppFont
import com.maxdot.app.ui.theme.FeedbackColors
import com.maxdot.app.ui.theme.toFamily
import com.maxdot.core.model.ChallengeType
import com.maxdot.core.model.ProofPassage
import com.maxdot.core.model.ProofResult
import com.maxdot.core.model.TokenOutcome

/** Fixed display order and labels for the mistake-count brief. */
private val TYPE_ORDER = listOf(
    ChallengeType.PUNCTUATION,
    ChallengeType.GRAMMAR,
    ChallengeType.APOSTROPHE,
    ChallengeType.CAPITALIZATION,
)

private fun typeLabel(type: ChallengeType, count: Int): String = when (type) {
    ChallengeType.PUNCTUATION -> "punctuation"
    ChallengeType.GRAMMAR -> "word choice"
    ChallengeType.APOSTROPHE -> if (count == 1) "apostrophe" else "apostrophes"
    ChallengeType.CAPITALIZATION -> "capitalization"
}

/** "5 mistakes to find — 2 punctuation · 2 word choice · 1 capitalization". */
@Composable
fun AdvancedBriefCard(passage: ProofPassage) {
    val total = passage.mistakeTotal
    val breakdown = TYPE_ORDER.mapNotNull { type ->
        passage.mistakeCounts[type]?.let { "$it ${typeLabel(type, it)}" }
    }.joinToString("  ·  ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "$total ${if (total == 1) "mistake" else "mistakes"} to find",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                breakdown,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap any word to fix it — punctuation attaches to the word before the gap. Then Check.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
            )
        }
    }
}

/**
 * The passage as plain, tappable words. Before checking, edited words are
 * highlighted and hinted words underlined; after checking, every token is
 * revealed in its correct form and colored by outcome.
 */
@Composable
fun AdvancedPassageCard(
    proof: ProofUiState,
    fontScale: Float,
    fontFamilyKey: AppFont,
    feedback: FeedbackColors,
    onTapToken: (Int) -> Unit,
) {
    val checked = proof.checked
    val editedStyle = SpanStyle(
        background = MaterialTheme.colorScheme.tertiaryContainer,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        fontWeight = FontWeight.Bold,
    )
    val hintStyle = SpanStyle(
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline,
    )

    val annotated = buildAnnotatedString {
        for (token in proof.passage.tokens) {
            if (token.leadingSpace) append(" ")

            if (!checked) {
                val display = proof.edits[token.index] ?: token.shownText
                val style = when {
                    token.index in proof.edits -> editedStyle
                    token.index in proof.revealedHints -> hintStyle
                    else -> null
                }
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "token_${token.index}",
                        linkInteractionListener = { onTapToken(token.index) },
                    ),
                ) {
                    if (style != null) withStyle(style) { append(display) } else append(display)
                }
            } else {
                when (proof.result?.outcomes?.get(token.index)) {
                    TokenOutcome.CAUGHT ->
                        withStyle(SpanStyle(color = feedback.correct, fontWeight = FontWeight.Bold)) {
                            append(token.correctText)
                        }

                    TokenOutcome.MISSED ->
                        withStyle(
                            SpanStyle(
                                color = feedback.wrong,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) { append(token.correctText) }

                    TokenOutcome.FALSE_FLAG -> {
                        val wrong = proof.edits[token.index] ?: token.shownText
                        withStyle(
                            SpanStyle(color = feedback.wrong, textDecoration = TextDecoration.LineThrough),
                        ) { append(wrong) }
                        append(" ")
                        withStyle(SpanStyle(color = feedback.correct, fontWeight = FontWeight.Bold)) {
                            append(token.correctText)
                        }
                    }

                    else -> append(token.correctText)
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

/** Inline editor: retype the word (and any punctuation) the way it should read. */
@Composable
fun TokenEditorDialog(
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember(initial) {
        mutableStateOf(TextFieldValue(initial, TextRange(initial.length)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fix the text") },
        text = {
            Column {
                Text(
                    "Retype this word the way it should appear — fix a spelling, add an " +
                        "apostrophe or capital, or add the punctuation that belongs after it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(value.text) }),
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (mark in listOf(".", ",", ";", ":", "!", "?", "'")) {
                        OutlinedButton(
                            onClick = { value = insertAtCursor(value, mark) },
                            modifier = Modifier.width(44.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        ) { Text(mark, style = MaterialTheme.typography.titleMedium) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(value.text) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun insertAtCursor(value: TextFieldValue, insert: String): TextFieldValue {
    val start = value.selection.start.coerceIn(0, value.text.length)
    val end = value.selection.end.coerceIn(0, value.text.length)
    val text = value.text.substring(0, start) + insert + value.text.substring(end)
    val cursor = start + insert.length
    return TextFieldValue(text, TextRange(cursor))
}

/** Post-check summary: caught / missed / false flags, XP, and a color legend. */
@Composable
fun AdvancedSummaryCard(
    result: ProofResult,
    xp: Int,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (result.perfect) "Perfect proofread! 🌟" else "Passage checked",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            val parts = buildList {
                add("Caught ${result.caught}/${result.totalMistakes}")
                if (result.falseFlags > 0) {
                    add("${result.falseFlags} false ${if (result.falseFlags == 1) "flag" else "flags"}")
                }
                add("+$xp XP")
            }
            Text(parts.joinToString("  ·  "))
            Spacer(Modifier.height(6.dp))
            Text(
                "Green = you caught it · red underline = missed · struck-through = a word you " +
                    "changed that was already correct.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Button(onClick = onNext) { Text("Next passage →") }
        }
    }
}
