package com.basicmediaplayer.android.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageButton
import com.basicmediaplayer.android.R
import com.basicmediaplayer.android.databinding.PipVideoLayoutBinding
import com.basicmediaplayer.android.ui.player.PlayerManager
import com.basicmediaplayer.android.util.Constants
import com.google.android.exoplayer2.ui.PlayerView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FloatingVideoService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var playerView: PlayerView
    private lateinit var closeButton: ImageButton

    @Inject
    lateinit var playerManager: PlayerManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoUrl = intent?.getStringExtra(Constants.VIDEO_URL_KEY)
            ?: run {
                Log.e("FloatingVideoService", "Video URL not provided")
                stopSelf()
                return START_NOT_STICKY
            }

        try {
            setupFloatingWindow()
            playVideo(videoUrl)
        } catch (e: Exception) {
            Log.e("FloatingVideoService", "Error setting up floating window or playing video", e)
            stopSelf()
        }

        return START_STICKY
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
            ?: throw IllegalStateException("WindowManager not available")

        val binding = PipVideoLayoutBinding.inflate(LayoutInflater.from(this))
        floatingView = binding.root

        playerView = floatingView.findViewById(R.id.pipVideoView)
        closeButton = floatingView.findViewById(R.id.btnClosePiP)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.CENTER
        windowManager.addView(floatingView, layoutParams)

        closeButton.setOnClickListener {
            stopPlayerAndCloseUi()
        }

        enableDragging(layoutParams)
    }

    private fun playVideo(videoUrl: String) {
        try {
            playerView.player = playerManager.player // Attach the player to the PlayerView
            playerManager.playVideo(videoUrl)
        } catch (e: Exception) {
            Log.e("FloatingVideoService", "Error playing video", e)
            stopPlayerAndCloseUi()
        }
    }

    private fun enableDragging(layoutParams: WindowManager.LayoutParams) {
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                return when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - touchX).toInt()
                        layoutParams.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        true
                    }
                    else -> false
                }
            }
        })
    }

    private fun stopPlayerAndCloseUi() {
        try {
            playerView.player = null // Detach player from PlayerView
            playerManager.stopPlayback() // Ensure playback stops

            // Explicitly release the player
            playerManager.player?.release()
            if (::floatingView.isInitialized && floatingView.windowToken != null) {
                windowManager.removeView(floatingView) // Remove the floating view
            }
        } catch (e: Exception) {
            Log.e("FloatingVideoService", "Error stopping player or closing UI", e)
        } finally {
            stopSelf() // Stop the service
        }
    }

    override fun onDestroy() {
        stopPlayerAndCloseUi()
        stopSelf()
        super.onDestroy()
    }

}
