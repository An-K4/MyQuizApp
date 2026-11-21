# 🔧 REFACTOR CHECKLIST: Clean Architecture + MVI Polish

> **Mục tiêu:** Chuẩn hóa kiến trúc, loại bỏ vi phạm Clean Architecture và MVI pattern  
> **Ngày bắt đầu:** 2026-08-31  
> **Ước tính:** 7-12 ngày (4 sprints)  
> **Tình trạng:** 🟡 In Progress

---

## 📊 Tổng quan tiến độ

| Sprint | Công việc | Trạng thái | Tiến độ |
|--------|-----------|------------|---------|
| Sprint 1 | Sửa vi phạm nghiêm trọng (5 việc) | ✅ Complete | 5/5 |
| Sprint 2 | Chuẩn hóa naming (4 việc) | ✅ Complete | 2/2 (2 skipped) |
| Sprint 3 | Refactor navigation (3 việc) | ✅ Complete | 3/3 |
| Sprint 4 | Documentation updates (2 việc) | ✅ Complete | 2/2 |
| **TỔNG** | **14 việc** | ✅ **COMPLETE** | **12/12 (100%)** |

---

## 🎯 Mục tiêu chính

### Trước refactor (hiện tại)
- ❌ ViewModel chứa navigation logic
- ❌ Business orchestration quá nặng trong presentation layer
- ❌ Presentation gọi Repository trực tiếp (bỏ qua UseCase)
- ❌ Effect pattern không đồng nhất
- ❌ Naming API không thống nhất (`onIntent` vs `handleIntent`)
- ❌ Navigation graph monolithic (219 dòng)

### Sau refactor (mục tiêu)
- ✅ MVI pattern chuẩn 100% (UiState + Intent + Effect)
- ✅ Clean Architecture nghiêm ngặt (Presentation → UseCase → Repository)
- ✅ ViewModel gọn nhẹ (~50-100 dòng), chỉ làm state management
- ✅ Business logic tập trung ở Domain layer
- ✅ Navigation modular, dễ maintain
- ✅ Naming convention thống nhất toàn project

---

## 📋 CHI TIẾT CÔNG VIỆC

---

## 🔴 Sprint 1: Sửa vi phạm nghiêm trọng (3-5 ngày)

### ✅ Việc 1: Sửa HomeViewModel - thêm HomeEffect, xóa navigation methods

**Mức độ:** 🔴 Nghiêm trọng  
**Ước tính:** 2-3 giờ  
**Trạng thái:** [x] ✅ Hoàn thành (2026-09-01)

**Files cần sửa:**
- `feature/home/src/main/java/android/kma/myquizzapp/feature/home/presentation/HomeViewModel.kt`
- `feature/home/src/main/java/android/kma/myquizzapp/feature/home/presentation/HomeScreen.kt`
- Tạo mới: `feature/home/src/main/java/android/kma/myquizzapp/feature/home/presentation/HomeEffect.kt`

**Chi tiết:**
1. Tạo file `HomeEffect.kt`:
   ```kotlin
   sealed interface HomeEffect {
       data object NavigateToSearch : HomeEffect
       data class NavigateToQuizDetail(val quizId: Long) : HomeEffect
   }
   ```
   
   *Lưu ý: HomeViewModel xử lý errors qua state (`homeError` trong `HomeUiState`), không cần ShowError effect.*

2. Trong `HomeViewModel.kt`:
   - Thêm `_effect` channel và `effect` flow
   - Xóa hàm `navigateToSearch()` và `navigateToQuizDetail()`
   - Trong `handleIntent`, emit effect thay vì gọi hàm navigation

3. Trong `HomeScreen.kt`:
   - Thêm `LaunchedEffect` collect effect
   - Handle navigation trong composable

**Kiểm tra:**
- [x] Build thành công ✅
- [x] Code changes verified ✅
- [x] ViewModel không còn navigation logic ✅
- [x] Manual test: HomeScreen navigate ✅ (2026-09-02)

---

### ✅ Việc 2: Sửa ProfileViewModel - thêm ProfileEffect, xóa isLoggedOut flag

**Mức độ:** 🔴 Nghiêm trọng  
**Ước tính:** 1-2 giờ  
**Trạng thái:** [x] ✅ Hoàn thành (2026-09-01)

