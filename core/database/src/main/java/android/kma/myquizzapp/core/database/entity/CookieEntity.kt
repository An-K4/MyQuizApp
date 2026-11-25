package android.kma.myquizzapp.core.database.entity

import androidx.room.Entity

@Entity(tableName = "cookie_store", primaryKeys = ["name", "domain", "path"])
data class CookieEntity(
    val name: String,
    val domain: String,
    val path: String,
    val value: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
    val legacy: Boolean
)
