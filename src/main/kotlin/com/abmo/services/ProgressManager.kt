package com.abmo.services

import com.abmo.api.model.DownloadProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

object ProgressManager {
    private val progressFlow = MutableSharedFlow<DownloadProgress>(replay = 1)

    suspend fun emitProgress(progress: DownloadProgress) {
        progressFlow.emit(progress)
    }

    fun getProgressFlow(): Flow<DownloadProgress> = progressFlow
}