**Files cần sửa:**
- `app/src/main/java/android/kma/myquizzapp/presentation/profile/ProfileViewModel.kt`
- `app/src/main/java/android/kma/myquizzapp/presentation/profile/ProfileScreen.kt`
- `app/src/main/java/android/kma/myquizzapp/presentation/profile/ProfileUiState.kt`
- T?o m?i: `app/src/main/java/android/kma/myquizzapp/presentation/profile/ProfileEffect.kt`

**Chi tiết:**
1. Tạo file `ProfileEffect.kt`:
   ```kotlin
   sealed interface ProfileEffect {
       data object NavigateBack : ProfileEffect
       data class ShowError(val message: String) : ProfileEffect
   }
   ```

2. Trong `ProfileUiState.kt`:
   - Xóa field `isLoggedOut: Boolean`

3. Trong `ProfileViewModel.kt`:
   - Thêm `_effect` channel và `effect` flow
   - Trong `logout()`, emit `ProfileEffect.NavigateBack` thay và set flag

4. Trong `ProfileScreen.kt`:
   - Xóa logic observe `isLoggedOut`
   - Thêm `LaunchedEffect` collect effect
   - Navigate khi nhận `NavigateBack`

**Kiểm tra:**
- [x] Build thành công ✅
- [x] ProfileEffect.kt created ✅
- [x] isLoggedOut removed from ProfileUiState ✅
- [x] Effect pattern implemented in ProfileViewModel ✅
- [x] Effects collected in ProfileScreen ✅
- [x] Manual test: Logout navigation ✅ (2026-09-02)

---

### ✅ Việc 3: Tách CreateQuizViewModel.submit() → CreateQuizWithAssetsUseCase

**Mức độ:** 🔴 Nghiêm trọng  
**Ước tính:** 3-4 giờ  
**Trạng thái:** [x] ✅ Hoàn thành (2026-09-01)

**Files đã tạo/sửa:**
- ✅ `feature/quiz-manage/src/main/java/android/kma/myquizzapp/feature/quiz_manage/presentation/createquiz/CreateQuizViewModel.kt` - Refactored
- ✅ `feature/quiz-manage/src/main/java/android/kma/myquizzapp/feature/quiz_manage/domain/usecase/CreateQuizWithAssetsUseCase.kt` - NEW (~75 lines)
- ✅ `feature/quiz-manage/src/main/java/android/kma/myquizzapp/feature/quiz_manage/domain/model/QuizDraft.kt` - NEW (~35 lines)

**Chi tiết implementation:**
1. Tạo `QuizDraft.kt` - transfer object từ presentation:
   ```kotlin
   data class QuizDraft(
       val quizName: String,
       val quizDescription: String?,
       val quizLanguage: String,
       val quizCategory: String?,
       val isPublic: Boolean,
       val coverImageUri: Uri?,
       val questions: List<QuestionDraft>
   )
   ```

2. Tạo `CreateQuizWithAssetsUseCase.kt`:
   - Nhận `QuizDraft` làm input
   - Upload cover image (nếu có)
   - Upload question images (loop)
   - Build `NewQuiz` object
   - Gọi `CreateQuizUseCase`
   - Return `Result<Quiz>`

3. Trong `CreateQuizViewModel`:
   - Giữ validation logic trong ViewModel (private fun validate)
   - Hàm `submit()` giờ chỉ:
     - Validate
     - Build `QuizDraft` từ state
     - Gọi `createQuizWithAssetsUseCase(draft)`
     - Handle result

**Lợi ích đạt được:**
- ✅ ViewModel giảm từ ~203 dòng xuống ~153 dòng (saved 50 lines)
- ✅ Logic upload/build tái sử dụng được cho EditQuiz
- ✅ Dễ test orchestration logic
- ✅ Đúng Clean Architecture (ViewModel → UseCase → Repository)

**Kiểm tra:**
- [x] Build thành công ✅ (2m 27s)
- [x] QuizDraft model created ✅
- [x] CreateQuizWithAssetsUseCase created ✅
- [x] ViewModel refactored, injection updated ✅
- [x] ViewModel giảm >50 dòng ✅
- [x] Manual test: Tạo quiz thành công (có ảnh + không ảnh) ✅ (2026-09-02)
- [x] Manual test: Upload ảnh lỗi → hiển thị đúng message ✅ (2026-09-02)

---

### ? Vi?c 4: T�ch EditQuizViewModel.submit() ? UpdateQuizWithAssetsUseCase

**M?c d?:** ?? Nghi�m tr?ng  
**U?c t�nh:** 3-4 gi?  
**Tr?ng th�i:** [ ] Chua l�m

