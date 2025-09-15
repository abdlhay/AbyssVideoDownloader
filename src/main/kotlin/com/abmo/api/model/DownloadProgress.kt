package com.abmo.api.model

data class DownloadProgress(
    val downloadedSegments: Int = 0,
    val totalSegments: Int = 0,
    val downloadedBytes: Long = 0,
    val mediaSize: Long = 0,
    val percent: Double = 0.0,
    val status: String? = null,
    val message: String? = null,
    val error: String ? = null,
    val startTime: Long = 0
)
