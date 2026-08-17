package android.kma.myquizzapp.core.common.model

/**
 * Một section trên home screen (horizontal row với title).
 * 
 * Ví dụ: "Trending", "Newest", "Continue Playing"
 */
data class HomeSection(
    val sectionKey: String,
    val title: String,
    val sectionType: String,  // "featured", "continue", "trending", "newest", "category"
    val items: List<QuizCard>
)
