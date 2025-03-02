package com.basicmediaplayer.android.data.network

import com.basicmediaplayer.android.data.model.VideoModel
import com.basicmediaplayer.android.util.Constants
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET(Constants.VIDEO_URL_ENDPOINT)
    suspend fun getVideoUrl(): Response<VideoModel>
}