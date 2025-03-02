package com.basicmediaplayer.android.ui.player

import android.content.Context
import android.media.AudioManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _player: ExoPlayer? = null
    private var audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusGranted = false

    val player: ExoPlayer
        get() {
            if (_player == null) {
                _player = ExoPlayer.Builder(context).build().apply {
                    repeatMode = ExoPlayer.REPEAT_MODE_OFF
                }
            }
            return _player!!
        }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Another app like Instagram/Facebook starts playing audio
                stopPlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporarily lost focus (pause)
                _player?.pause()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained focus (resume play)
                _player?.play()
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
        audioFocusGranted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return audioFocusGranted
    }

    private fun releaseAudioFocus() {
        if (audioFocusGranted) {
            audioManager.abandonAudioFocus(audioFocusChangeListener)
            audioFocusGranted = false
        }
    }

    fun playVideo(videoUrl: String) {
        if (!requestAudioFocus()) return // Stop if focus is not granted

        _player?.apply {
            stopPlayback() // Ensure any ongoing playback is stopped
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
        }
        releaseAudioFocus()
    }

    fun stopPlayback() {
        _player?.apply {
            stop()
            clearMediaItems()
        }
    }

    fun releasePlayer() {
        _player?.release()
        _player = null
    }
}

