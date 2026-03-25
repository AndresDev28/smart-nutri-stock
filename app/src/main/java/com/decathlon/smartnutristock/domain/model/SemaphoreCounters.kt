package com.decathlon.smartnutristock.domain.model

data class SemaphoreCounters(
    val red: Int,
    val yellow: Int,
    val green: Int,
    val expired: Int
) {
    val total: Int
        get() = red + yellow + green + expired
}
