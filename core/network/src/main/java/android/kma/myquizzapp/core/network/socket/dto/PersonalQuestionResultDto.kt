package android.kma.myquizzapp.core.network.socket.dto

import android.kma.myquizzapp.core.common.model.PersonalQuestionResult
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** All fields are required, including explicitly nullable fields. Preserve mixed wire casing. */
@Serializable
internal data class PersonalQuestionResultDto(
    val index: Int,
    @SerialName("question_id") val questionId: Long,
    val answered: Boolean,
    val isCorrect: Boolean?,
    val scoreEarned: Int,
    val totalScore: Int,
    val streak: Int,
    val nextQuestionAt: String?,
    val serverTime: String
) {
    fun toDomain(): PersonalQuestionResult {
        require(index >= 0 && questionId > 0)
        require(scoreEarned >= 0 && totalScore >= 0 && streak >= 0)
        require(if (answered) isCorrect != null else isCorrect == null && scoreEarned == 0 && streak == 0)
        Instant.parse(serverTime)
        nextQuestionAt?.let { Instant.parse(it) }
        return PersonalQuestionResult(index, questionId, answered, isCorrect, scoreEarned,
            totalScore, streak, nextQuestionAt, serverTime)
    }
}

private val personalResultJson = Json { ignoreUnknownKeys = true }

/** A malformed optional private field must not discard the snapshot's phase/pause/deadline. */
internal fun JsonElement?.toPersonalQuestionResult(): PersonalQuestionResult? = this?.let {
    runCatching { personalResultJson.decodeFromJsonElement(PersonalQuestionResultDto.serializer(), it).toDomain() }
        .getOrNull()
}