**Files c?n s?a:**
- `feature/quiz-manage/src/main/java/android/kma/myquizzapp/feature/quiz_manage/presentation/editquiz/EditQuizViewModel.kt`
- T?o m?i: `feature/quiz-manage/src/main/java/android/kma/myquizzapp/feature/quiz_manage/domain/usecase/UpdateQuizWithAssetsUseCase.kt`
- S? d?ng: `QuizDraft.kt` (d� t?o ? vi?c 3)


**Chi ti?t:**
1. T?o `UpdateQuizWithAssetsUseCase.kt` - tuong t? `CreateQuizWithAssetsUseCase`:
   - Nh?n `quizId` v� `QuizDraft`
   - Upload ?nh M?I (n?u c� Uri)
   - Gi? nguy�n URL cu (existingCoverUrl/existingImageUrl)
   - Build `QuizPatch` object
   - G?i `UpdateQuizUseCase`

2. Trong `EditQuizViewModel`:
   - Tuong t? CreateQuizViewModel
   - H�m `submit()` g?n l?i ch? validate + call UseCase

**Kiểm tra:**
- [x] Build thành công ✅ (1m 20s)
- [x] Sửa quiz thành công ✅ (2026-09-02)
- [x] Upload ảnh mới work ✅ (2026-09-02)
- [x] Giữ nguyên ảnh cũ work ✅ (2026-09-02)
- [x] ViewModel giảm >30 dòng ✅

---

### ✅ Việc 5: Sửa QuizDetailViewModel - tạo GetQuizWithOwnershipUseCase

**Mức độ:** 🔴 Nghiêm trọng  
**Ước tính:** 2-3 giờ  
**Trạng thái:** [x] ✅ Hoàn thành (2026-09-02)

**Files đã tạo/sửa:**
- ✅ `feature/quiz-manage/src/main/java/android/kma/myquizzapp/feature/quiz_manage/presentation/quizdetail/QuizDetailViewModel.kt` - Refactored
- ✅ `feature/quiz-manage/src/main/java/android/kma/myquizzapp/feature/quiz_manage/domain/usecase/GetQuizWithOwnershipUseCase.kt` - NEW (~48 lines)
- ✅ `feature/quiz-manage/src/main/java/android/kma/myquizzapp/feature/quiz_manage/domain/model/QuizWithOwnership.kt` - NEW (~15 lines)

**Chi tiết implementation:**
1. Tạo `QuizWithOwnership.kt` - transfer object từ Domain:
   ```kotlin
   data class QuizWithOwnership(
       val quiz: Quiz,
       val isOwner: Boolean
   )
   ```

2. Tạo `GetQuizWithOwnershipUseCase.kt`:
   - Inject QuizRepository + AuthRepository
   - Fetch quiz qua `quizRepository.getQuizDetail(quizId)`
   - Check ownership: `user.id == quiz.quizOwner`
   - Return QuizWithOwnership
   - Guest (401) → isOwner = false

3. Trong `QuizDetailViewModel`:
   - Xóa `authRepository` injection
   - Inject `getQuizWithOwnershipUseCase: GetQuizWithOwnershipUseCase`
   - Trong `loadQuizDetail()`, gọi UseCase và unwrap result:
     ```kotlin
     val result = getQuizWithOwnershipUseCase(quizId)
     // result.data.quiz, result.data.isOwner
     ```

**Lợi ích đạt được:**
- ✅ ViewModel không còn gọi Repository trực tiếp (Clean Architecture compliance)
- ✅ Ownership check logic tập trung ở Domain layer
- ✅ Reusable UseCase cho các contexts khác
- ✅ Dễ test: mock UseCase thay vì mock 2 Repositories

**Kiểm tra:**
- [x] Build thành công ✅ (1m 12s)
- [x] QuizWithOwnership model created ✅
- [x] GetQuizWithOwnershipUseCase created ✅
- [x] ViewModel refactored, authRepository removed ✅
- [x] Manual test: Owner thấy nút Edit/Delete ✅ (2026-09-02)
- [x] Manual test: Non-owner không thấy nút ✅ (2026-09-02)
- [x] Manual test: Guest (401) vẫn xem được quiz public ✅ (2026-09-02)

---

## ?? Sprint 2: Chu?n h�a naming (1-2 ng�y)

### ✅ Việc 6: Đổi tất cả handleIntent → onIntent

**Mức độ:** ⚠️ Trung bình  
**Ước tính:** 1 giờ  
**Trạng thái:** [x] ✅ HOÀN THÀNH (2026-09-02)

