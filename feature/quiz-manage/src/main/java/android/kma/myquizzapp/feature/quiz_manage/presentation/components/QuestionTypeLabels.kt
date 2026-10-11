package android.kma.myquizzapp.feature.quiz_manage.presentation.components

import android.kma.myquizzapp.core.common.model.QuestionType

internal fun questionTypeLabel(type: QuestionType): String = when (type) {
    QuestionType.MULTIPLE_CHOICE -> "Chọn 1 đáp án"
    QuestionType.MULTIPLE_SELECT -> "Chọn nhiều đáp án"
    QuestionType.SHORT_ANSWER -> "Trả lời ngắn"
    QuestionType.LONG_ANSWER -> "Trả lời dài"
}
