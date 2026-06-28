package com.example.adremover.model

data class DetectedAd(
    val signature: AdSignature,
    val matchedIn: String,
    val confidence: Float = 0.9f
)
