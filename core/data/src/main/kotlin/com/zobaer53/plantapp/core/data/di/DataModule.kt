package com.zobaer53.plantapp.core.data.di

import com.zobaer53.plantapp.core.data.classifier.LiteRtLeafDiseaseClassifier
import com.zobaer53.plantapp.core.data.image.ImageTensorSource
import com.zobaer53.plantapp.core.data.image.UriImageTensorSource
import com.zobaer53.plantapp.core.data.inference.InferenceEngine
import com.zobaer53.plantapp.core.data.inference.LiteRtInferenceEngine
import com.zobaer53.plantapp.core.data.info.StaticDiseaseInfoRepository
import com.zobaer53.plantapp.core.domain.classifier.LeafDiseaseClassifier
import com.zobaer53.plantapp.core.domain.repository.DiseaseInfoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindLeafDiseaseClassifier(impl: LiteRtLeafDiseaseClassifier): LeafDiseaseClassifier

    @Binds
    @Singleton
    abstract fun bindDiseaseInfoRepository(impl: StaticDiseaseInfoRepository): DiseaseInfoRepository

    /** LiteRtInferenceEngine is itself @Singleton, so there is one model per process. */
    @Binds
    abstract fun bindInferenceEngine(impl: LiteRtInferenceEngine): InferenceEngine

    @Binds
    abstract fun bindImageTensorSource(impl: UriImageTensorSource): ImageTensorSource
}
