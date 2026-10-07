package android.kma.myquizzapp.core.common.model

/** null means omitted; an empty phone/description explicitly clears that field. */
data class UserProfilePatch(
    val fullname: String? = null,
    val phone: String? = null,
    val description: String? = null
) {
    val isEmpty: Boolean get() = fullname == null && phone == null && description == null
}

/** Identifies an authenticated lifetime, including logging back into the same user. */
data class SessionUserToken(val userId: Long, val generation: Long)
