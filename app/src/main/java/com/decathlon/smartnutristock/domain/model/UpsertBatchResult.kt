package com.decathlon.smartnutristock.domain.model

sealed class UpsertBatchResult {
    data class Success(val status: SemaphoreStatus) : UpsertBatchResult()
    data object Deleted : UpsertBatchResult()
    data class Error(val message: String) : UpsertBatchResult()
}
