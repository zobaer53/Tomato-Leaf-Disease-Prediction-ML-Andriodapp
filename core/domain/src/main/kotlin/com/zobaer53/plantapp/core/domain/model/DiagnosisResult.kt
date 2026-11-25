package com.zobaer53.plantapp.core.domain.model

sealed interface DiagnosisResult {
    data class Success(val diagnosis: Diagnosis) : DiagnosisResult
    data class Failure(val error: DiagnosisError) : DiagnosisResult
}
