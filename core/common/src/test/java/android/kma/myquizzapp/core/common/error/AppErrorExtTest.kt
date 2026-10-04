package android.kma.myquizzapp.core.common.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppErrorExtTest {
    @Test
    fun `backend codes keep specific user messages`() {
        assertEquals(
            "Quản trị viên không thể khóa chính tài khoản của mình",
            AppError.Api("ADMIN_CANNOT_BAN_SELF").toUserMessage()
        )
        assertEquals(
            "Quiz không tồn tại hoặc đã bị xóa",
            AppError.Api("QUIZ_NOT_FOUND").toUserMessage()
        )
    }

    @Test
    fun `unknown and server errors do not expose internal details`() {
        assertEquals(
            "Đã có lỗi xảy ra, vui lòng thử lại",
            AppError.Unknown(IllegalStateException("secret stack detail")).toUserMessage()
        )
        assertEquals(
            "Dịch vụ tạm thời gián đoạn, vui lòng thử lại sau",
            AppError.Server(503).toUserMessage()
        )
    }

    @Test
    fun `session terminal classification does not treat network as logout`() {
        assertTrue(AppError.Api("AUTH_TOKEN_INVALID").isSessionTerminal)
        assertTrue(AppError.Api("USER_DEACTIVATED").isSessionTerminal)
        assertFalse(AppError.Network.isSessionTerminal)
        assertFalse(AppError.Server(503).isSessionTerminal)
    }

    @Test
    fun `missing resource remains context specific`() {
        assertTrue(AppError.Api("QUIZ_NOT_FOUND").isMissingResource("QUIZ_NOT_FOUND"))
        assertFalse(AppError.Api("QUIZ_NOT_FOUND").isMissingResource("GAME_ROOM_NOT_FOUND"))
        assertTrue(AppError.Gone.isMissingResource("GAME_ROOM_NOT_FOUND"))
        assertTrue(AppError.NotFound.isMissingResource("QUIZ_NOT_FOUND"))
    }
}
