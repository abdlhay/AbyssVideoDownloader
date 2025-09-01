package com.abmo.api.model


sealed class Endpoint(val path: String) {
    data object Root: Endpoint("/")
    data object DownloadVideo: Endpoint("/download")
    data object FetchVideoMetadata: Endpoint("/fetch")
    data object GetCurrentProgress: Endpoint("/progress")
}