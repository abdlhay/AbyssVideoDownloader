package com.abmo.api.model

import com.abmo.model.Video

data class DownloadRequest(
    val config: WebConfig,
    val videoMetadata: Video
)