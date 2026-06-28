package com.example.adremover.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val apkPath: String,
    val apkSize: Long,
    val versionName: String
)

data class AdSignature(
    val name: String,
    val packagePattern: String,
    val filePatterns: List<String> = emptyList()
)

data class DetectedAd(
    val signature: AdSignature,
    val matchedIn: String,
    val confidence: Float
)

data class AnalysisResult(
    val hasAds: Boolean,
    val detectedAds: List<DetectedAd>,
    val totalSize: Long
)
