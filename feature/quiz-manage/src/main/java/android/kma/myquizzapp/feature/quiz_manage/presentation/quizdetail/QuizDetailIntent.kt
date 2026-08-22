package android.kma.myquizzapp.feature.quiz_manage.presentation.quizdetail

/**
 * User intents for Quiz Detail screen (MVI pattern).
 * 
 * Navigation (back, tạo phòng) được xử lý qua callback truyền trực tiếp vào
 * QuizDetailScreen, không đi qua intent - giống pattern onNavigateToXxx của HomeScreen.
 */
sealed interface QuizDetailIntent {
    /** Load/reload quiz detail */
    data object LoadQuizDetail : QuizDetailIntent
    
    /** Retry sau khi lỗi */
    data object Retry : QuizDetailIntent
}
