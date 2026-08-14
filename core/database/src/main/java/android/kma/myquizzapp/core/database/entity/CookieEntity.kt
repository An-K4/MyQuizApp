package android.kma.myquizzapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cookie_store")
data class CookieEntity(
    @PrimaryKey val key: String,   // "host|name"
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean
)