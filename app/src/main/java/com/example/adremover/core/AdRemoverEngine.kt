package com.example.adremover.core

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.adremover.model.AnalysisResult
import com.example.adremover.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdRemoverEngine(private val context: Context) {

    private companion object {
        const val TAG = "AdRemoverEngine"
    }

    private val extractor = AppExtractor(context)
    private val analyzer = AdAnalyzer()
    private val patcher = SmaliPatcher()
    private val signer = ApkSignerUtil()

    sealed class ProcessState {
        object Idle : ProcessState()
        object Extracting : ProcessState()
        data class Analyzing(val message: String) : ProcessState()
        data class Patching(val message: String) : ProcessState()
        object Signing : ProcessState()
        data class Success(val outputFile: File, val appName: String) : ProcessState()
        data class Error(val message: String) : ProcessState()
    }

    fun getInstalledApps(): List<AppInfo> {
        return extractor.getInstalledApps()
    }

    suspend fun processApp(
        packageName: String,
        onStateChange: (ProcessState) -> Unit
    ) = withContext(Dispatchers.IO) {

        try {
            onStateChange(ProcessState.Extracting)
            val originalApk = extractor.extractApk(packageName)
                ?: throw Exception("无法获取APK文件")

            onStateChange(ProcessState.Analyzing("正在扫描广告SDK..."))
            val analysis = analyzer.analyze(originalApk)

            if (!analysis.hasAds) {
                onStateChange(ProcessState.Error("未检测到已知广告SDK"))
                return@withContext
            }

            val adNames = analysis.detectedAds.joinToString { it.signature.name }
            onStateChange(ProcessState.Analyzing("识别广告: "))

            onStateChange(ProcessState.Patching("正在修改APK..."))
            val patchedApk = File(context.cacheDir, "_patched.apk")
            val patched = patcher.processApk(originalApk, patchedApk, analysis.detectedAds)

            if (!patched) {
                throw Exception("APK修改失败")
            }

            onStateChange(ProcessState.Signing)
            val signedApk = File(context.cacheDir, "_signed.apk")
            val signed = signer.signApk(patchedApk, signedApk)

            if (!signed) {
                throw Exception("APK签名失败")
            }

            val appName = getInstalledApps()
                .find { it.packageName == packageName }
                ?.appName ?: packageName

            val outputDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "AdRemover"
            ).apply { mkdirs() }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val finalApk = File(outputDir, "_no_ads_.apk")

            signedApk.copyTo(finalApk)

            originalApk.delete()
            patchedApk.delete()
            signedApk.delete()

            Log.d(TAG, "Output: `{finalApk.absolutePath}, size=`{finalApk.length()}")
            onStateChange(ProcessState.Success(finalApk, appName))

        } catch (e: Exception) {
            Log.e(TAG, "Process failed", e)
            onStateChange(ProcessState.Error(e.message ?: "未知错误"))
        }
    }

    suspend fun analyzeApp(packageName: String): AnalysisResult? {
        return withContext(Dispatchers.IO) {
            try {
                val apk = extractor.extractApk(packageName) ?: return@withContext null
                val result = analyzer.analyze(apk)
                apk.delete()
                result
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}