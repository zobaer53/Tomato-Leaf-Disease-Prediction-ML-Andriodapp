package com.zobaer53.plantapp.core.domain.model

/** [guideUrl] is an https URL, or null when no guide applies (HEALTHY). */
data class DiseaseInfo(val disease: LeafDisease, val summary: String, val guideUrl: String?)
