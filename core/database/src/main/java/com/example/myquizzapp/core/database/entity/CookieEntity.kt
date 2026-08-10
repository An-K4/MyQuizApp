package com.example.myquizzapp.core.database.entity

import androidx.room.Entity

@Entity(tableName = "cookies", primaryKeys = ["host", "name", "path"])
data class CookieEntity(
    val host: String,
    val name: String,
    val path: String,
    val value: String,
    val expiresAt: Long,   // epoch millis
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean
)