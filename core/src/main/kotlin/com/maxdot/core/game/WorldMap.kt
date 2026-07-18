package com.maxdot.core.game

/** A node on a book's world map. */
enum class NodeKind { LEVEL, BOSS }
enum class NodeState { DONE, CURRENT, LOCKED }

data class MapNodeModel(
    val index: Int,
    val kind: NodeKind,
    val state: NodeState,
)

/**
 * Turns a book's linear reading progress into a finite winding path of level
 * nodes. The game still streams passages in order; the map is a progress
 * visualization laid over that, with a boss node every [BOSS_INTERVAL] steps and
 * always at the end.
 */
object WorldMap {

    const val BOSS_INTERVAL = 5
    private const val SENTENCES_PER_NODE = 6
    private const val MIN_NODES = 8
    private const val MAX_NODES = 30

    /** Number of nodes representing a book of [sentenceCount] sentences. */
    fun nodeCount(sentenceCount: Int): Int =
        (sentenceCount / SENTENCES_PER_NODE).coerceIn(MIN_NODES, MAX_NODES)

    /** Index of the node the player is currently on (0-based). */
    fun currentIndex(percent: Int, nodeCount: Int): Int {
        if (nodeCount <= 0) return 0
        val raw = (percent.coerceIn(0, 100) / 100f * nodeCount).toInt()
        return raw.coerceIn(0, nodeCount - 1)
    }

    fun isBoss(index: Int, nodeCount: Int): Boolean =
        (index + 1) % BOSS_INTERVAL == 0 || index == nodeCount - 1

    /** The full node list with per-node state derived from [percent]. */
    fun nodes(percent: Int, nodeCount: Int): List<MapNodeModel> {
        if (nodeCount <= 0) return emptyList()
        val finished = percent >= 100
        val current = currentIndex(percent, nodeCount)
        return (0 until nodeCount).map { i ->
            val kind = if (isBoss(i, nodeCount)) NodeKind.BOSS else NodeKind.LEVEL
            val state = when {
                finished -> NodeState.DONE
                i < current -> NodeState.DONE
                i == current -> NodeState.CURRENT
                else -> NodeState.LOCKED
            }
            MapNodeModel(i, kind, state)
        }
    }
}
