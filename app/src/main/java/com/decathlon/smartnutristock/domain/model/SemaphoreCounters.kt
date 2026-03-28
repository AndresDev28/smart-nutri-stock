package com.decathlon.smartnutristock.domain.model

data class SemaphoreCounters(
    val yellow: Int,
    val green: Int,
    val expired: Int
) {
    val total: Int
        get() = yellow + green + expired
}
