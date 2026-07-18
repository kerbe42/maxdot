package com.maxdot.core

import com.maxdot.core.game.NodeKind
import com.maxdot.core.game.NodeState
import com.maxdot.core.game.WorldMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldMapTest {

    @Test
    fun nodeCountIsClamped() {
        assertEquals(8, WorldMap.nodeCount(0))
        assertEquals(8, WorldMap.nodeCount(30))
        assertEquals(30, WorldMap.nodeCount(100_000))
        assertTrue(WorldMap.nodeCount(120) in 8..30)
    }

    @Test
    fun currentIndexTracksPercent() {
        assertEquals(0, WorldMap.currentIndex(0, 10))
        assertEquals(5, WorldMap.currentIndex(50, 10))
        assertEquals(9, WorldMap.currentIndex(100, 10)) // clamped to last
    }

    @Test
    fun bossEveryFifthAndAtEnd() {
        assertTrue(WorldMap.isBoss(4, 12))   // 5th node
        assertTrue(WorldMap.isBoss(9, 12))   // 10th node
        assertTrue(WorldMap.isBoss(11, 12))  // last node
        assertTrue(!WorldMap.isBoss(0, 12))
        assertTrue(!WorldMap.isBoss(3, 12))
    }

    @Test
    fun statesSplitAroundCurrent() {
        val nodes = WorldMap.nodes(percent = 40, nodeCount = 10) // current = 4
        assertEquals(NodeState.DONE, nodes[0].state)
        assertEquals(NodeState.DONE, nodes[3].state)
        assertEquals(NodeState.CURRENT, nodes[4].state)
        assertEquals(NodeState.LOCKED, nodes[5].state)
        assertEquals(10, nodes.size)
    }

    @Test
    fun finishedBookIsAllDone() {
        val nodes = WorldMap.nodes(percent = 100, nodeCount = 10)
        assertTrue(nodes.all { it.state == NodeState.DONE })
    }

    @Test
    fun lastNodeIsBossKind() {
        val nodes = WorldMap.nodes(percent = 0, nodeCount = 12)
        assertEquals(NodeKind.BOSS, nodes.last().kind)
    }
}
