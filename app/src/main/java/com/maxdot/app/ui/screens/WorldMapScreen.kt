package com.maxdot.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.maxdot.app.MainViewModel
import com.maxdot.app.data.Book
import com.maxdot.app.ui.theme.LocalGameTheme
import com.maxdot.app.ui.theme.NodeShape
import com.maxdot.core.game.MapNodeModel
import com.maxdot.core.game.NodeKind
import com.maxdot.core.game.NodeState
import com.maxdot.core.game.WorldMap

@Composable
fun WorldMapScreen(
    book: Book,
    mainViewModel: MainViewModel,
    onPlay: () -> Unit,
    onBack: () -> Unit,
) {
    val theme = LocalGameTheme.current
    val progress = mainViewModel.books.progress(book.id)
    val nodeCount = WorldMap.nodeCount(progress.sentenceCount)
    val nodes = WorldMap.nodes(progress.percent, nodeCount)

    Scaffold(containerColor = Color.Transparent) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.colors.textPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        book.title,
                        color = theme.colors.textPrimary,
                        fontFamily = theme.type.display,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        maxLines = 1,
                    )
                    Text(
                        "${progress.percent}% cleared",
                        color = theme.colors.textSecondary,
                        fontFamily = theme.type.hud,
                        fontSize = 12.sp,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            ) {
                itemsIndexed(nodes) { i, node ->
                    WorldMapRow(
                        node = node,
                        nudgeRight = i % 2 == 0,
                        onClick = if (node.state == NodeState.CURRENT) onPlay else null,
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun WorldMapRow(
    node: MapNodeModel,
    nudgeRight: Boolean,
    onClick: (() -> Unit)?,
) {
    val theme = LocalGameTheme.current
    val spineColor = if (node.state == NodeState.DONE) theme.colors.primary
    else theme.colors.surfaceAlt
    Box(
        modifier = Modifier.fillMaxWidth().height(if (node.kind == NodeKind.BOSS) 108.dp else 92.dp),
        contentAlignment = Alignment.Center,
    ) {
        // spine segment behind the node
        Box(
            Modifier
                .width(5.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(3.dp))
                .background(spineColor),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MapNode(
                node = node,
                onClick = onClick,
                modifier = Modifier.offset(x = if (nudgeRight) 26.dp else (-26).dp),
            )
            if (node.state == NodeState.CURRENT) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (node.kind == NodeKind.BOSS) "BOSS" else "PLAY",
                    modifier = Modifier.offset(x = if (nudgeRight) 26.dp else (-26).dp),
                    color = theme.colors.accent,
                    fontFamily = theme.type.hud,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
fun MapNode(
    node: MapNodeModel,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val theme = LocalGameTheme.current
    val boss = node.kind == NodeKind.BOSS
    val sizeDp = if (boss) 66.dp else 54.dp

    val fill = when (node.state) {
        NodeState.DONE -> theme.colors.primary
        NodeState.CURRENT -> if (boss) theme.colors.hearts else theme.colors.accent
        NodeState.LOCKED -> theme.colors.surfaceAlt
    }
    val content = when (node.state) {
        NodeState.LOCKED -> theme.colors.textSecondary
        else -> theme.colors.onPrimary
    }
    val icon: ImageVector = when {
        node.state == NodeState.LOCKED -> Icons.Filled.Lock
        boss -> Icons.Filled.Whatshot
        node.state == NodeState.DONE -> Icons.Filled.Check
        else -> Icons.Filled.PlayArrow
    }

    // Pulse the current node.
    val scale = if (node.state == NodeState.CURRENT) {
        val t = rememberInfiniteTransition(label = "nodePulse")
        val s by t.animateFloat(
            initialValue = 1f,
            targetValue = 1.09f,
            animationSpec = infiniteRepeatable(tween(720), RepeatMode.Reverse),
            label = "nodeScale",
        )
        s
    } else {
        1f
    }

    val diamond = theme.shapes.node == NodeShape.DIAMOND
    val shape = when (theme.shapes.node) {
        NodeShape.SQUIRCLE -> RoundedCornerShape(30)
        NodeShape.GLOW_CIRCLE -> CircleShape
        NodeShape.DIAMOND -> RectangleShape
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val glow = theme.colors.glow && node.state != NodeState.LOCKED
        Box(
            Modifier
                .size(sizeDp)
                .then(if (diamond) Modifier.rotate(45f) else Modifier)
                .clip(shape)
                .background(fill, shape)
                .then(if (glow) Modifier.border(BorderStroke(2.dp, fill), shape) else Modifier)
                .then(if (theme.shapes.node == NodeShape.DIAMOND) Modifier.border(BorderStroke(2.dp, content.copy(alpha = 0.5f)), shape) else Modifier),
        )
        Icon(
            icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(if (boss) 30.dp else 24.dp),
        )
    }
}
