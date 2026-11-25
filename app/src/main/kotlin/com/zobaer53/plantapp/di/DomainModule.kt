package com.zobaer53.plantapp.di

import com.zobaer53.plantapp.core.domain.classifier.LeafDiseaseClassifier
import com.zobaer53.plantapp.core.domain.repository.DiseaseInfoRepository
import com.zobaer53.plantapp.core.domain.usecase.ConfidencePolicy
import com.zobaer53.plantapp.core.domain.usecase.DiagnoseLeafUseCase
import com.zobaer53.plantapp.core.domain.usecase.GetDiseaseInfoUseCase
import com.zobaer53.plantapp.core.domain.usecase.ThresholdConfidencePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The domain module has no DI annotations, so its use cases are provided here. */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    @Provides
    fun provideConfidencePolicy(): ConfidencePolicy = ThresholdConfidencePolicy()

    @Provides
    fun provideDiagnoseLeafUseCase(
        classifier: LeafDiseaseClassifier,
        policy: ConfidencePolicy,
    ): DiagnoseLeafUseCase = DiagnoseLeafUseCase(classifier, policy)

    @Provides
    fun provideGetDiseaseInfoUseCase(repository: DiseaseInfoRepository): GetDiseaseInfoUseCase =
        GetDiseaseInfoUseCase(repository)
}
