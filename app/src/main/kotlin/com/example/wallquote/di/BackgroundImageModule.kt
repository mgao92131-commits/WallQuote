package com.example.wallquote.di

import com.example.wallquote.wallpaper.background.BackgroundBitmapCache
import com.example.wallquote.wallpaper.background.BackgroundImageLoader
import com.example.wallquote.wallpaper.background.BackgroundImageProcessor
import com.example.wallquote.wallpaper.background.BackgroundMemoryController
import com.example.wallquote.wallpaper.background.DefaultBackgroundImageLoader
import com.example.wallquote.wallpaper.background.DefaultBackgroundImageProcessor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BackgroundCacheModule {
    @Provides
    @Singleton
    fun provideBackgroundBitmapCache(): BackgroundBitmapCache = BackgroundBitmapCache()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BackgroundImageModule {
    @Binds
    @Singleton
    abstract fun bindProcessor(impl: DefaultBackgroundImageProcessor): BackgroundImageProcessor

    @Binds
    @Singleton
    abstract fun bindLoader(impl: DefaultBackgroundImageLoader): BackgroundImageLoader

    @Binds
    @Singleton
    abstract fun bindMemoryController(impl: BackgroundBitmapCache): BackgroundMemoryController
}
