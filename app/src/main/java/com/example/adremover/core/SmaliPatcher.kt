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
    }

    fun extractApk(apkFile: File, outputDir: File): Boolean {
        return try {
            if (outputDir.exists()) outputDir.deleteRecursively()
            outputDir.mkdirs()

            ZipFile(apkFile).use { zip ->
                zip.entries().toList().forEach { entry ->
                    val outputFile = File(outputDir, entry.name)
                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outputFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Extract failed", e)
            false
        }
    }

    fun patchDexFiles(extractedDir: File, detectedAds: List<DetectedAd>) {
        for (ad in detectedAds) {
            val adPackagePath = ad.signature.packagePattern.replace(".", "/")
            val search = adPackagePath.toByteArray(Charsets.US_ASCII)
            val poison = buildPoisonPath(search.size).toByteArray(Charsets.US_ASCII)

            val dexFiles = extractedDir.listFiles { file -> file.name.endsWith(".dex") }

            dexFiles?.forEach { dexFile ->
                try {
                    val bytes = dexFile.readBytes()
                    var count = 0
                    var i = 0
                    while (i <= bytes.size - search.size) {
                        if (bytes.matchAt(i, search)) {
                            System.arraycopy(poison, 0, bytes, i, search.size)
                            count++
                            i += search.size
                        } else {
                            i++
                        }
                    }

                    if (count > 0) {
                        dexFile.writeBytes(bytes)
                        Log.d(TAG, "Patched `{dexFile.name}: `{count} replacements")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Patch `{dexFile.name} failed", e)
                }
            }
        }
    }

    fun patchManifest(extractedDir: File, detectedAds: List<DetectedAd>) {
        val manifestFile = File(extractedDir, "AndroidManifest.xml")
        if (!manifestFile.exists()) return

        try {
            val bytes = manifestFile.readBytes()
            var patched = false

            for (ad in detectedAds) {
                val search = ad.signature.packagePattern.toByteArray(Charsets.US_ASCII)
                val poison = buildPoisonPath(search.size).toByteArray(Charsets.US_ASCII)

                var i = 0
                while (i <= bytes.size - search.size) {
                    if (bytes.matchAt(i, search)) {
                        System.arraycopy(poison, 0, bytes, i, search.size)
                        patched = true
                        i += search.size
                    } else {
                        i++
                    }
                }
            }

            if (patched) {
                manifestFile.writeBytes(bytes)
                Log.d(TAG, "Manifest patched")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Manifest patch failed", e)
        }
    }

    fun repackageApk(extractedDir: File, outputApk: File): Boolean {
        return try {
            if (outputApk.exists()) outputApk.delete()

            val zipEntries = mutableMapOf<String, Int>()
            ZipFile(File(extractedDir.parent, extractOriginalName(extractedDir.name))).use { orig ->
                orig.entries().toList().forEach { entry ->
                    zipEntries[entry.name] = entry.method
                }
            }

            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                val files = extractedDir.walkTopDown().filter { it.isFile }.toList()
                files.forEach { file ->
                    val entryName = file.relativeTo(extractedDir).path.replace(File.separatorChar, '/')
                    val entry = ZipEntry(entryName)
                    val originalMethod = zipEntries[entryName]
                    if (originalMethod != null) {
                        entry.method = originalMethod
                    } else {
                        entry.method = ZipEntry.DEFLATED
                    }

                    FileInputStream(file).use { input ->
                        zipOut.putNextEntry(entry)
                        input.copyTo(zipOut)
                        zipOut.closeEntry()
                    }
                }
            }
            Log.d(TAG, "Repackaged: `{outputApk.absolutePath}, size=`{outputApk.length()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Repackage failed", e)
            false
        }
    }

    private fun extractOriginalName(dirName: String): String {
        return dirName.removePrefix("extracted_")
    }

    private fun ByteArray.matchAt(offset: Int, pattern: ByteArray): Boolean {
        if (offset + pattern.size > this.size) return false
        for (j in pattern.indices) {
            if (this[offset + j] != pattern[j]) return false
        }
        return true
    }

    private fun buildPoisonPath(len: Int): String {
        val sb = StringBuilder()
        while (sb.length < len) {
            if (sb.isNotEmpty()) sb.append('/')
            sb.append("x")
        }
        return sb.toString().take(len)
    }
}