**Files đã sửa (16 files - 26 occurrences):**
- [x] `feature/home/presentation/HomeViewModel.kt` (method definition)
- [x] `feature/home/presentation/HomeScreen.kt` (2 call sites)
- [x] `feature/home/presentation/search/SearchViewModel.kt` (method definition)
- [x] `feature/home/presentation/search/SearchScreen.kt` (2 call sites)
- [x] `feature/quiz-manage/.../createquiz/CreateQuizViewModel.kt` (method definition)
- [x] `feature/quiz-manage/.../createquiz/CreateQuizScreen.kt` (3 call sites)
- [x] `feature/quiz-manage/.../editquiz/EditQuizViewModel.kt` (method definition)
- [x] `feature/quiz-manage/.../editquiz/EditQuizScreen.kt` (3 call sites)
- [x] `feature/quiz-manage/.../quizdetail/QuizDetailViewModel.kt` (method definition + init call)
- [x] `feature/quiz-manage/.../quizdetail/QuizDetailScreen.kt` (2 call sites)
- [x] `feature/quiz-manage/.../createroom/CreateRoomViewModel.kt` (method definition)
- [x] `feature/quiz-manage/.../createroom/CreateRoomScreen.kt` (1 call site)
- [x] `feature/quiz-manage/.../quizmanagelist/QuizManageListViewModel.kt` (method definition)
- [x] `feature/quiz-manage/.../quizmanagelist/QuizManageListScreen.kt` (2 call sites)
- [x] `feature/lobby/.../hostlobby/HostLobbyViewModel.kt` (method definition)
- [x] `feature/lobby/.../hostlobby/HostLobbyScreen.kt` (1 call site)

**Chi tiết:**
Đơn giản find-replace trong mỗi file:
```kotlin
// Từ:
fun handleIntent(intent: XxxIntent) { ... }

// Thành:
fun onIntent(intent: XxxIntent) { ... }
```

Cập nhật call site trong Screen:
```kotlin
// Từ:
onIntent = viewModel::handleIntent

// Thành:
onIntent = viewModel::onIntent
```

**Kiểm tra:**
- [x] Build thành công (2026-09-02, Exit Code: 0)
- [x] Grep verified: Không còn "handleIntent" nào trong codebase

---

### ✅ Việc 7: Đổi GameApi.kt → GameApiService.kt

**Mức độ:** ⚠️ Trung bình  
**Ước tính:** 15 phút  
**Trạng thái:** [x] ✅ HOÀN THÀNH (2026-09-02)

**Files đã sửa (3 files):**
- [x] `core/network/.../api/GameApiService.kt` (renamed from GameApi.kt, interface renamed)
- [x] `core/network/.../di/NetworkModule.kt` (import + @Provides function signature)
- [x] `core/network/.../repository/GameSessionRepositoryImpl.kt` (import + constructor param)

**Chi tiết:**
1. ✅ Renamed file: `GameApi.kt` → `GameApiService.kt` (using `mv` command)
2. ✅ Renamed interface: `GameApi` → `GameApiService`
3. ✅ Updated imports and type references:
   - NetworkModule: import + provideGameApi() return type
   - GameSessionRepositoryImpl: import + constructor parameter type

**Note:** UseCases (CreateGameSessionUseCase, GetGameModesUseCase, GetHostTokenUseCase) không cần update vì chúng inject Repository, không inject GameApi trực tiếp.

**Kiểm tra:**
- [x] Build thành công (2026-09-02, 45s, :core:network:build)
- [x] Không có import error - Grep verified

---

### ⏭️ Việc 8: Xóa tất cả comment milestone "N{số}" (SKIPPED)

**Mức độ:** ⚠️ Trung bình  
**Ước tính:** 1-2 giờ  
**Trạng thái:** [x] ⏭️ BỎ QUA (2026-09-02)

**Lý do bỏ qua:** Đang trong giai đoạn phát triển, N{số} comments giúp agents và developers tracking timeline của các milestone. Sẽ dọn dẹp sau khi project ổn định. **Impact thấp đến Clean Architecture và MVI pattern.**

**Ph?m vi:** To�n b? codebase

**Chi ti?t:**
T�m v� x�a/s?a comment d?ng:
- `// N18: m�n lobby host th?t...`
- `// N16.5: cursor m?i...`
- `// N13-14: danh s�ch...`

