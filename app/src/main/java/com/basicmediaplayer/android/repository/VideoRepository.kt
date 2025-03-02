package com.basicmediaplayer.android.repository

import com.basicmediaplayer.android.data.network.ApiService
import javax.inject.Inject

class VideoRepository @Inject constructor(private val apiService: ApiService) {
    suspend fun fetchVideoUrl() = apiService.getVideoUrl()
}
