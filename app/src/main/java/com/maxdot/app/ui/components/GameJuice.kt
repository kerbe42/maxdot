package com.maxdot.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.maxdot.app.ui.theme.LocalGameTheme
import com.maxdot.core.game.Combo
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The live combo meter: a big multiplier that pumps up when the streak crosses
 * into a higher tier and shakes when a wrong answer breaks it back to x1.
 */
@Composable
fun ComboMeter(streak: Int, modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current
    val mult = Combo.multiplier(streak)
    val active = mult > 1
    val meterColor = if (active) theme.colors.accent else theme.colors.textSecondary

    var prevStreak by remember { mutableStateOf(streak) }
    val scale = remember { Animatable(1f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(streak) {
        if (Combo.isTierUp(prevStreak, streak)) {
            scale.snapTo(1f)
            scale.animateTo(1.3f, tween(110))
            scale.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = Spring.StiffnessMediumLow))
        }
        if (prevStreak > 0 && streak == 0) {
            shake.snapTo(0f)
            repeat(3) {
                shake.animateTo(1f, tween(45))
                shake.animateTo(-1f, tween(45))
            }
            shake.animateTo(0f, tween(45))
        }
        prevStreak = streak
    }
    val fill by animateFloatAsState(Combo.progress(streak), tween(320), label = "comboFill")

    Column(modifier.offset(x = (shake.value * 4).dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "COMBO",
                color = theme.colors.textSecondary,
                fontFamily = theme.type.hud,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                Combo.label(streak),
                modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
                color = meterColor,
                fontFamily = theme.type.hud,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(theme.colors.surface),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fill)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (active) theme.colors.accent else theme.colors.primary),
            )
        }
    }
}

/** The themed play HUD: combo meter as the hero, plus streak, hints, and book progress. */
@Composable
fun GameHud(
    streak: Int,
    hints: Int,
    bookPercent: Int,
    modifier: Modifier = Modifier,
) {
    val theme = LocalGameTheme.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.controlCorner))
            .background(theme.colors.surfaceAlt)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ComboMeter(streak, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        HudStat(
            icon = { Icon(Icons.Filled.LocalFireDepartment, contentDescription = "Streak", tint = if (streak > 0) theme.colors.accent else theme.colors.textSecondary, modifier = Modifier.size(18.dp)) },
            value = "$streak",
        )
        Spacer(Modifier.width(12.dp))
        HudStat(
            icon = { Icon(Icons.Filled.Lightbulb, contentDescription = "Hints", tint = theme.colors.warning, modifier = Modifier.size(18.dp)) },
            value = "$hints",
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "$bookPercent%",
            color = theme.colors.textSecondary,
            fontFamily = theme.type.hud,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun HudStat(icon: @Composable () -> Unit, value: String) {
    val theme = LocalGameTheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(3.dp))
        Text(
            value,
            color = theme.colors.textPrimary,
            fontFamily = theme.type.hud,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

/** A "+N  xM" that floats upward and fades — call inside an overlay Box. */
@Composable
fun FloatingXp(xp: Int, multiplier: Int, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current
    val y = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        launch { y.animateTo(-64f, tween(900)) }
        alpha.animateTo(0f, tween(900, easing = LinearEasing))
        onDone()
    }
    Text(
        buildString {
            append("+")
            append(xp)
            if (multiplier > 1) append("  x$multiplier")
        },
        modifier = modifier
            .offset(y = y.value.dp)
            .graphicsLayer { this.alpha = alpha.value },
        color = theme.colors.success,
        fontFamily = theme.type.hud,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp,
    )
}

private data class Confetto(
    val x: Float,
    val startY: Float,
    val color: Color,
    val size: Float,
    val speed: Float,
    val sway: Float,
)

/** A one-shot confetti burst that rains down and settles. */
@Composable
fun Confetti(colors: List<Color>, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(1900, easing = LinearEasing)) }
    val pieces = remember {
        List(70) {
            Confetto(
                x = Random.nextFloat(),
                startY = -Random.nextFloat() * 0.25f,
                color = colors[Random.nextInt(colors.size)],
                size = 6f + Random.nextFloat() * 8f,
                speed = 0.7f + Random.nextFloat() * 0.9f,
                sway = Random.nextFloat() * 12f,
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        pieces.forEach { c ->
            val y = (c.startY + p * c.speed * 1.4f) * size.height
            val x = c.x * size.width + sin(p * 6.283f + c.x * 10f) * c.sway
            if (y < size.height + 20f) {
                drawRect(color = c.color, topLeft = Offset(x, y), size = Size(c.size, c.size * 1.6f))
            }
        }
    }
}

/** Full-screen level-up celebration with confetti; auto-dismisses. */
@Composable
fun LevelUpOverlay(level: Int, onDismiss: () -> Unit) {
    val theme = LocalGameTheme.current
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
        delay(2000)
        onDismiss()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(remember { MutableInteractionSource() }, null) { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Confetti(listOf(theme.colors.primary, theme.colors.accent, theme.colors.success, theme.colors.hearts))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Bolt, contentDescription = null, tint = theme.colors.accent, modifier = Modifier.size(40.dp))
                Text(
                    "LEVEL UP",
                    color = theme.colors.accent,
                    fontFamily = theme.type.display,
                    fontWeight = FontWeight.Black,
                    fontSize = 38.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "You reached level $level",
                color = Color.White,
                fontFamily = theme.type.hud,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "+1 hint",
                color = theme.colors.success,
                fontFamily = theme.type.hud,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}

/** Boss-fight banner with a depleting health bar (remaining hits to land). */
@Composable
fun BossBanner(remaining: Int, total: Int, modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current
    val frac = if (total <= 0) 0f else (remaining.toFloat() / total).coerceIn(0f, 1f)
    val animFrac by animateFloatAsState(frac, tween(400), label = "bossHp")
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.controlCorner))
            .background(theme.colors.surfaceAlt)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Whatshot, contentDescription = null, tint = theme.colors.hearts, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "BOSS FIGHT",
                color = theme.colors.hearts,
                fontFamily = theme.type.hud,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.surface),
            ) {
                Box(
                    Modifier.fillMaxWidth(animFrac).height(9.dp).clip(RoundedCornerShape(4.dp)).background(theme.colors.hearts),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "$remaining",
            color = theme.colors.hearts,
            fontFamily = theme.type.hud,
            fontWeight = FontWeight.Black,
            fontSize = 16.sp,
        )
    }
}