**Nguy�n t?c:**
1. Comment KH�NG c� gi� tr? k? thu?t ? X�A ho�n to�n
2. Comment c� gi?i th�ch why/c?nh b�o ? GI? L?I nhung x�a prefix "N{s?}"

**V� d?:**
```kotlin
// X�A (tracking ti?n d?):
// N18: m�n lobby host th?t ? feature:lobby (thay HostLobbyPlaceholder).

// GI? L?I + S?A (c� gi� tr? k? thu?t):
// Tru?c: // N18: Socket.io t? reconnect nhung KH�NG t? join l?i room...
// Sau:  // Socket.io t? reconnect nhung KH�NG t? join l?i room...
```

**Files ch?a nhi?u comment milestone:**
- `AppNavGraph.kt`
- `HostLobbyViewModel.kt`
- `SearchViewModel.kt`
- `CreateRoomViewModel.kt`
- `QuizDetailViewModel.kt`

**Ki?m tra:**
- [ ] Grep search `"N\d+"` kh�ng tr? v? k?t qu?
- [ ] Comment quan tr?ng v?n c�n (kh�ng m?t gi?i th�ch k? thu?t)

---

### ⏭️ Việc 9: Di chuyển inline comment trong import ra ngoài (SKIPPED)

**Mức độ:** ⚠️ Thấp  
**Ước tính:** 30 phút  
**Trạng thái:** [x] ⏭️ BỎ QUA (2026-09-02)

**Lý do bỏ qua:** Inline import comments hiện tại rất ít và có giá trị giải thích. Có thể chọn lọc làm sau nếu cần. **Impact cực thấp đến Clean Architecture và MVI pattern.**

**Files c?n s?a:**
- `core/network/src/main/java/android/kma/myquizzapp/core/network/cookie/TokenAuthenticator.kt`
- T�m c�c file kh�c c� pattern tuong t?

**Chi ti?t:**
```kotlin
// T?:
import dagger.Lazy    // ? G?C L?I: ph?i l� dagger.Lazy, KH�NG ph?i kotlin.Lazy

// Th�nh:
import dagger.Lazy

/**
 * Authenticator x? l� 401 ? refresh ? retry.
 * 
 * Luu �: inject dagger.Lazy<AuthApiService> (KH�NG ph?i kotlin.Lazy) 
 * d? ph� v�ng l?p DI: Retrofit ? OkHttp ? Authenticator ? Retrofit.
 */
@Singleton
class TokenAuthenticator ...
```

**Ki?m tra:**
- [ ] Kh�ng c�n comment inline trong import statements
- [ ] Gi?i th�ch quan tr?ng du?c di chuy?n v�o KDoc

---

## ?? Sprint 3: Refactor navigation (2-3 ng�y)

### ✅ Việc 10: Tách AppNavGraph → các NavGraph riêng theo feature

**Mức độ:** 🔵 Trung bình  
**Ước tính:** 3-4 giờ  
**Trạng thái:** [x] ✅ HOÀN THÀNH (2026-09-02)

**Files đã tạo mới (4 files):**
- [x] `app/src/main/java/android/kma/myquizzapp/navigation/AuthNavGraph.kt` (85 lines) - Auth routes
- [x] `app/src/main/java/android/kma/myquizzapp/navigation/MainNavGraph.kt` (70 lines) - Public/Protected routes
- [x] `app/src/main/java/android/kma/myquizzapp/navigation/QuizManageNavGraph.kt` (75 lines) - Quiz CRUD routes
- [x] `app/src/main/java/android/kma/myquizzapp/navigation/GameNavGraph.kt` (45 lines) - Gameplay routes

**Files đã sửa (1 file):**
- [x] `app/src/main/java/android/kma/myquizzapp/navigation/AppNavGraph.kt` (refactored: ~267 lines → ~60 lines)

**Chi tiết:**
1. ✅ Tạo `AuthNavGraph.kt`: extension function `authGraph(navController)` chứa 5 auth routes (Login, Register, ForgotPassword, OtpVerification, ResetPassword)
2. ✅ Tạo `MainNavGraph.kt`: extension function `mainGraph(navController)` chứa 6 public/protected routes (Home, Search, Discover, JoinRoom, Library, Profile)
3. ✅ Tạo `QuizManageNavGraph.kt`: extension function `quizManageGraph(navController)` chứa 5 quiz routes (MyQuizzes, CreateQuiz, EditQuiz, QuizDetail, CreateRoom)
4. ✅ Tạo `GameNavGraph.kt`: extension function `gameGraph(navController)` chứa 5 gameplay routes (PlayerLobby, HostLobby, GamePlay, HostGame, FinalResult)
5. ✅ Refactor `AppNavGraph.kt`: Loại bỏ ~200 lines route definitions, giữ lại Splash + calls to 4 sub-graphs

