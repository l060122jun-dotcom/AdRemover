package com.example.adremover.core

import java.io.File
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.util.Date
import com.android.apksig.ApkSigner
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.math.BigInteger

class ApkSignerUtil {
    
    fun signApk(inputApk: File, outputApk: File): Boolean {
        return try {
            val keyPair = KeyPairGenerator.getInstance("RSA").apply {
                initialize(2048)
            }.generateKeyPair()
            
            val issuer = X500Name("CN=AdRemover,O=OpenSource,C=US")
            val serial = BigInteger.ONE
            val notBefore = Date()
            val notAfter = Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000)
            
            val publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
            
            val certBuilder = JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, issuer, publicKeyInfo
            )
            
            val signer = JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.private)
            
            val certificate = JcaX509CertificateConverter()
                .getCertificate(certBuilder.build(signer))
            
            val signerConfig = ApkSigner.SignerConfig.Builder(
                "CERT", keyPair.private, listOf(certificate)
            ).build()
            
            val apkSigner = ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setMinSdkVersion(26)
                .build()
            
            apkSigner.sign()
            
            println("Signed: `{outputApk.absolutePath}")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}