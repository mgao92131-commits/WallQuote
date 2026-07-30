package com.example.wallquote.di

import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.repository.CollectionRepository
import com.example.wallquote.domain.repository.CustomStyleRepository
import com.example.wallquote.domain.usecase.CleanupOrphanBackgroundAssetsUseCase
import com.example.wallquote.domain.usecase.CreateStyleFromCollectionUseCase
import com.example.wallquote.domain.usecase.DeleteCollectionUseCase
import com.example.wallquote.domain.usecase.DeleteCustomStyleUseCase
import com.example.wallquote.domain.usecase.GetCollectionUseCase
import com.example.wallquote.domain.usecase.GetCustomStyleUseCase
import com.example.wallquote.domain.usecase.ObserveCustomStylesUseCase
import com.example.wallquote.domain.usecase.ObserveOrderedCollectionsUseCase
import com.example.wallquote.domain.usecase.ReorderCustomStylesUseCase
import com.example.wallquote.domain.usecase.SaveCollectionUseCase
import com.example.wallquote.domain.usecase.SaveCollectionWithBackgroundUseCase
import com.example.wallquote.domain.usecase.SaveCustomStyleUseCase
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

    @Provides
    @Singleton
    fun provideObserveCustomStylesUseCase(
        repository: CustomStyleRepository,
    ): ObserveCustomStylesUseCase = ObserveCustomStylesUseCase(repository)

    @Provides
    @Singleton
    fun provideGetCustomStyleUseCase(repository: CustomStyleRepository): GetCustomStyleUseCase =
        GetCustomStyleUseCase(repository)

    @Provides
    @Singleton
    fun provideSaveCustomStyleUseCase(repository: CustomStyleRepository): SaveCustomStyleUseCase =
        SaveCustomStyleUseCase(repository)

    @Provides
    @Singleton
    fun provideDeleteCustomStyleUseCase(repository: CustomStyleRepository): DeleteCustomStyleUseCase =
        DeleteCustomStyleUseCase(repository)

    @Provides
    @Singleton
    fun provideReorderCustomStylesUseCase(
        repository: CustomStyleRepository,
    ): ReorderCustomStylesUseCase = ReorderCustomStylesUseCase(repository)

    @Provides
    @Singleton
    fun provideCreateStyleFromCollectionUseCase(
        repository: CustomStyleRepository,
    ): CreateStyleFromCollectionUseCase = CreateStyleFromCollectionUseCase(repository)
}
