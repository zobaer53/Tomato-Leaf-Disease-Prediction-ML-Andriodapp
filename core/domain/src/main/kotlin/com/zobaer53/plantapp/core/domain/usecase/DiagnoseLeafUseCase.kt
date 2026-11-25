package com.zobaer53.plantapp.core.domain.usecase

import com.zobaer53.plantapp.core.domain.classifier.LeafDiseaseClassifier
import com.zobaer53.plantapp.core.domain.model.ClassificationException
import com.zobaer53.plantapp.core.domain.model.Diagnosis
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.DiagnosisResult
import com.zobaer53.plantapp.core.domain.model.ImageRef

class DiagnoseLeafUseCase(
    private val classifier: LeafDiseaseClassifier,
    private val confidencePolicy: ConfidencePolicy,
) {
    /**
     * classify -> stable sort by confidence desc -> Diagnosis(top, ranked, policy.isConfident(top)).
     * ClassificationException -> Failure(e.error). Empty list -> Failure(INFERENCE_FAILED).
     * Catches ONLY ClassificationException (cancellation and programmer errors propagate).
     */
    suspend operator fun invoke(image: ImageRef): DiagnosisResult {
        val predictions = try {
            classifier.classify(image)
        } catch (e: ClassificationException) {
            return DiagnosisResult.Failure(e.error)
        }
        if (predictions.isEmpty()) return DiagnosisResult.Failure(DiagnosisError.INFERENCE_FAILED)

        // sortedByDescending is stable, so ties keep the classifier's order.
        val ranked = predictions.sortedByDescending { it.confidence }
        val top = ranked.first()
        return DiagnosisResult.Success(
            Diagnosis(top = top, ranked = ranked, isConfident = confidencePolicy.isConfident(top)),
        )
    }
}
