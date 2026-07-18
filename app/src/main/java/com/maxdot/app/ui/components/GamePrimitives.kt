package com.maxdot.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import com.maxdot.app.ui.theme.ButtonStyle
import com.maxdot.app.ui.theme.GameTheme
import com.maxdot.app.ui.theme.GameThemes
import com.maxdot.app.ui.theme.LocalGameTheme

/** Full-screen box painted with the active theme's background brush. */
@Composable
fun GameBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val theme = LocalGameTheme.current
    val stops = theme.colors.bg.let { if (it.size == 1) it + it else it }
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(stops)),
        content = content,
    )
}

/** A themed surface card: soft-elevated (Playful), glow-edged (Neon), or block-bordered (Retro). */
@Composable
fun GameCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val theme = LocalGameTheme.current
    val shape = RoundedCornerShape(theme.shapes.cardCorner)
    val elevated = if (!theme.colors.dark) {
        Modifier.shadow(6.dp, shape, clip = false)
    } else {
        Modifier
    }
    val bordered = when (theme.shapes.button) {
        ButtonStyle.PIXEL_BLOCK -> Modifier.border(BorderStroke(2.dp, theme.colors.primary), shape)
        ButtonStyle.NEON_GLOW -> Modifier.border(BorderStroke(1.dp, theme.colors.primary.copy(alpha = 0.4f)), shape)
        else -> Modifier
    }
    Column(
        modifier
            .then(elevated)
            .clip(shape)
            .background(theme.colors.surface, shape)
            .then(bordered)
            .padding(16.dp),
        content = content,
    )
}

/** The theme's headline treatment — display font, heavy, primary-text color. */
@Composable
fun GameTitle(text: String, modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current
    Text(
        text,
        modifier = modifier,
        color = theme.colors.textPrimary,
        fontFamily = theme.type.display,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
    )
}

/** Primary action button, rendered in the active theme's button language. */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val theme = LocalGameTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val face = if (enabled) theme.colors.primary else theme.colors.primary.copy(alpha = 0.4f)

    val label = @Composable {
        Text(
            text,
            color = theme.colors.onPrimary,
            fontFamily = theme.type.hud,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
        )
    }

    when (theme.shapes.button) {
        ButtonStyle.PRESSABLE_3D -> {
            val shape = RoundedCornerShape(theme.shapes.controlCorner)
            val lip = theme.colors.primary.darken(0.22f)
            Box(modifier.background(lip, shape)) {
                Box(
                    Modifier
                        .offset(y = if (pressed) 0.dp else (-5).dp)
                        .clip(shape)
                        .background(face, shape)
                        .clickable(interaction, null, enabled = enabled) { onClick() }
                        .padding(horizontal = 22.dp, vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) { label() }
            }
        }

        ButtonStyle.NEON_GLOW -> {
            val shape = RoundedCornerShape(theme.shapes.controlCorner)
            Box(
                modifier
                    .border(BorderStroke(6.dp, face.copy(alpha = 0.18f)), shape)
                    .border(BorderStroke(2.dp, face.copy(alpha = 0.5f)), shape)
                    .clip(shape)
                    .background(face, shape)
                    .clickable(interaction, null, enabled = enabled) { onClick() }
                    .padding(horizontal = 22.dp, vertical = 13.dp),
                contentAlignment = Alignment.Center,
            ) { label() }
        }

        ButtonStyle.PIXEL_BLOCK -> {
            val shape = RoundedCornerShape(0.dp)
            Box(
                modifier
                    .border(BorderStroke(2.dp, theme.colors.onPrimary), shape)
                    .background(if (pressed) face.darken(0.15f) else face, shape)
                    .clickable(interaction, null, enabled = enabled) { onClick() }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { label() }
        }
    }
}

private fun androidx.compose.ui.graphics.Color.darken(amount: Float) =
    androidx.compose.ui.graphics.Color(
        red = (red * (1 - amount)).coerceIn(0f, 1f),
        green = (green * (1 - amount)).coerceIn(0f, 1f),
        blue = (blue * (1 - amount)).coerceIn(0f, 1f),
        alpha = alpha,
    )

@Composable
private fun PreviewKit(theme: GameTheme) {
    CompositionLocalProvider(LocalGameTheme provides theme) {
        GameBackground {
            Column(Modifier.padding(20.dp)) {
                GameTitle(theme.name)
                Box(Modifier.padding(top = 12.dp)) {
                    GameCard {
                        Text(
                            "It was the best of times.",
                            color = theme.colors.textPrimary,
                            fontFamily = theme.type.body,
                        )
                    }
                }
                Box(Modifier.padding(top = 12.dp)) {
                    GameButton("Fix it", onClick = {})
                }
            }
        }
    }
}

@Preview(name = "Playful", widthDp = 300, heightDp = 260)
@Composable
private fun PreviewPlayful() = PreviewKit(GameThemes.PLAYFUL)

@Preview(name = "Neon", widthDp = 300, heightDp = 260)
@Composable
private fun PreviewNeon() = PreviewKit(GameThemes.NEON)

@Preview(name = "Retro", widthDp = 300, heightDp = 260)
@Composable
private fun PreviewRetro() = PreviewKit(GameThemes.RETRO)
