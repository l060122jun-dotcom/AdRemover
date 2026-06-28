package com.example.adremover.core

import com.example.adremover.model.AdSignature
import com.example.adremover.model.AnalysisResult
import com.example.adremover.model.DetectedAd
import java.io.File
import java.util.zip.ZipFile

class AdAnalyzer {

    private val adSignatures = listOf(
        AdSignature("穿山甲广告", "com.bytedance.sdk.openadsdk", listOf("pangle", "tt_")),
        AdSignature("腾讯广点通", "com.qq.e.ads", listOf("gdt_", "GDT")),
        AdSignature("百度广告", "com.baidu.mobads", listOf("baidu", "baidustl")),
        AdSignature("快手广告", "com.kwad.sdk", listOf("ksad", "kwad")),
        AdSignature("Sigmob广告", "com.sigmob.sdk", listOf("sigmob")),
        AdSignature("Unity Ads", "com.unity3d.services", listOf("unity")),
        AdSignature("AdMob广告", "com.google.android.gms.ads", listOf("admob", "google.ads")),
        AdSignature("Mintegral", "com.mbridge.msdk", listOf("mbridge", "mintegral")),
        AdSignature("IronSource", "com.ironsource", listOf("ironsource")),
        AdSignature("Vungle", "com.vungle", listOf("vungle")),
        AdSignature("AppLovin", "com.applovin", listOf("applovin")),
        AdSignature("Chartboost", "com.chartboost", listOf("chartboost")),
        AdSignature("InMobi", "com.inmobi", listOf("inmobi")),
        AdSignature("StartApp", "com.startapp", listOf("startapp")),
        AdSignature("Yandex Ads", "com.yandex.mobile.ads", listOf("yandex")),
        AdSignature("TopOn聚合", "com.anythink", listOf("anythink", "topon")),
        AdSignature("小米广告", "com.xiaomi.ad", listOf("xiaomi_ad")),
        AdSignature("华为广告", "com.huawei.ads", listOf("huawei_ads")),
        AdSignature("OPPO广告", "com.heytap.msp", listOf("oppo_ad")),
        AdSignature("vivo广告", "com.vivo.identifier", listOf("vivo_ad"))
    )

    fun analyze(apkFile: File): AnalysisResult {
        val detectedAds = mutableListOf<DetectedAd>()

        try {
            ZipFile(apkFile).use { zipFile ->
                for (entry in zipFile.entries()) {
                    val entryName = entry.name

                    if (entryName.endsWith(".dex")) {
                        analyzeDex(zipFile, entry, entryName, detectedAds)
                    }

                    if (entryName == "AndroidManifest.xml") {
                        analyzeManifest(zipFile, entry, entryName, detectedAds)
                    }

                    analyzeNativeLibs(entryName, detectedAds)
                    analyzeAssets(entryName, detectedAds)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val uniqueAds = detectedAds.groupBy { it.signature.name }
            .map { (_, ads) -> ads.first() }
            .sortedByDescending { it.confidence }

        return AnalysisResult(
            hasAds = uniqueAds.isNotEmpty(),
            detectedAds = uniqueAds,
            totalSize = apkFile.length()
        )
    }

    private fun analyzeDex(zipFile: ZipFile, entry: java.util.zip.ZipEntry, entryName: String, detectedAds: MutableList<DetectedAd>) {
        try {
            val dexBytes = zipFile.getInputStream(entry).readBytes()
            val dexString = String(dexBytes, Charsets.ISO_8859_1)

            for (signature in adSignatures) {
                val packagePath = signature.packagePattern.replace(".", "/")

                if (dexString.contains(packagePath)) {
                    if (detectedAds.none { it.signature.name == signature.name }) {
                        detectedAds.add(
                            DetectedAd(
                                signature = signature,
                                matchedIn = entryName,
                                confidence = 0.95f
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun analyzeManifest(zipFile: ZipFile, entry: java.util.zip.ZipEntry, entryName: String, detectedAds: MutableList<DetectedAd>) {
        try {
            val manifestBytes = zipFile.getInputStream(entry).readBytes()

            for (signature in adSignatures) {
                val patternBytes = signature.packagePattern.toByteArray(Charsets.US_ASCII)

                if (containsBytes(manifestBytes, patternBytes)) {
                    if (detectedAds.none { it.signature.name == signature.name }) {
                        detectedAds.add(
                            DetectedAd(
                                signature = signature,
                                matchedIn = entryName,
                                confidence = 0.85f
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun analyzeNativeLibs(entryName: String, detectedAds: MutableList<DetectedAd>) {
        if (!entryName.contains("/lib/") || !entryName.endsWith(".so")) return

        val adLibPatterns = mapOf(
            "穿山甲广告" to listOf("libpangle", "libttmetrix"),
            "腾讯广点通" to listOf("libgdt", "libtms"),
            "百度广告" to listOf("libbaidustl"),
            "快手广告" to listOf("libkwsc", "libkwai"),
            "TopOn聚合" to listOf("libanythink")
        )

        for ((name, patterns) in adLibPatterns) {
            for (pattern in patterns) {
                if (entryName.contains(pattern, ignoreCase = true)) {
                    val sig = adSignatures.find { it.name == name }
                    if (sig != null && detectedAds.none { it.signature.name == name }) {
                        detectedAds.add(
                            DetectedAd(
                                signature = sig,
                                matchedIn = entryName,
                                confidence = 0.9f
                            )
                        )
                    }
                }
            }
        }
    }

    private fun analyzeAssets(entryName: String, detectedAds: MutableList<DetectedAd>) {
        if (!entryName.startsWith("assets/")) return

        val adAssetPatterns = mapOf(
            "穿山甲广告" to listOf("pangle", "tt_"),
            "腾讯广点通" to listOf("gdt_", "gdtplugin"),
            "快手广告" to listOf("ksad"),
            "TopOn聚合" to listOf("anythink")
        )

        for ((name, patterns) in adAssetPatterns) {
            for (pattern in patterns) {
                if (entryName.contains(pattern, ignoreCase = true)) {
                    val sig = adSignatures.find { it.name == name }
                    if (sig != null && detectedAds.none { it.signature.name == name }) {
                        detectedAds.add(
                            DetectedAd(
                                signature = sig,
                                matchedIn = entryName,
                                confidence = 0.8f
                            )
                        )
                    }
                }
            }
        }
    }

    private fun containsBytes(data: ByteArray, pattern: ByteArray): Boolean {
        if (pattern.isEmpty()) return false
        outer@ for (i in 0..data.size - pattern.size) {
            for (j in pattern.indices) {
                if (data[i + j] != pattern[j]) continue@outer
            }
            return true
        }
        return false
    }
}
