package android.kma.myquizzapp.core.network.dto

import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.model.QuizCard
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO cho response từ GET /v1/quizzes/home
 * 
 * Backend trả sections khác nhau tùy auth state:
 * - Guest: featured, trending, newest, most_played
 * - Authenticated: thêm "continue" section
 */
@Serializable
data class HomeContentDto(
    val sections: List<HomeSectionDto>
)

/**
 * Một section trên home screen (horizontal row).
 */
@Serializable
data class HomeSectionDto(
    @SerialName("section_key")
    val sectionKey: String,
    
    val title: String,
    
    @SerialName("section_type")
    val sectionType: SectionType,
    
    val items: List<QuizCardDto>
)

/**
 * Quiz card trong section - map sang QuizCard domain model.
 */
@Serializable
data class QuizCardDto(
    val id: Long,
    
    @SerialName("quiz_name")
    val quizName: String,
    
    @SerialName("quiz_description")
    val quizDescription: String? = null,
    
    @SerialName("quiz_image")
    val quizImage: String? = null,
    
    @SerialName("quiz_category")
    val quizCategory: String? = null,
    
    @SerialName("quiz_language")
    val quizLanguage: String,
    
    @SerialName("quiz_owner")
    val quizOwnerId: Long,
    
    val owner: QuizOwnerDto? = null,
    
    @SerialName("question_count")
    val questionCount: Int,
    
    @SerialName("play_count")
    val playCount: Int,
    
    @SerialName("completion_rate")
    val completionRate: Double,
    
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class QuizOwnerDto(
    val id: Long,
    val fullname: String,
    val avatar: String? = null
)

/**
 * Section types từ backend.
 */
@Serializable
enum class SectionType {
    @SerialName("featured") FEATURED,
    @SerialName("continue") CONTINUE,
    @SerialName("trending") TRENDING,
    @SerialName("newest") NEWEST,
    @SerialName("category") CATEGORY
}

// === Mapper extensions: DTO → Domain ===

fun HomeContentDto.toDomain(): List<HomeSection> {
    return sections.map { it.toDomain() }
}

fun HomeSectionDto.toDomain(): HomeSection {
    return HomeSection(
        sectionKey = sectionKey,
        title = title,
        sectionType = sectionType.name.lowercase(),
        items = items.map { it.toDomain() }
    )
}

fun QuizCardDto.toDomain(): QuizCard {
    return QuizCard(
        id = id,
        quizName = quizName,
        quizDescription = quizDescription,
        quizImage = quizImage,
        quizCategory = quizCategory,
        quizLanguage = quizLanguage,
        quizOwnerId = quizOwnerId,
        owner = owner?.toDomain(),
        questionCount = questionCount,
        playCount = playCount,
        completionRate = completionRate,
        createdAt = createdAt
    )
}

fun QuizOwnerDto.toDomain(): android.kma.myquizzapp.core.common.model.QuizOwner {
    return android.kma.myquizzapp.core.common.model.QuizOwner(
        id = id,
        fullname = fullname,
        avatar = avatar
    )
}
