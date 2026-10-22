package android.kma.myquizzapp.core.common.model

/** Private, server-settled Classic result. Zero is data, absence is not zero. */
data class PersonalQuestionResult(
    val index: Int,
    val questionId: Long,
    val answered: Boolean,
    val isCorrect: Boolean?,
    val scoreEarned: Int,
    val totalScore: Int,
    val streak: Int,
    val nextQuestionAt: String?,
    val serverTime: String
)
