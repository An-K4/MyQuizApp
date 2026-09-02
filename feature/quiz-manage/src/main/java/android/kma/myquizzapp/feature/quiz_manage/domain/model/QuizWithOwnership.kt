package android.kma.myquizzapp.feature.quiz_manage.domain.model

import android.kma.myquizzapp.core.common.model.Quiz

/**
 * Transfer object từ Domain layer chứa quiz và ownership status.
 *
 * Dùng để tránh Presentation layer phải inject AuthRepository trực tiếp.
 * Ownership check logic được tập trung trong GetQuizWithOwnershipUseCase.
 *
 * @property quiz Quiz data
 * @property isOwner true nếu user hiện tại là owner, false nếu không phải hoặc chưa đăng nhập
 */
data class QuizWithOwnership(
    val quiz: Quiz,
    val isOwner: Boolean
)
