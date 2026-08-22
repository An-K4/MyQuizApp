package android.kma.myquizzapp.core.network.dto

import android.kma.myquizzapp.core.common.model.Quiz
import kotlinx.serialization.Serializable

/**
 * Wrapper cho response của GET/POST/PATCH/DELETE /quizzes/id/:quizId.
 *
 * Backend (quiz.controller.ts) luôn trả `success(res, { quiz })`, tức là
 * envelope data = { "quiz": {...} }, KHÔNG phải Quiz object trực tiếp ở top-level.
 * Giống hệt pattern AuthDataDto(user) / HomeContentDto(sections) — mọi endpoint
 * backend đều bọc data trong 1 key đặt tên theo resource.
 */
@Serializable
data class QuizDetailDto(val quiz: Quiz)
