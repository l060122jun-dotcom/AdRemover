package com.example.adremover.core

import android.util.Log
import com.example.adremover.model.DetectedAd
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class SmaliPatcher {

    private companion object {
        const val TAG = "SmaliPatcher"

        val AD_NATIVE_LIBS = listOf(
            "libpangle", "libtt", "libttmetrix", "libgeoban",
            "libgdt", "libtencent", "libams", "libMarsHelper",
            "libmiui", "libjiagu", "libx3g", "libbaidustl",
            "libsopard", "libbdpush", "libBugly", "libRapidSolutions",
            "libwbsafecontrol", "libsgmain", "libsgsecuritybody",
            "libsgavmp", "libnms", "libapm", "libjiagu_64",
            "libDexHelper", "libDexHelper-x86", "libshell-super.2019",
            "libshella-4.2.0", "libx3g", "libAPKProtect"
        )

        val AD_ASSET_PATTERNS = listOf(
            "gdt_plugin", "ksad", "pangle", "anythink",
            "ad_config", "ad_config.json", "supplierconfig"
        )

        val AD_RESOURCE_PATTERNS = listOf(
            "reward_video", "interstitial_ad", "splash_ad",
            "banner_ad", "native_ad", "fullscreen_ad"
        )
    }

    fun processApk(
        inputApk: File,
        outputApk: File,
        detectedAds: List<DetectedAd>
    ): Boolean {
        return try {
            if (outputApk.exists()) outputApk.delete()

            val adPackagePaths = detectedAds.map { ad ->
                ad.signature.packagePattern.replace(".", "/")
            }

            var removedCount = 0
            var keptCount = 0

            ZipFile(inputApk).use { zipIn ->
                ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                    zipIn.entries().toList().forEach { entry ->
                        val entryName = entry.name

                        if (shouldRemoveEntry(entryName, adPackagePaths)) {
                            removedCount++
                            Log.d(TAG, "Removed: $entryName")
                            return@forEach
                        }

                        val outEntry = ZipEntry(entryName)
                        outEntry.method = entry.method

                        if (entry.method == ZipEntry.STORED) {
                            outEntry.size = entry.size
                            outEntry.compressedSize = entry.compressedSize
                            outEntry.crc = entry.crc
                        }

                        zipOut.putNextEntry(outEntry)
                        zipIn.getInputStream(entry).use { input ->
                            input.copyTo(zipOut)
                        }
                        zipOut.closeEntry()
                        keptCount++
                    }
                }
            }

            Log.d(TAG, "Processed: kept=$keptCount, removed=$removedCount")
            true
        } catch (e: Exception) {
            Log.e(TAG, "processApk failed", e)
            false
        }
    }

    private fun shouldRemoveEntry(entryName: String, adPackagePaths: List<String>): Boolean {
        for (adPath in adPackagePaths) {
            if (entryName.contains(adPath, ignoreCase = true)) {
                return true
            }
        }

        if (entryName.contains("/lib/") && entryName.endsWith(".so")) {
            for (libPattern in AD_NATIVE_LIBS) {
                if (entryName.contains(libPattern, ignoreCase = true)) {
                    return true
                }
            }
        }

        if (entryName.startsWith("assets/")) {
            for (assetPattern in AD_ASSET_PATTERNS) {
                if (entryName.contains(assetPattern, ignoreCase = true)) {
                    return true
                }
            }
        }

        return false
    }
}
