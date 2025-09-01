package com.abmo.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

object ProgressManager {
    private val progressFlow = MutableSharedFlow<Map<String, Any>>(replay = 1)

    suspend fun emitProgress(progress: Map<String, Any>) {
        progressFlow.emit(progress)
    }

    fun getFlow(): Flow<Map<String, Any>> = progressFlow
}