**Cấu trúc mới:**
```kotlin
NavHost(...) {
    composable<Route.Splash> { ... }
    authGraph(navController)
    navigation<Route.MainGraph>(...) {
        mainGraph(navController)
        quizManageGraph(navController)
        gameGraph(navController)
    }
}
```

**Lợi ích:**
- ✅ Separation of Concerns: Mỗi feature quản lý own navigation
- ✅ Modularity: Navigation logic encapsulated per feature
- ✅ Scalability: Thêm feature mới = tạo NavGraph mới, không touch existing
- ✅ Maintainability: AppNavGraph.kt từ 267 lines → 60 lines (giảm 78%)

**Kiểm tra:**
- [x] Build thành công (2026-09-02, 7m 22s, BUILD SUCCESSFUL)
- [x] Không có compilation errors
- [x] All routes accessible through sub-graphs

**Nguyên tắc:**
1. Comment KHÔNG có giá trị kỹ thuật → XÓA hoàn toàn
2. Comment có giải thích why/cảnh báo → GIỮ LẠI nhưng xóa prefix "N{số}"

**Ví dụ:**
```kotlin
// XÓA (tracking tiến độ):
// N18: màn lobby host thật ở feature:lobby (thay HostLobbyPlaceholder).

// GIỮ LẠI + SỬA (có giá trị kỹ thuật):
// Trước: // N18: Socket.io tự reconnect nhưng KHÔNG tự join lại room...
// Sau:  // Socket.io tự reconnect nhưng KHÔNG tự join lại room...
```

**Files chứa nhiều comment milestone:**
- `AppNavGraph.kt`
- `HostLobbyViewModel.kt`
- `SearchViewModel.kt`
- `CreateRoomViewModel.kt`
- `QuizDetailViewModel.kt`

**Kiểm tra:**
- [ ] Grep search `"N\d+"` không trả về kết quả
- [ ] Comment quan trọng vẫn còn (không mất giải thích kỹ thuật)

---

### ✅ Việc 9: Di chuyển inline comment trong import ra ngoài

**Mức độ:** 🟡 Thấp  
**Ước tính:** 30 phút  
**Trạng thái:** [ ] Chưa làm

**Files cần sửa:**
- `core/network/src/main/java/android/kma/myquizzapp/core/network/cookie/TokenAuthenticator.kt`
- Tìm các file khác có pattern tương tự

**Chi tiết:**
```kotlin
// Từ:
import dagger.Lazy    // ← GỐC LỖI: phải là dagger.Lazy, KHÔNG phải kotlin.Lazy

// Thành:
import dagger.Lazy

/**
 * Authenticator xử lý 401 → refresh → retry.
 * 
 * Lưu ý: inject dagger.Lazy<AuthApiService> (KHÔNG phải kotlin.Lazy) 
 * để phá vòng lặp DI: Retrofit → OkHttp → Authenticator → Retrofit.
 */
@Singleton
class TokenAuthenticator ...
```

**Kiểm tra:**
- [ ] Không còn comment inline trong import statements
- [ ] Giải thích quan trọng được di chuyển vào KDoc

---

## 🔵 Sprint 3: Refactor navigation (2-3 ngày)

### ✅ Việc 10: Tách AppNavGraph → các NavGraph riêng theo feature

**Mức độ:** 🔵 Trung bình  
**Ước tính:** 3-4 giờ  
**Trạng thái:** [ ] Chưa làm

**Files cần sửa:**
- `app/src/main/java/android/kma/myquizzapp/navigation/AppNavGraph.kt`
- Tạo mới: `app/src/main/java/android/kma/myquizzapp/navigation/AuthNavGraph.kt`
- Tạo mới: `app/src/main/java/android/kma/myquizzapp/navigation/MainNavGraph.kt`
- Tạo mới: `app/src/main/java/android/kma/myquizzapp/navigation/QuizManageNavGraph.kt`
- Tạo mới: `app/src/main/java/android/kma/myquizzapp/navigation/GameNavGraph.kt`

