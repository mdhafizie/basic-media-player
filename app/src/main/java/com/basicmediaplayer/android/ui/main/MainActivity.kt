package com.basicmediaplayer.android.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.basicmediaplayer.android.R
import com.basicmediaplayer.android.databinding.ActivityMainBinding
import com.basicmediaplayer.android.ui.player.PlayerManager
import com.basicmediaplayer.android.ui.player.VideoPlayerActivity
import com.basicmediaplayer.android.util.AppUtils
import com.basicmediaplayer.android.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val videoViewModel: MainViewModel by viewModels()
    @Inject
    lateinit var playerManager: PlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure fresh launch
        if (!isTaskRoot) {
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        playerManager.player.playWhenReady = false // Ensure playWhenReady is reset

        // Observe video URL once and update UI accordingly
        videoViewModel.getVideoUrl().observe(this) { videoUrl ->
            binding.btnPlay.setOnClickListener {
                if (!AppUtils.checkInternetAndShowToast(this)) return@setOnClickListener

                if (!AppUtils.isValidUrl(videoUrl)) {
                    AppUtils.showToast(this, "Invalid video URL format")
                    return@setOnClickListener
                }

                AppUtils.checkUrlReachable(videoUrl) { isReachable ->
                    if (isReachable) {
                        val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                            putExtra(Constants.VIDEO_URL_KEY, videoUrl)
                            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                    } else {
                        AppUtils.showToast(this, "Invalid or unreachable video URL")
                    }
                }
            }
        }
    }
    override fun onBackPressed() {
        super.onBackPressed()
        stopPlayerAndFinish()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayerAndFinish()
    }
    private fun stopPlayerAndFinish() {
        playerManager.stopPlayback()
        playerManager.releasePlayer() // Ensure complete cleanup
        finish()
    }
}
