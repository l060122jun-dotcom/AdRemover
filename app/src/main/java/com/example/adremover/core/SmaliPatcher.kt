package com.example.adremover.core

import com.example.adremover.model.DetectedAd
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class SmaliPatcher {
    
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
            e.printStackTrace()
            false
        }
    }
    
    fun patchDexFiles(extractedDir: File, detectedAds: List<DetectedAd>) {
        for (ad in detectedAds) {
            val adPackagePath = ad.signature.packagePattern.replace(".", "/")
            
            val dexFiles = extractedDir.listFiles { file -> file.extension == "dex" }
            
            dexFiles?.forEach { dexFile ->
                try {
                    val bytes = dexFile.readBytes()
                    val content = String(bytes, Charsets.UTF_8)
                    
                    var modifiedContent = content
                    var modified = false
                    
                    if (content.contains(adPackagePath)) {
                        modifiedContent = content.replace(adPackagePath, "removed/$adPackagePath")
                        modified = true
                    }
                    
                    if (modified) {
                        dexFile.writeBytes(modifiedContent.toByteArray(Charsets.UTF_8))
                        println("Patched: ${dexFile.name}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun patchManifest(extractedDir: File, detectedAds: List<DetectedAd>) {
        val manifestFile = File(extractedDir, "AndroidManifest.xml")
        if (!manifestFile.exists()) return
        
        try {
            val bytes = manifestFile.readBytes()
            var content = String(bytes, Charsets.UTF_8)
            var modified = false
            
            for (ad in detectedAds) {
                val adPackage = ad.signature.packagePattern
                
                if (content.contains(adPackage)) {
                    content = content.replace(adPackage, "removed.$adPackage")
                    modified = true
                }
            }
            
            if (modified) {
                manifestFile.writeBytes(content.toByteArray(Charsets.UTF_8))
                println("Manifest patched")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun repackageApk(extractedDir: File, outputApk: File): Boolean {
        return try {
            if (outputApk.exists()) outputApk.delete()
            
            ZipOutputStream(FileOutputStream(outputApk)).use { zipOut ->
                extractedDir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val entryName = file.relativeTo(extractedDir).path.replace(File.separatorChar, '/')
                        
                        val entry = ZipEntry(entryName)
                        zipOut.putNextEntry(entry)
                        
                        file.inputStream().use { input ->
                            input.copyTo(zipOut)
                        }
                        
                        zipOut.closeEntry()
                    }
            }
            
            println("Repackaged: ${outputApk.absolutePath}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
