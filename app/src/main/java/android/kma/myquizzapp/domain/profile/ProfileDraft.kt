package android.kma.myquizzapp.domain.profile

import android.kma.myquizzapp.core.common.model.User
import android.kma.myquizzapp.core.common.model.UserProfilePatch

data class ProfileFieldErrors(
    val fullname: String? = null,
    val phone: String? = null,
    val description: String? = null
) {
    val hasErrors: Boolean get() = fullname != null || phone != null || description != null
}

/** Draft is not another logged-in User. The immutable baseline defines PATCH delta. */
data class ProfileDraft(
    val originalFullname: String,
    val originalPhone: String,
    val originalDescription: String,
    val fullname: String = originalFullname,
    val phone: String = originalPhone,
    val description: String = originalDescription
) {
    fun patch(): UserProfilePatch = UserProfilePatch(
        fullname = fullname.trim().takeIf { it != originalFullname },
        phone = phone.trim().takeIf { it != originalPhone },
        description = description.trim().takeIf { it != originalDescription }
    )
    val isDirty: Boolean get() = !patch().isEmpty

    fun errors(): ProfileFieldErrors {
        val patch = patch()
        val name = patch.fullname
        val number = patch.phone
        val bio = patch.description
        return ProfileFieldErrors(
            fullname = if (name != null && name.length !in 2..100)
                "Họ tên cần từ 2 đến 100 ký tự" else null,
            phone = if (number != null && number.isNotEmpty() &&
                !Regex("^\\+?[0-9]{7,15}$").matches(number))
                "Số điện thoại cần 7–15 chữ số, có thể bắt đầu bằng +" else null,
            description = if (bio != null && bio.length > 200)
                "Giới thiệu không được quá 200 ký tự" else null
        )
    }

    companion object {
        fun from(user: User) = ProfileDraft(user.fullname, user.phone.orEmpty(), user.description.orEmpty())
    }
}
