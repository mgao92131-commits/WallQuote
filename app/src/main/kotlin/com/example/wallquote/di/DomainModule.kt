package com.example.wallquote.di

import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.repository.CollectionRepository
import com.example.wallquote.domain.usecase.CleanupOrphanBackgroundAssetsUseCase
import com.example.wallquote.domain.usecase.DeleteCollectionUseCase
import com.example.wallquote.domain.usecase.GetCollectionUseCase
import com.example.wallquote.domain.usecase.ObserveOrderedCollectionsUseCase
import com.example.wallquote.domain.usecase.RenameCollectionUseCase
import com.example.wallquote.domain.usecase.SaveCollectionUseCase
import com.example.wallquote.domain.usecase.SaveCollectionWithBackgroundUseCase
import com.example.wallquote.domain.usecase.SelectActiveCollectionsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideObserveOrderedCollectionsUseCase(
        repository: CollectionRepository,
    ): ObserveOrderedCollectionsUseCase = ObserveOrderedCollectionsUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCollectionUseCase(repository: CollectionRepository): GetCollectionUseCase =
        GetCollectionUseCase(repository)

    @Provides
    @Singleton
    fun provideSaveCollectionUseCase(repository: CollectionRepository): SaveCollectionUseCase =
        SaveCollectionUseCase(repository)

    @Provides
    @Singleton
    fun provideSaveCollectionWithBackgroundUseCase(
        repository: CollectionRepository,
        assetStore: BackgroundAssetStore,
    ): SaveCollectionWithBackgroundUseCase =
        SaveCollectionWithBackgroundUseCase(repository, assetStore)

    @Provides
    @Singleton
    fun provideRenameCollectionUseCase(
        repository: CollectionRepository,
    ): RenameCollectionUseCase = RenameCollectionUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteCollectionUseCase(
        repository: CollectionRepository,
        assetStore: BackgroundAssetStore,
    ): DeleteCollectionUseCase = DeleteCollectionUseCase(repository, assetStore)

    @Provides
    @Singleton
    fun provideCleanupOrphanBackgroundAssetsUseCase(
        repository: CollectionRepository,
        assetStore: BackgroundAssetStore,
    ): CleanupOrphanBackgroundAssetsUseCase =
        CleanupOrphanBackgroundAssetsUseCase(repository, assetStore)

    @Provides
    fun provideSelectActiveCollectionsUseCase(): SelectActiveCollectionsUseCase =
        SelectActiveCollectionsUseCase()
}
