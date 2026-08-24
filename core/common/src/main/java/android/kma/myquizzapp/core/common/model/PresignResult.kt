package android.kma.myquizzapp.core.common.model

/**
 * Kết quả xin phép upload ảnh — khớp response POST /storage/presign
 * (storage.controller.ts → success(res, { presignedUrl: result })).
 *
 * - uploadUrl: URL đã ký (AWS SigV4), PUT trực tiếp ảnh lên đây, hết hạn 5 phút,
 *   KHÔNG gửi cookie/auth khi gọi (xem StorageRepositoryImpl).
 * - publicUrl: URL xem ảnh công khai, có ngay từ bước presign (không cần đợi
 *   upload xong) — dùng để lưu vào quiz_image/question_image khi tạo quiz.
 * - key: đường dẫn object trên storage ({folder}/{userId}/{uuid}) — hiện tại
 *   không dùng ở client (web cũng không dùng), giữ lại cho đủ theo response thật.
 */
data class PresignResult(
    val uploadUrl: String,
    val publicUrl: String,
    val key: String
)
