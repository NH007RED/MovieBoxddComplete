package com.moviebox.app.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import java.util.Locale

/**
 * Native ExoPlayer builder with FFmpeg extension renderer fallback (AC-3, E-AC-3, DTS, TrueHD, FLAC, Opus),
 * AudioDelayProcessor sink, and multi-channel audio track support.
 * Ported from VideoPlayerOffline v1.4.
 */
@UnstableApi
object PlayerHolder {
    val audioDelay = AudioDelayProcessor()

    val SPEEDS = floatArrayOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val SPEED_LABELS = listOf("0.25x", "0.5x", "0.75x", "1.0x (Normal)", "1.25x", "1.5x", "2.0x")
    val ASPECT_LABELS = listOf("Fit", "Stretch", "Crop (Zoom)", "16:9", "4:3")
    val SUBTITLE_COLORS = intArrayOf(
        android.graphics.Color.WHITE,
        android.graphics.Color.YELLOW,
        android.graphics.Color.CYAN,
        android.graphics.Color.rgb(255, 165, 0)
    )
    val SUBTITLE_COLOR_NAMES = listOf("White", "Yellow", "Cyan", "Orange")

    fun createPlayer(context: Context): ExoPlayer {
        val app = context.applicationContext
        val factory = object : DefaultRenderersFactory(app) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(false)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessors(arrayOf<AudioProcessor>(audioDelay))
                .build()
        }
            // ON = use hardware decoders first; fall back to Jellyfin FFmpeg software decoder
            // (AC-3 / E-AC-3 / DTS / TrueHD / FLAC / Opus)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .setEnableDecoderFallback(true)

        val player = ExoPlayer.Builder(app, factory)
            .setAudioAttributes(AudioAttributes.DEFAULT, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        (player.trackSelector as? DefaultTrackSelector)?.let { ts ->
            ts.setParameters(
                ts.buildUponParameters()
                    .setConstrainAudioChannelCountToDeviceCapabilities(false)
            )
        }
        return player
    }

    fun trackLabel(f: Format, n: Int, kind: String): String {
        val lang = f.language?.takeIf { it != "und" }?.let { Locale(it).displayLanguage }
        val base = f.label ?: lang ?: "$kind $n"
        val extra = if (f.channelCount > 0) {
            when (f.channelCount) {
                1 -> " • Mono"
                2 -> " • Stereo"
                6 -> " • 5.1"
                8 -> " • 7.1"
                else -> " • ${f.channelCount}ch"
            }
        } else ""
        return base + extra
    }

    fun codecName(mime: String?): String = when (mime) {
        MimeTypes.AUDIO_AAC -> "AAC"
        MimeTypes.AUDIO_AC3 -> "AC-3"
        MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC-3"
        MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD, MimeTypes.AUDIO_DTS_EXPRESS -> "DTS"
        MimeTypes.AUDIO_MPEG -> "MP3"
        MimeTypes.AUDIO_OPUS -> "Opus"
        MimeTypes.AUDIO_VORBIS -> "Vorbis"
        MimeTypes.AUDIO_FLAC -> "FLAC"
        MimeTypes.AUDIO_TRUEHD -> "TrueHD"
        null -> ""
        else -> mime.substringAfter('/').uppercase(Locale.US)
    }
}
