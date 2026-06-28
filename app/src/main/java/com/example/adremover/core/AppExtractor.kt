package com.example.adremover.core

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.adremover.model.AppInfo
import java.io.File

class AppExtractor(private val context: Context) {
    
    private val pm = context.packageManager
    
    fun getInstalledApps(): List<AppInfo> {
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                val isThirdParty = appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0
                val isNotSelf = appInfo.packageName != context.packageName
                isThirdParty && isNotSelf
            }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo),
                    apkPath = appInfo.sourceDir,
                    apkSize = File(appInfo.sourceDir).length(),
                    versionName = try {
                        pm.getPackageInfo(appInfo.packageName, 0).versionName ?: "未知"
                    } catch (e: Exception) { "未知" }
                )
            }
            .sortedBy { it.appName.lowercase() }
    }
    
    fun extractApk(packageName: String): File? {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val sourceFile = File(appInfo.sourceDir)
            
            if (!sourceFile.exists()) return null
            
            val destDir = File(context.cacheDir, "extracted").apply { mkdirs() }
            val destFile = File(destDir, "${packageName}.apk")
            
            if (destFile.exists()) destFile.delete()
            
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }
            
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
