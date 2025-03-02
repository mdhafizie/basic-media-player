package com.basicmediaplayer.android.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.basicmediaplayer.android.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: VideoRepository
) : ViewModel() {

    fun getVideoUrl() = liveData(Dispatchers.IO) {
        try {
            val response = repository.fetchVideoUrl()
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(it.url)
                }
            } else {
                emit("Error fetching video URL")
            }
        } catch (e: Exception) {
            emit("Network Error: ${e.message}")
        }
    }
}
