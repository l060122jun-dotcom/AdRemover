package com.example.adremover.model

data class AnalysisResult(
    val hasAds: Boolean,
    val detectedAds: List<DetectedAd>,
    val totalSize: Long
)