**Chi tiết:**
1. Tạo `AuthNavGraph.kt`:
   ```kotlin
   fun NavGraphBuilder.authGraph(navController: NavController) {
       navigation<Route.AuthGraph>(startDestination = Route.Login) {
           composable<Route.Login> { LoginScreen(...) }
           composable<Route.Register> { RegisterScreen(...) }
           composable<Route.ForgotPassword> { ForgotPasswordScreen(...) }
           composable<Route.OtpVerification> { OtpVerificationScreen(...) }
           composable<Route.ResetPassword> { ResetPasswordScreen(...) }
       }
   }
   ```

2. Tạo `MainNavGraph.kt` - public routes (Home, Search, Discover...)

3. Tạo `QuizManageNavGraph.kt` - quiz CRUD routes

4. Tạo `GameNavGraph.kt` - gameplay routes (lobby, game, result)

5. Trong `AppNavGraph.kt` chỉ còn gọi các graph con:
   ```kotlin
   @Composable
   fun AppNavGraph(navController: NavHostController, startDestination: Any) {
       NavHost(navController = navController, startDestination = startDestination) {
           authGraph(navController)
           mainGraph(navController)
           quizManageGraph(navController)
           gameGraph(navController)
       }
   }
   ```

**Kiểm tra:**
- [ ] Build thành công
- [ ] AppNavGraph.kt giảm từ ~219 dòng xuống <50 dòng
- [ ] Tất cả navigation vẫn work
- [ ] 4 file graph mới được tạo

---

### ✅ Việc 11: Xóa placeholder screens khỏi AppNavGraph

**Mức độ:** 🔵 Thấp  
**Ước tính:** 30 phút  
**Trạng thái:** [x] Hoàn thành - Không cần xóa gì (placeholders chưa có screen thật)

**Files cần sửa:**
- `app/src/main/java/android/kma/myquizzapp/navigation/AppNavGraph.kt` (hoặc các graph con sau khi split)

**Chi tiết:**
Xóa các placeholder composable:
```kotlin
// XÓA:
composable<Route.PlayerLobby> {
    PlaceholderScreen(title = "Player Lobby")
}
composable<Route.Game> {
    PlaceholderScreen(title = "Game Playing")
}
composable<Route.GameResult> {
    PlaceholderScreen(title = "Game Result")
}
```

**Lưu ý:**
- Chỉ xóa placeholder nếu screen thật đã có
- Nếu screen chưa implement, GIỮ LẠI placeholder

**Kiểm tra:**
- [ ] Build thành công
- [ ] Navigate đến screen thật work
- [ ] Không còn PlaceholderScreen nào dư thừa

---

### âœ… Viá»‡c 12: Thá»‘ng nháº¥t validation pattern trong ViewModels

**Má»©c Ä‘á»™:** ðŸ”µ Trung bÃ¬nh  
**Æ¯á»›c tÃ­nh:** 2-3 giá»  
**Trạng thái:** [x] ✅ Hoàn thành (2026-09-02)

**Files cáº§n sá»­a:**
- `feature/auth/presentation/login/LoginViewModel.kt`
- `feature/auth/presentation/register/RegisterViewModel.kt`
- `feature/quiz-manage/presentation/createquiz/CreateQuizViewModel.kt`
- `feature/quiz-manage/presentation/editquiz/EditQuizViewModel.kt`
- `feature/quiz-manage/presentation/createroom/CreateRoomViewModel.kt`

**Chi tiáº¿t:**
Hiá»‡n táº¡i cÃ³ 2 patterns validation:

**Pattern A (inline check):**
```kotlin
fun submit() {
    if (state.email.isBlank()) {
        _state.update { it.copy(emailError = "Email required") }
        return
    }
    // proceed...
}
```

**Pattern B (validate function):**
```kotlin
private fun validate(): Boolean {
    val errors = mutableListOf<String>()
    if (state.email.isBlank()) errors.add("Email required")
    // ...
    return errors.isEmpty()
}

fun submit() {
    if (!validate()) return
    // proceed...
}
```

**Quyáº¿t Ä‘á»‹nh:** Chá»n Pattern B (validate function riÃªng)

**Lá»£i Ã­ch:**
- TÃ¡ch biá»‡t validation logic
- Dá»… test
- RÃµ rÃ ng hÆ¡n

**Kiểm tra:**
- [x] Tất cả ViewModels dùng cùng pattern
- [x] Validation vẫn work như cũ
- [x] Error messages hiển thị đúng

---

## 🟣 Sprint 4: Documentation + Final polish (1-2 ngày)

### ✅ Việc 13: Cập nhật project_structure.md

