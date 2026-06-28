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
    }

    fun processApk(
        inputApk: File,
        outputApk: File,
        detectedAds: List<DetectedAd>
    ): Boolean {
        return try {
            if (outputApk.exists()) outputApk.delete()

            val searchPatterns = detectedAds.map { ad ->
                ad.signature.packagePattern.replace(".", "/").toByteArray(Charsets.US_ASCII)
            }

            ZipFile(inputApk).use { zipIn ->
                ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                    zipIn.entries().toList().forEach { entry ->
                        val entryName = entry.name
                        val isDex = entryName.endsWith(".dex")
                        val isManifest = entryName == "AndroidManifest.xml"

                        if (isDex || isManifest) {
                            val rawBytes = zipIn.getInputStream(entry).use { it.readBytes() }
                            val patchedBytes = patchBytes(rawBytes, searchPatterns)

                            val outEntry = ZipEntry(entryName)
                            outEntry.method = entry.method
                            zipOut.putNextEntry(outEntry)
                            zipOut.write(patchedBytes)
                            zipOut.closeEntry()

                            if (patchedBytes.size != rawBytes.size) {
                                Log.w(TAG, "Size changed: `{entryName} `{rawBytes.size} -> `{patchedBytes.size}")
                            }
                        } else {
                            val outEntry = ZipEntry(entryName)
                            outEntry.method = entry.method
                            zipOut.putNextEntry(outEntry)
                            zipIn.getInputStream(entry).use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
            }

            Log.d(TAG, "Processed: input=`{inputApk.length()} output=`{outputApk.length()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "processApk failed", e)
            false
        }
    }

    private fun patchBytes(bytes: ByteArray, patterns: List<ByteArray>): ByteArray {
        var result = bytes
        for (pattern in patterns) {
            val replacement = ByteArray(pattern.size) { 'x'.code.toByte() }
            var count = 0
            var i = 0
            while (i <= result.size - pattern.size) {
                if (result.matchAt(i, pattern)) {
                    System.arraycopy(replacement, 0, result, i, pattern.size)
                    count++
                    i += pattern.size
                } else {
                    i++
                }
            }
            if (count > 0) {
                Log.d(TAG, "Replaced `{count} occurrences of pattern")
                fixDexChecksum(result)
            }
        }
        return result
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

    private fun ByteArray.matchAt(offset: Int, pattern: ByteArray): Boolean {
        if (offset + pattern.size > this.size) return false
        for (j in pattern.indices) {
            if (this[offset + j] != pattern[j]) return false
        }
        return true
    }
}