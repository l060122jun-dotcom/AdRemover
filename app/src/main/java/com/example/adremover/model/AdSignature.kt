package com.example.adremover.model

data class AdSignature(
    val name: String,
    val packagePattern: String,
    val filePatterns: List<String> = emptyList()
)
