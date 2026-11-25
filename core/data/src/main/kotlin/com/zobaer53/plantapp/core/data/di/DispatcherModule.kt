package com.zobaer53.plantapp.core.data.di

import com.zobaer53.plantapp.core.data.classifier.ModelSpec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.Executors
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
internal object DispatcherModule {
    @Provides
    @IoDispatcher
    fun provideIo(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefault(): CoroutineDispatcher = Dispatchers.Default

    /** Singleton so the CompiledModel never sees a second thread. Daemon: never blocks process exit. */
    @Provides
    @Singleton
    @InferenceDispatcher
    fun provideInference(): CoroutineDispatcher =
        Executors.newSingleThreadExecutor { r -> Thread(r, "litert-inference").apply { isDaemon = true } }
            .asCoroutineDispatcher()

    @Provides
    fun provideModelSpec(): ModelSpec = ModelSpec.TomatoLeafV1
}
