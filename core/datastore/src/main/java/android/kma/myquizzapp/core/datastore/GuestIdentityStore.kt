package android.kma.myquizzapp.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.guestIdentityDataStore by preferencesDataStore(name = "guest_identity")

/**
 * Lưu UUID định danh cho người chơi khách (guest).
 *
 * Vì sao cần: backend bắt buộc `player_guest_id` dạng uuid khi join mà không có
 * phiên đăng nhập (game.schema.ts). Đây là thứ duy nhất để server nhận ra "vẫn là
 * máy đó" giữa nhiều lần vào phòng, nên PHẢI để cố định theo thiết bị, không sinh
 * mới mỗi lần join (sinh mới thì lịch sử trận của khách ở N39 sẽ vỡ và reconnect
 * sau khi kill app cũng thành người chơi khác).
 *
 * DataStore riêng (file "guest_identity") thay vì nhồi vào SettingsDataStore: đây là
 * định danh, không phải thiết lập — xóa cài đặt (reset settings) không được phép
 * làm mất định danh này.
 *
 * Sinh LƯỜI (lazy): chỉ tạo lần đầu thực sự cần join, không tạo sẵn ở splash —
 * người dùng chỉ xem quiz mãi mãi thì không cần định danh nào cả.
 */
@Singleton
class GuestIdentityStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Lấy uuid đã lưu, hoặc sinh mới và lưu lại nếu chưa có.
     *
     * Dùng `edit` để đọc-ghi trong một transaction: hai lần gọi song song sẽ không
     * sinh ra hai uuid khác nhau.
     */
    suspend fun getOrCreateGuestId(): String {
        val existing = context.guestIdentityDataStore.data.first()[KEY_GUEST_ID]
        if (!existing.isNullOrBlank()) return existing

        var result = ""
        context.guestIdentityDataStore.edit { prefs ->
            val current = prefs[KEY_GUEST_ID]
            result = if (current.isNullOrBlank()) {
                UUID.randomUUID().toString().also { prefs[KEY_GUEST_ID] = it }
            } else {
                current
            }
        }
        return result
    }

    private companion object {
        val KEY_GUEST_ID = stringPreferencesKey("guest_id")
    }
}
