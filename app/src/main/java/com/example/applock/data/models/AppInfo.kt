package com.example.applock.data.models

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isProtected: Boolean = false,
    val isSystemApp: Boolean = false
)
