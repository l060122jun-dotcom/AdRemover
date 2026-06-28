package com.example.adremover.core

import android.util.Log
import java.io.File
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Date
import com.android.apksig.ApkSigner
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger

class ApkSignerUtil {
    
    private companion object {
        const val TAG = "ApkSignerUtil"
    }
    
    fun signApk(inputApk: File, outputApk: File): Boolean {
        return try {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
                Log.d(TAG, "BouncyCastle provider registered")
            }
            
            Log.d(TAG, "inputApk: `{inputApk.absolutePath}, size=`{inputApk.length()}")
            Log.d(TAG, "outputApk: `{outputApk.absolutePath}")
            
            val keyPair = KeyPairGenerator.getInstance("RSA").apply {
                initialize(2048)
            }.generateKeyPair()
            
            Log.d(TAG, "KeyPair generated")
            
            val issuer = X500Name("CN=AdRemover,O=OpenSource,C=US")
            val serial = BigInteger.ONE
            val notBefore = Date()
            val notAfter = Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)
            
            val publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
            Log.d(TAG, "PublicKeyInfo created")
            
            val certBuilder = JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, issuer, publicKeyInfo
            )
            
            val signer = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
            Log.d(TAG, "ContentSigner created")
            
            val certificate = JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certBuilder.build(signer))
            Log.d(TAG, "Certificate created: `{certificate.subjectX500Principal}")
            
            val signerConfig = ApkSigner.SignerConfig.Builder(
                "CERT", keyPair.private, listOf(certificate)
            ).build()
            
            val apkSigner = ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setMinSdkVersion(26)
                .build()
            
            Log.d(TAG, "Starting APK signing...")
            apkSigner.sign()
            Log.d(TAG, "APK signed successfully, output size=`{outputApk.length()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "APK signing failed", e)
            false
        }
    }
}