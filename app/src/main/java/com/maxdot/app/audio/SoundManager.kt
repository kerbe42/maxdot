package com.maxdot.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import com.maxdot.app.R

/** The game's sound effects, backed by bundled synth clips in res/raw. */
enum class Sfx(val res: Int) {
    TAP(R.raw.sfx_tap),
    CORRECT(R.raw.sfx_correct),
    WRONG(R.raw.sfx_wrong),
    COMBO(R.raw.sfx_combo),
    LEVEL_UP(R.raw.sfx_levelup),
    BOSS_WIN(R.raw.sfx_bosswin),
}

/** No-op sound interface so previews and un-wired hosts can ignore audio. */
interface SoundPlayer {
    fun play(sfx: Sfx)
}

/** Plays short SFX through a [SoundPool]. Cheap to construct; release when done. */
class SoundManager(context: Context) : SoundPlayer {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids: Map<Sfx, Int> =
        Sfx.entries.associateWith { pool.load(context.applicationContext, it.res, 1) }

    /** Mirrors the user's SFX setting; when false, [play] is a no-op. */
    @Volatile
    var enabled: Boolean = true

    override fun play(sfx: Sfx) {
        if (!enabled) return
        ids[sfx]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    fun release() = pool.release()
}

val LocalSound = staticCompositionLocalOf<SoundPlayer> {
    object : SoundPlayer {
        override fun play(sfx: Sfx) {}
    }
}