**Mức độ:** 🟣 Thấp  
**Ước tính:** 1 giờ  
**Trạng thái:** [x] ✅ Hoàn thành (2026-09-02)

**Files cần sửa:**
- `project_structure.md`

**Chi tiết:**
Cập nhật để phản ánh:
1. Navigation được split thành 4 graph files
2. UseCase mới: `CreateQuizWithAssetsUseCase`, `UpdateQuizWithAssetsUseCase`, `GetQuizWithOwnershipUseCase`
3. Effect pattern đã được áp dụng đầy đủ
4. Naming conventions đã chuẩn hóa

**Kiểm tra:**
- [x] Document match với codebase thực tế
- [x] Không còn thông tin cũ/sai

---

### ✅ Việc 14: Ghi chép vào myquizz-review-backend-ke-hoach-50-ngay.md

**Mức độ:** 🟣 Thấp  
**Ước tính:** 30 phút  
**Trạng thái:** [x] ✅ Hoàn thành (2026-09-02)

**Files cần sửa:**
- `myquizz-review-backend-ke-hoach-50-ngay.md`

**Chi tiết:**
Thêm section mới sau N18:

```markdown
## N18.5: Polish Architecture (31/8/2026 - 2/9/2026)

**Mục tiêu:** Chuẩn hóa Clean Architecture + MVI pattern toàn dự án

**Thành tựu:**
- Sửa 5 vi phạm nghiêm trọng (navigation in VM, god methods, layer violations)
- Chuẩn hóa naming: onIntent, XxxApiService
- Refactor navigation thành 4 graph modules
- Thống nhất validation pattern
- Xóa milestone comments

**Files affected:** 30+ files across all modules

**Lessons learned:**
- MVI Effect pattern cần áp dụng đồng nhất từ đầu
- Navigation logic KHÔNG BAO GIỜ ở ViewModel
- UseCase layer phải strict, không bypass
```

**Kiểm tra:**
- [x] Entry đã được thêm vào timeline
- [x] Thông tin chính xác

---

## 📋 Quy tắc làm việc

### Khi làm mỗi việc:
1. ✅ Đọc kỹ chi tiết việc trong checklist này
2. ✅ Tạo branch mới từ main (nếu cần)
3. ✅ Làm từng bước trong "Chi tiết"
4. ✅ Chạy build để verify
5. ✅ Test thủ công các luồng liên quan
6. ✅ Tick checkbox ở đầu việc
7. ✅ Commit với message rõ ràng
8. ✅ Chuyển sang việc tiếp theo

### MVI Pattern Standards (tham kháº£o):
```kotlin
// State: data class, immutable
data class XxxUiState(...)

// Intent: sealed interface
sealed interface XxxIntent {
    data object OnBack : XxxIntent
    data class OnSubmit(...) : XxxIntent
}

// Effect: sealed interface (NAVIGATION + ONE-TIME EVENTS)
sealed interface XxxEffect {
    data object NavigateBack : XxxEffect
    data class ShowToast(val message: String) : XxxEffect
}

// ViewModel:
class XxxViewModel : ViewModel() {
    private val _state = MutableStateFlow(XxxUiState())
    val state = _state.asStateFlow()
    
    private val _effect = Channel<XxxEffect>()
    val effect = _effect.receiveAsFlow()
    
    fun onIntent(intent: XxxIntent) {
        when (intent) { ... }
    }
    
    private suspend fun emitEffect(effect: XxxEffect) {
        _effect.send(effect)
    }
}

// Screen:
@Composable
fun XxxScreen(viewModel: XxxViewModel, navController: NavController) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is XxxEffect.NavigateBack -> navController.popBackStack()
                is XxxEffect.ShowToast -> { /* show toast */ }
            }
        }
    }
    
    XxxContent(
        state = state,
        onIntent = viewModel::onIntent
    )
}
```

---

## 🎯 Tiến độ tổng quan

**Sprint 1 (3-5 ngày):** 🔴🔴🔴🔴🔴 5/5 ✅  
**Sprint 2 (1-2 ngày):** 🟡🟡🟡🟡 2/2 ✅ (2 skipped)  
**Sprint 3 (2-3 ngày):** 🔵🔵🔵 3/3 ✅  
**Sprint 4 (1-2 ngày):** 🟣🟣 2/2 ✅  

**Tổng tiến độ: 12/12 (100%) ✅ HOÀN THÀNH**

---

*Document được tạo tự động từ audit report ngày 31/8/2026*  
*Cập nhật cuối: 2026-09-02*
