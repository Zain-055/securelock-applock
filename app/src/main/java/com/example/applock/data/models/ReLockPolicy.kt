package com.example.applock.data.models

enum class ReLockPolicy(val displayName: String, val timeoutMillis: Long) {
    IMMEDIATELY("Immediately", 0L),
    AFTER_LEAVING("After leaving app", 0L),
    SECONDS_30("30 seconds after leaving", 30_000L),
    MINUTE_1("1 minute after leaving", 60_000L)
}