/** Blitz HUD: lives, a countdown, live score, and the combo meter. */
@Composable
fun BlitzHud(
    streak: Int,
    hearts: Int,
    maxHearts: Int,
    secondsLeft: Int,
    score: Int,
    modifier: Modifier = Modifier,
) {
    val theme = LocalGameTheme.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.controlCorner))
            .background(theme.colors.surfaceAlt)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) {
                repeat(maxHearts) { i ->
                    Icon(
                        if (i < hearts) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = theme.colors.hearts,
                        modifier = Modifier.size(20.dp).padding(end = 2.dp),
                    )
                }
            }
            Icon(
                Icons.Filled.Timer,
                contentDescription = null,
                tint = if (secondsLeft <= 10) theme.colors.hearts else theme.colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "${secondsLeft}s",
                color = if (secondsLeft <= 10) theme.colors.hearts else theme.colors.textPrimary,
                fontFamily = theme.type.hud,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                "$score",
                color = theme.colors.accent,
                fontFamily = theme.type.hud,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        ComboMeter(streak)
    }
}

/** End-of-run Blitz summary. */
@Composable
fun BlitzOverCard(score: Int, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.shapes.cardCorner))
            .background(theme.colors.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "BLITZ OVER",
            color = theme.colors.accent,
            fontFamily = theme.type.display,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Score",
            color = theme.colors.textSecondary,
            fontFamily = theme.type.hud,
            fontSize = 13.sp,
        )
        Text(
            "$score",
            color = theme.colors.textPrimary,
            fontFamily = theme.type.display,
            fontWeight = FontWeight.Black,
            fontSize = 40.sp,
        )
        Spacer(Modifier.height(16.dp))
        GameButton("Back to map", onClick = onExit)
    }
}

/** A compact confetti + stamp for a perfect passage; sits behind the summary card. */
@Composable
fun PerfectBurst(modifier: Modifier = Modifier) {
    val theme = LocalGameTheme.current
    Box(modifier.fillMaxWidth().height(80.dp)) {
        Confetti(
            listOf(theme.colors.primary, theme.colors.accent, theme.colors.success),
            Modifier.fillMaxHeight(),
        )
    }
}
