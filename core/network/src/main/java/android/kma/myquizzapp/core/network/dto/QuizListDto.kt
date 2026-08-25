package android.kma.myquizzapp.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Wrapper cho response của GET /quizzes/search và GET /quizzes/me.
 *
 * Backend (listing.controller.ts) luôn trả `success(res, { quizzes: page.items }, ...)`,
 * tức là envelope data = { "quizzes": [...] }, KHÔNG phải mảng trần ở top-level.
 * Cùng quy ước với QuizDetailDto/AuthDataDto/HomeContentDto: mọi endpoint backend đều
 * bọc data trong 1 key đặt tên theo resource — luôn đọc lại controller trước khi khai
 * báo kiểu trả về ở Retrofit, không đoán theo domain model.
 *
 * Note (N16.5): `meta.pagination` (cursor) đã được dùng — ResultCall tự gắn vào
 * Result.Success.page, SearchViewModel đọc nextCursor/hasMore từ đó.
 */
@Serializable
data class QuizListDto(val quizzes: List<QuizCardDto>)
