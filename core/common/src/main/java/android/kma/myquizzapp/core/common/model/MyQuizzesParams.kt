package android.kma.myquizzapp.core.common.model

/**
 * Filter/sort cho danh sách "Quiz của tôi" — khớp myQuizzesQuerySchema (quiz.schema.ts).
 * apiValue là chuỗi backend hiểu, tránh lặp lại string literal ở nhiều nơi.
 */
enum class MyQuizzesVisibility(val apiValue: String) {
    ALL("all"),
    PUBLIC("public"),
    PRIVATE("private")
}

/**
 * Sort accepted bởi GET /quizzes/me — subset của LIST_SORTS (listing.type.ts),
 * xem MY_QUIZZES_SORTS trong quiz.schema.ts.
 */
enum class MyQuizzesSort(val apiValue: String) {
    RECENTLY_UPDATED("recently_updated"),
    NEWEST("newest"),
    OLDEST("oldest"),
    NAME_ASC("name_asc")
}

/**
 * Params cho QuizRepository.getMyQuizzes(). cursor null = trang đầu.
 * limit tối đa 24 theo backend (myQuizzesQuerySchema limitQuery).
 */
data class MyQuizzesParams(
    val visibility: MyQuizzesVisibility = MyQuizzesVisibility.ALL,
    val keyword: String? = null,
    val sort: MyQuizzesSort = MyQuizzesSort.RECENTLY_UPDATED,
    val cursor: String? = null,
    val limit: Int = 12
)
