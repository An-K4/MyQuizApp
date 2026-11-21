# N9 Knowledge: Google One Tap với Credential Manager API

## 📚 **TỔNG QUAN**

Google One Tap là tính năng đăng nhập nhanh cho phép người dùng chọn tài khoản Google đã lưu trên thiết bị mà không cần nhập mật khẩu. Từ Android 14+, Google khuyến nghị sử dụng **Credential Manager API** thay cho `GoogleSignInClient` cũ.

---

## 🔑 **CREDENTIAL MANAGER API**

### **Là gì?**
- API mới của Android (androidx.credentials) để quản lý credentials
- Hỗ trợ nhiều loại credential: password, passkey, federated (Google, Facebook...)
- Thay thế GoogleSignInClient (deprecated từ 2023)

### **Ưu điểm:**
- ✅ API thống nhất cho tất cả loại credential
- ✅ Hỗ trợ Passkey (WebAuthn)
- ✅ Better UX với bottom sheet thay vì full-screen activity
- ✅ Privacy-focused: không share thông tin không cần thiết

### **Dependencies:**
```kotlin
implementation("androidx.credentials:credentials:1.5.0")
implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
```

---

## 🔐 **GOOGLE ONE TAP FLOW**

### **1. Client Request (Android App)**

```kotlin
// Generate nonce for security
val rawNonce = UUID.randomUUID().toString()
val hashedNonce = rawNonce.hashSha256()

// Build Google ID option
val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(false) // Show all Google accounts
    .setServerClientId("YOUR_WEB_CLIENT_ID.apps.googleusercontent.com")
    .setNonce(hashedNonce)
    .build()

// Create credential request
val request = GetCredentialRequest.Builder()
    .addCredentialOption(googleIdOption)
    .build()

// Launch Credential Manager
val result = credentialManager.getCredential(
    request = request,
    context = context,
)
```

### **2. User Selection**
- Bottom sheet hiện danh sách tài khoản Google
- User chọn tài khoản → Google xác thực
- Trả về **ID Token** (JWT)

### **3. Extract ID Token**

```kotlin
val credential = result.credential
if (credential is GoogleIdTokenCredential) {
    val idToken = credential.idToken
    // Send to backend
}
```

### **4. Backend Verification**

Backend verify ID token với Google API:
```typescript
// Backend (Node.js example)
const ticket = await client.verifyIdToken({
    idToken: req.body.idToken,
    audience: WEB_CLIENT_ID,
});
const payload = ticket.getPayload();
// payload.email, payload.name, payload.sub (Google user ID)
```

---

## 🛡️ **SECURITY: NONCE**

### **Tại sao cần nonce?**
- Ngăn chặn **replay attacks** (tấn công phát lại token cũ)
- Đảm bảo ID token chỉ dùng cho request này, không reuse

### **Flow:**
1. Client tạo `rawNonce` (UUID random)
2. Client hash thành `hashedNonce` (SHA-256)
3. Client gửi `hashedNonce` cho Google
4. Google embed `hashedNonce` vào ID token
5. Backend verify token → check nonce khớp

### **SHA-256 Implementation:**

```kotlin
private fun String.hashSha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
```

**Input:** `"abc123-uuid-xyz"`  
**Output:** `"5f3a8b2c..."` (64 hex characters)

---

## 🔧 **WEB CLIENT ID CONFIGURATION**

### **Tại sao cần Web Client ID?**
- Google yêu cầu **Web Client ID** (KHÔNG phải Android Client ID)
- Backend verify token với cùng Web Client ID này

### **Cách lấy Web Client ID:**

1. **Firebase Console:**
   - Project Settings → Service accounts
   - Web API Key → Copy Web Client ID

2. **Google Cloud Console:**
   - APIs & Services → Credentials
   - OAuth 2.0 Client IDs → Web client
   - Copy Client ID

**Format:** `123456789-abc.apps.googleusercontent.com`

### **Cập nhật code:**

