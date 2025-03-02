package com.basicmediaplayer.android.data.network

import android.app.Application
import android.content.Context
import com.basicmediaplayer.android.repository.VideoRepository
import com.basicmediaplayer.android.util.Constants
import com.google.android.exoplayer2.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRetrofit(): ApiService {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVideoRepository(apiService: ApiService): VideoRepository {
        return VideoRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideExoPlayer(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context).build()
    }

    @Provides
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }
}
