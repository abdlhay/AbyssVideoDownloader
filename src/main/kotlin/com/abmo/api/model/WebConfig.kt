package com.abmo.api.model

import com.abmo.common.Constants.DEFAULT_CONCURRENT_DOWNLOAD_LIMIT

data class WebConfig(
    val url: String,
    val resolution: String,
    var outputFile: String?,
    val header: Map<String, String>? = null,
    val connections: Int = DEFAULT_CONCURRENT_DOWNLOAD_LIMIT
)