```kotlin
val googleIdOption = GetGoogleIdOption.Builder()
    .setServerClientId("YOUR_ACTUAL_WEB_CLIENT_ID.apps.googleusercontent.com")
    // ⚠️ Thay YOUR_ACTUAL_WEB_CLIENT_ID bằng ID thật
    .build()
```

---

## 🎨 **UX BEST PRACTICES**

### **1. Loading State**
```kotlin
val isLoading by viewModel.isLoading.collectAsState()

OutlinedButton(
    onClick = { launchGoogleOneTap() },
    enabled = !isLoading, // Disable khi đang loading
) {
    Text("Tiếp tục với Google")
}
```

### **2. Error Handling**

```kotlin
try {
    val result = credentialManager.getCredential(...)
    // Success
} catch (e: GetCredentialCancellationException) {
    // User cancelled → Do nothing
} catch (e: NoCredentialException) {
    // No Google account on device
    showSnackbar("Không tìm thấy tài khoản Google")
} catch (e: GetCredentialException) {
    // Network error, configuration error, etc.
    showSnackbar("Lỗi đăng nhập: ${e.message}")
}
```

### **3. Fallback Options**
- Luôn có Email/Password login làm fallback
- Không bắt buộc người dùng phải dùng Google

---

## 🔗 **BACKEND INTEGRATION**

### **Endpoint:**
```
POST /auth/google/one-tap
Content-Type: application/json

{
  "idToken": "eyJhbGc..."
}
```

### **Response:**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 123,
      "email": "user@example.com",
      "fullname": "John Doe"
    }
  }
}
```

### **Cookies:**
Backend set `accessToken` và `refreshToken` cookies (HttpOnly).

---

## 📊 **SO SÁNH VỚI GOOGLELOGINCLIENT CŨ**

| Feature | GoogleSignInClient (Old) | Credential Manager (New) |
|---------|--------------------------|--------------------------|
| API | Google Sign-In SDK | androidx.credentials |
| Status | Deprecated 2023 | Recommended |
| UI | Full-screen Activity | Bottom Sheet |
| Passkey | ❌ Not supported | ✅ Supported |
| Privacy | Share nhiều info | Share ít hơn |
| Dependencies | 3+ libraries | 2 libraries |

---

## ⚠️ **COMMON ISSUES & FIX**

### **Issue 1: "Developer Error" khi test**
**Nguyên nhân:** Web Client ID sai hoặc chưa config  
**Fix:** 
1. Check Firebase Console → Web Client ID đúng chưa
2. Thay `YOUR_WEB_CLIENT_ID` trong code
3. Clean & rebuild project

### **Issue 2: "No credentials found"**
**Nguyên nhân:** Không có tài khoản Google trên thiết bị  
**Fix:** Settings → Accounts → Add Google Account

### **Issue 3: Backend return 401**
**Nguyên nhân:** Backend verify token với Client ID khác  
**Fix:** Backend phải dùng CÙNG Web Client ID với Android app

---

## 📖 **TÀI LIỆU THAM KHẢO**

- [Credential Manager Official Guide](https://developer.android.com/training/sign-in/credential-manager)
- [Google Sign-In Migration Guide](https://developers.google.com/identity/android-credential-manager)
- [GetGoogleIdOption API Reference](https://developer.android.com/reference/androidx/credentials/GetGoogleIdOption)

---

## 🎯 **CHECKLIST IMPLEMENTATION**

- [x] Add dependencies (credentials, googleid)
- [x] Create CredentialManager instance
- [x] Build GetGoogleIdOption with Web Client ID
- [x] Generate nonce + hash SHA-256
- [x] Launch getCredential() in coroutine
- [x] Extract ID token from GoogleIdTokenCredential
- [x] Send ID token to LoginViewModel
- [x] Handle errors (cancellation, no credential, network)
- [x] Replace Web Client ID với ID thật từ Firebase Console
- [ ] **TODO: Test trên thiết bị thật với tài khoản Google**

---

**Date:** 2026-08-12  
**Milestone:** M2 - Auth E2E (Week 2, N9)  
**Status:** Implementation complete, pending Web Client ID configuration
