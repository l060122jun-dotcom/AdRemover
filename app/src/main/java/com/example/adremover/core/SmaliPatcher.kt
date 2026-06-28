package com.example.adremover.core

import android.util.Log
import com.example.adremover.model.DetectedAd
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import java.util.zip.Adler32
import java.security.MessageDigest

class SmaliPatcher {

    private companion object {
        const val TAG = "SmaliPatcher"
        const val META_FILE = ".patch_meta"
    }

    fun extractApk(apkFile: File, outputDir: File): Boolean {
        return try {
            if (outputDir.exists()) outputDir.deleteRecursively()
            outputDir.mkdirs()

            val methods = StringBuilder()

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
                    methods.appendLine("`{entry.name}	`{entry.method}")
                }
            }

            File(outputDir, META_FILE).writeText(methods.toString())
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
            val poisonLen = search.size
            val poisonBytes = ByteArray(poisonLen) { 'x'.code.toByte() }

            val dexFiles = extractedDir.listFiles { file -> file.name.endsWith(".dex") }

            dexFiles?.forEach { dexFile ->
                try {
                    val bytes = dexFile.readBytes()
                    var count = 0
                    var i = 0
                    while (i <= bytes.size - poisonLen) {
                        if (bytes.matchAt(i, search)) {
                            System.arraycopy(poisonBytes, 0, bytes, i, poisonLen)
                            count++
                            i += poisonLen
                        } else {
                            i++
                        }
                    }

                    if (count > 0) {
                        fixDexChecksum(bytes)
                        dexFile.writeBytes(bytes)
                        Log.d(TAG, "Patched `{dexFile.name}: `{count} replacements")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Patch `{dexFile.name} failed", e)
                }
            }
        }
    }

    private fun fixDexChecksum(bytes: ByteArray) {
        if (bytes.size < 36) return
        val fileSize = bytes.size

        bytes[32] = (fileSize shr 24).toByte()
        bytes[33] = (fileSize shr 16).toByte()
        bytes[34] = (fileSize shr 8).toByte()
        bytes[35] = (fileSize).toByte()

        val sha1 = MessageDigest.getInstance("SHA-1")
        sha1.update(bytes, 32, fileSize - 32)
        val digest = sha1.digest()
        System.arraycopy(digest, 0, bytes, 12, 20)

        val adler = Adler32()
        adler.update(bytes, 12, fileSize - 12)
        val checksum = adler.value.toInt()
        bytes[8] = (checksum shr 24).toByte()
        bytes[9] = (checksum shr 16).toByte()
        bytes[10] = (checksum shr 8).toByte()
        bytes[11] = (checksum).toByte()
    }

    fun patchManifest(extractedDir: File, detectedAds: List<DetectedAd>) {
        val manifestFile = File(extractedDir, "AndroidManifest.xml")
        if (!manifestFile.exists()) return

        try {
            val bytes = manifestFile.readBytes()
            var patched = false

            for (ad in detectedAds) {
                val search = ad.signature.packagePattern.toByteArray(Charsets.US_ASCII)
                val poisonBytes = ByteArray(search.size) { 'x'.code.toByte() }

                var i = 0
                while (i <= bytes.size - search.size) {
                    if (bytes.matchAt(i, search)) {
                        System.arraycopy(poisonBytes, 0, bytes, i, search.size)
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

            val methods = loadCompressionMeta(extractedDir)

            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                val files = extractedDir.walkTopDown().filter { it.isFile && it.name != META_FILE }.toList()
                files.forEach { file ->
                    val entryName = file.relativeTo(extractedDir).path.replace(File.separatorChar, '/')
                    val entry = ZipEntry(entryName)
                    val origMethod = methods[entryName]
                    entry.method = origMethod ?: ZipEntry.DEFLATED

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

    private fun loadCompressionMeta(extractedDir: File): Map<String, Int> {
        val metaFile = File(extractedDir, META_FILE)
        if (!metaFile.exists()) return emptyMap()

        val methods = mutableMapOf<String, Int>()
        metaFile.readLines().forEach { line ->
            val parts = line.split("\t")
            if (parts.size == 2) {
                val name = parts[0]
                val method = parts[1].toIntOrNull() ?: ZipEntry.DEFLATED
                methods[name] = method
            }
        }
        return methods
    }

    private fun ByteArray.matchAt(offset: Int, pattern: ByteArray): Boolean {
        if (offset + pattern.size > this.size) return false
        for (j in pattern.indices) {
            if (this[offset + j] != pattern[j]) return false
        }
        return true
    }
}