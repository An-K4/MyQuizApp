package android.kma.myquizzapp.core.common.util

import java.security.MessageDigest

/**
 * Security utility functions for cryptographic operations
 */

/**
 * Hash a string using SHA-256 algorithm
 * Commonly used for nonce hashing in OAuth/OIDC flows
 * 
 * @return Hexadecimal string representation of the SHA-256 hash
 */
fun String.hashSha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
