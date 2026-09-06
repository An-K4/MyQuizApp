package android.kma.myquizzapp.feature.home.domain.usecase

import android.kma.myquizzapp.core.common.model.HomeSection
import android.kma.myquizzapp.core.common.repository.QuizRepository
import android.kma.myquizzapp.core.common.result.Result
import javax.inject.Inject

/**
 * Use case to fetch home content (sections of quiz cards).
 * 
 * Backend tự quyết định trả những section nào dựa trên cookie phiên và bảng
 * cấu hình `home_sections`. ĐỪNG liệt kê tên section ở đây: tiêu đề sửa được
 * bằng SQL, số lượng section đổi được bất kỳ lúc nào, và section rỗng bị lọc
 * trước khi trả về — mọi danh sách ghi cứng trong KDoc sẽ sai rất nhanh.
 *
 * Phần duy nhất ổn định: `sectionType` (5 giá trị, xem `SectionType`), và
 * `continue` chỉ có khi đã đăng nhập.
 */
class GetHomeContentUseCase @Inject constructor(
    private val quizRepository: QuizRepository
) {
    /**
     * Fetch home sections.
     * 
     * @return Success with list of HomeSection, or Error with AppError
     */
    suspend operator fun invoke(): Result<List<HomeSection>> {
        return quizRepository.getHomeContent()
    }
}
