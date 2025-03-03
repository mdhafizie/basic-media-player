package com.basicmediaplayer.android.ui.player

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.basicmediaplayer.android.databinding.ActivityVideoPlayerBinding
import com.basicmediaplayer.android.service.FloatingVideoService
import com.basicmediaplayer.android.util.AppUtils.isValidUrl
import com.basicmediaplayer.android.util.AppUtils.showToast
import com.basicmediaplayer.android.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private var videoUrl: String? = null

    @Inject
    lateinit var playerManager: PlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoUrl = intent.getStringExtra(Constants.VIDEO_URL_KEY)
        if (videoUrl.isNullOrBlank() || !isValidUrl(videoUrl!!)) {
            showToast(this,"Invalid video URL")
            finish()
            return
        }

        if (videoUrl != null) {
            setupPlayer(videoUrl)
        }

        binding.btnPipMode.setOnClickListener {
            enterPiPMode()
        }
    }

    private fun setupPlayer(videoUrl: String?) {
        binding.videoView.player = playerManager.player
        if (!videoUrl.isNullOrEmpty()) {
            playerManager.playVideo(videoUrl)
        }
    }

    // Enter PiP mode
    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!isInPictureInPictureMode) {  // Prevent entering PiP multiple times
                val pipParams = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(pipParams)
            }
        } else {
            startFloatingVideoService()
        }
    }

    private fun startFloatingVideoService() {
        if (videoUrl != null) {
            val intent = Intent(this, FloatingVideoService::class.java).apply {
                putExtra(Constants.VIDEO_URL_KEY, videoUrl)
            }
            startService(intent)
        }
    }

    // Handle screen rotation and PiP mode restoration
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // If in landscape, enter PiP mode automatically
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            enterPiPMode()
        }

        // Restore full-screen UI when coming back from PiP mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode) {
            binding.videoView.useController = false
            binding.btnPipMode.visibility = View.GONE
        } else {
            binding.videoView.useController = true
            binding.btnPipMode.visibility = View.VISIBLE
        }
    }

    @Deprecated("Deprecated in android.app.Activity")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            binding.videoView.useController = false
            binding.btnPipMode.visibility = View.GONE
        } else {
            binding.videoView.useController = true
            binding.btnPipMode.visibility = View.VISIBLE

            // Resume video playback when exiting PiP
            if (!playerManager.player.isPlaying) {
                playerManager.player.playWhenReady = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume playback when the user returns to the app
        if (!playerManager.player.isPlaying) {
            playerManager.player.playWhenReady = true
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Pause the video when the user presses the Home button
        //playerManager.player.playWhenReady = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPiPMode()
        } else {
            playerManager.player.playWhenReady = false // Pause playback on older devices
        }
    }

    override fun onStop() {
        super.onStop()
        // If the app is not in the foreground but still running, don't stop playback
        if (!isChangingConfigurations && !isInPictureInPictureMode) {
            playerManager.player.playWhenReady = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            playerManager.stopPlayback() // Stop the player but don't release it
        }
    }
}