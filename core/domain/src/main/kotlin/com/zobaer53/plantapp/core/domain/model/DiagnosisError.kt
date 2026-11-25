package com.zobaer53.plantapp.core.domain.model

enum class DiagnosisError { IMAGE_UNREADABLE, MODEL_UNAVAILABLE, INFERENCE_FAILED }

class ClassificationException(
    val error: DiagnosisError,
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message ?: error.name, cause)
