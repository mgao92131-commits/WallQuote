package com.example.wallquote.data.di

import android.content.Context
import androidx.room.Room
import com.example.wallquote.data.background.FileBackgroundAssetStore
import com.example.wallquote.data.local.AppDatabase
import com.example.wallquote.data.local.CollectionDao
import com.example.wallquote.data.local.CustomStyleDao
import com.example.wallquote.data.repository.CollectionRepositoryImpl
import com.example.wallquote.data.repository.CustomStyleRepositoryImpl
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.repository.CollectionRepository
import com.example.wallquote.domain.repository.CustomStyleRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "wallquote.db")
            // Unreleased app: drop and recreate on schema change. Real migrations start after first release.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideCollectionDao(db: AppDatabase): CollectionDao = db.collectionDao()

    @Provides
    fun provideCustomStyleDao(db: AppDatabase): CustomStyleDao = db.customStyleDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository

    @Binds
    @Singleton
    abstract fun bindCustomStyleRepository(impl: CustomStyleRepositoryImpl): CustomStyleRepository

    @Binds
    @Singleton
    abstract fun bindBackgroundAssetStore(impl: FileBackgroundAssetStore): BackgroundAssetStore
}
