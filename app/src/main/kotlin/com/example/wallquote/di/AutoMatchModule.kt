package com.example.wallquote.di

import com.example.wallquote.automatch.BackgroundSampler
import com.example.wallquote.automatch.DefaultBackgroundSampler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AutoMatchModule {
    @Binds
    @Singleton
    abstract fun bindBackgroundSampler(impl: DefaultBackgroundSampler): BackgroundSampler
}
