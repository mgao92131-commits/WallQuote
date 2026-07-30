package com.example.wallquote.data.di

import android.content.Context
import androidx.room.Room
import com.example.wallquote.data.local.AppDatabase
import com.example.wallquote.data.local.CollectionDao
import com.example.wallquote.data.repository.CollectionRepositoryImpl
import com.example.wallquote.domain.repository.CollectionRepository
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
        Room.databaseBuilder(context, AppDatabase::class.java, "wallquote.db").build()

    @Provides
    fun provideCollectionDao(db: AppDatabase): CollectionDao = db.collectionDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository
}
