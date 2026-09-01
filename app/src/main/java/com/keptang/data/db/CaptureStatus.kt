package com.keptang.data.db

enum class CaptureStatus {
    RECORDING,
    CAPTURED,
    TRANSCRIBING,
    PARSING,
    PROCESSED,
    NEEDS_REVIEW,
    FAILED,
    CANCELLED;

    /** Captures in these states may be (re)processed without risk of duplicating results. */
    fun isRetryable(): Boolean = this == CAPTURED || this == FAILED || this == NEEDS_REVIEW
}
