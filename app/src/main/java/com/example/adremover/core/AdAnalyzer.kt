package com.example.adremover.core

import com.example.adremover.model.AdSignature
import com.example.adremover.model.AnalysisResult
import com.example.adremover.model.DetectedAd
import java.io.File
import java.util.zip.ZipFile

class AdAnalyzer {
    
    private val adSignatures = listOf(
        AdSignature("穿山甲广告", "com.bytedance.sdk.openadsdk"),
        AdSignature("广点通广告", "com.qq.e.ads"),
        AdSignature("百度广告", "com.baidu.mobads"),
        AdSignature("快手广告", "com.kwad.sdk"),
        AdSignature("Sigmob广告", "com.sigmob.sdk"),
        AdSignature("Unity Ads", "com.unity3d.services"),
        AdSignature("AdMob广告", "com.google.android.gms.ads"),
        AdSignature("Mintegral", "com.mbridge.msdk"),
        AdSignature("IronSource", "com.ironsource"),
        AdSignature("Vungle", "com.vungle"),
        AdSignature("AppLovin", "com.applovin"),
        AdSignature("Chartboost", "com.chartboost"),
        AdSignature("InMobi", "com.inmobi"),
        AdSignature("StartApp", "com.startapp"),
        AdSignature("Yandex Ads", "com.yandex.mobile.ads")
    )
    
    fun analyze(apkFile: File): AnalysisResult {
        val detectedAds = mutableListOf<DetectedAd>()
        
        try {
            val zipFile = ZipFile(apkFile)
            
            for (entry in zipFile.entries()) {
                val entryName = entry.name
                
                if (entryName.endsWith(".dex")) {
                    val dexBytes = zipFile.getInputStream(entry).readBytes()
                    val dexString = String(dexBytes, Charsets.UTF_8)
                    
                    for (signature in adSignatures) {
                        val packagePath = signature.packagePattern.replace(".", "/")
                        
                        if (dexString.contains(packagePath) || 
                            dexString.contains(signature.packagePattern)) {
                            detectedAds.add(
                                DetectedAd(
                                    signature = signature,
                                    matchedIn = entryName,
                                    confidence = 0.9f
                                )
                            )
                        }
                    }
                }
                
                if (entryName == "AndroidManifest.xml") {
                    val manifestBytes = zipFile.getInputStream(entry).readBytes()
                    
                    for (signature in adSignatures) {
                        if (manifestBytes.any { byte ->
                            val chunk = manifestBytes.slice(maxOf(0, byte.toInt()) until minOf(manifestBytes.size, byte.toInt() + 100))
                                .toByteArray()
                            String(chunk, Charsets.UTF_8).contains(signature.packagePattern)
                        }) {
                            if (detectedAds.none { it.signature.name == signature.name }) {
                                detectedAds.add(
                                    DetectedAd(
                                        signature = signature,
                                        matchedIn = "AndroidManifest.xml",
                                        confidence = 0.85f
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            zipFile.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        val uniqueAds = detectedAds.groupBy { it.signature.name }
            .map { (_, ads) -> ads.first() }
        
        return AnalysisResult(
            hasAds = uniqueAds.isNotEmpty(),
            detectedAds = uniqueAds,
            totalSize = apkFile.length()
        )
    }
}
