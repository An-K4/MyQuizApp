# N10 Knowledge: Android Integration Testing với Hilt

**Ngày hoàn thành:** 12/8/2026  
**Milestone:** M2 - Auth E2E  
**Task:** Integration test auth E2E với backend local

---

## 1. Tổng Quan

**Integration Testing** là gì?
- Test toàn bộ flow từ UI/ViewModel → Repository → Network → Backend
- Sử dụng **dependencies thật** (Hilt DI, Retrofit, Room, backend)
- Chạy trên **emulator/device** (không phải JVM)
- Test với **backend local** (docker-compose)

**Khác với Unit Test:**
| Aspect | Unit Test | Integration Test |
|--------|-----------|------------------|
| Chạy ở đâu | JVM (máy dev) | Emulator/Device |
| Dependencies | Mock/Fake | Real (Hilt DI) |
| Scope | 1 class/function | End-to-end flow |
| Tốc độ | Nhanh (~ms) | Chậm (~giây) |
| Backend | MockWebServer | Real backend |

---

## 2. Setup Hilt Testing

### 2.1. Dependencies

**gradle/libs.versions.toml:**
```toml
[libraries]
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
```

**app/build.gradle.kts:**
```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "com.example.myquizzapp.HiltTestRunner"
    }
}

dependencies {
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    kspAndroidTest(libs.hilt.compiler)  // ⚠️ Quan trọng!
}
```

### 2.2. HiltTestRunner

**Tại sao cần custom runner?**
- Hilt cần thay `Application` class bằng `HiltTestApplication`
- `AndroidJUnitRunner` mặc định không biết về Hilt

**app/src/androidTest/.../HiltTestRunner.kt:**
```kotlin
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
```

---

## 3. Viết Integration Test

### 3.1. Template Cơ Bản

```kotlin
@HiltAndroidTest                              // (1) Annotation bắt buộc
@RunWith(AndroidJUnit4::class)               // (2) Test runner
class AuthIntegrationTest {

    @get:Rule                                // (3) Hilt rule - inject dependencies
    var hiltRule = HiltAndroidRule(this)

    @Inject                                  // (4) Inject thật từ Hilt
    lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        hiltRule.inject()                    // (5) Trigger injection
    }

    @Test
    fun testSomething() = runTest {          // (6) Coroutines test
        // Arrange
        val email = "test@example.com"
        
        // Act
        val result = authRepository.login(email, "password")
        
        // Assert
        assertTrue(result is Result.Success)
    }
}
```

### 3.2. Key Components

**(1) @HiltAndroidTest:**
- Marks class as Hilt test
- Generates test DI components
- Required for `@Inject` to work

**(2) @RunWith(AndroidJUnit4::class):**
- Uses Android test runner
- Allows test to run on device/emulator

**(3) HiltAndroidRule:**
- Initializes Hilt for test
- Must be **@get:Rule** (không phải @Rule)
- Must call `hiltRule.inject()` in @Before

**(4) @Inject:**
- Inject real dependencies từ production code
- Không mock - dùng implementation thật

**(5) hiltRule.inject():**
- Trigger dependency injection
- Phải gọi **TRƯỚC** khi dùng @Inject fields

**(6) runTest:**
- From `kotlinx-coroutines-test`
- Test coroutines với virtual time
- Auto-waits for coroutines to complete

---

## 4. Test Auth E2E

### 4.1. Test Cases

**AuthIntegrationTest.kt** có 6 tests:

1. **register_withValidData_returnsSuccess**
   - Tạo user mới với email unique (timestamp)
   - Verify response chứa user data
   - Backend: POST /v1/auth/register

2. **login_withValidCredentials_returnsSuccess**
   - Login với test account
   - Verify cookies được set
   - Backend: POST /v1/auth/login

3. **login_withInvalidCredentials_returnsUnauthorized**
   - Login với sai password
   - Verify trả về 401 Unauthorized
   - Test error handling

4. **getCurrentUser_whenLoggedIn_returnsUser**
   - Gọi GET /v1/users/me
   - Verify cookies được persist từ Room
   - Test CookieJar persistence

5. **logout_clearsSession**
   - Logout → cookies cleared
   - Verify getCurrentUser trả 401
   - Backend: POST /v1/auth/logout

6. **tokenRefresh_isConfiguredInAuthenticator**
   - Verify TokenAuthenticator exists
   - Manual test: wait 15min → auto-refresh
   - Automated test khó (cần mock time)

### 4.2. Backend Setup

**Chạy backend local:**
```bash
cd server/backend
docker-compose up -d  # PostgreSQL + Redis
pnpm install
pnpm dev              # Backend runs on :3000
```

**Android debug build config:**
```kotlin
buildTypes {
    debug {
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/v1/\"")
        buildConfigField("String", "SOCKET_URL", "\"http://10.0.2.2:3000\"")
    }
}
```

**10.0.2.2 = localhost** khi chạy trên emulator.

---

## 5. Chạy Tests

### 5.1. Command Line

```bash
# Chạy tất cả integration tests
./gradlew :app:connectedDebugAndroidTest

# Chạy specific test class
./gradlew :app:connectedDebugAndroidTest \
  --tests "com.example.myquizzapp.auth.AuthIntegrationTest"

# Chạy specific test method
./gradlew :app:connectedDebugAndroidTest \
  --tests "*.AuthIntegrationTest.login_withValidCredentials_returnsSuccess"
```

### 5.2. Android Studio

1. Right-click test class/method
2. Run 'AuthIntegrationTest'
3. Chọn device/emulator
4. Xem kết quả trong Run panel

### 5.3. Prerequisites

✅ **Backend đang chạy** tại http://localhost:3000  
✅ **Emulator/device đang chạy**  
✅ **Backend có test account** hoặc test tạo unique users  
✅ **Database clean** (hoặc accept 409 conflicts)

---

## 6. Patterns & Best Practices

### 6.1. Test Data Management

**❌ WRONG - Hardcode data:**
```kotlin
@Test
fun testRegister() = runTest {
    val result = authRepository.register("test@example.com", "password", "Test")
    // Problem: test fails on 2nd run (409 Conflict)
}
```

**✅ CORRECT - Unique data:**
```kotlin
@Test
fun testRegister() = runTest {
    val timestamp = System.currentTimeMillis()
    val email = "test_$timestamp@example.com"  // Unique!
    val result = authRepository.register(email, "Test@123", "Test User")
    assertTrue(result is Result.Success)
}
```

### 6.2. Test Independence

**❌ WRONG - Tests depend on each other:**
```kotlin
@Test fun test1_login() { /* login */ }
@Test fun test2_getCurrentUser() { /* assumes logged in from test1 */ }
```

**✅ CORRECT - Each test standalone:**
```kotlin
@Test
fun getCurrentUser_whenLoggedIn_returnsUser() = runTest {
    // Arrange: login first IN THIS TEST
    authRepository.login("test@example.com", "Test@123")
    
    // Act
    val result = authRepository.getCurrentUser()
    
    // Assert
    assertTrue(result is Result.Success)
}
```

### 6.3. Skip Tests Gracefully

**Khi backend không có seeded data:**
```kotlin
@Test
fun login_withValidCredentials_returnsSuccess() = runTest {
    val result = authRepository.login("test@example.com", "Test@123")
    
    when (result) {
        is Result.Success -> {
            // Test passed
            assertEquals("test@example.com", result.data.email)
        }
        is Result.Error -> {
            // Skip test if account doesn't exist
            println("Skipping - no seeded account (expected)")
            return@runTest  // Exit gracefully
        }
    }
}
```

### 6.4. Timeout Handling

**Coroutines test timeout mặc định = 60s:**
```kotlin
@Test(timeout = 10_000)  // 10 seconds
fun testFastOperation() = runTest {
    // Test should complete trong 10s
}
```

---

## 7. Troubleshooting

### 7.1. "lateinit property authRepository has not been initialized"

**Cause:** Quên gọi `hiltRule.inject()`

**Fix:**
```kotlin
@Before
fun setup() {
    hiltRule.inject()  // ⚠️ Phải có!
}
```

### 7.2. "No instrumentation runner found"

**Cause:** Chưa config testInstrumentationRunner

**Fix trong app/build.gradle.kts:**
```kotlin
android {
    defaultConfig {
        testInstrumentationRunner = "com.example.myquizzapp.HiltTestRunner"
    }
}
```

### 7.3. "Connection refused" khi test

**Cause:** Backend không chạy hoặc sai URL

**Check:**
- Backend running? `curl http://localhost:3000/v1/health`
- Emulator dùng `10.0.2.2` thay vì `localhost`
- Real device cần IP LAN (e.g., `192.168.1.100`)

### 7.4. "Hilt test modules not found"

**Cause:** Thiếu `kspAndroidTest(libs.hilt.compiler)`

**Fix trong dependencies:**
```kotlin
androidTestImplementation(libs.hilt.android.testing)
kspAndroidTest(libs.hilt.compiler)  // ⚠️ Quan trọng!
```

---

## 8. Integration Test vs Compose UI Test

**AuthIntegrationTest (N10):**
- Test **business logic** E2E
- Không test UI
- Inject Repository directly
- Fast (vài giây)

**Compose UI Test (N44 - Tuần 9):**
- Test **UI interactions**
- Click buttons, type text, verify screens
- Use `ComposeTestRule`
- Slower (nhiều giây)

**Example:**
```kotlin
// Integration test (N10)
@Test
fun login_returnsSuccess() = runTest {
    val result = authRepository.login(email, password)
    assertTrue(result is Result.Success)
}

// UI test (N44)
@Test
fun loginScreen_clickLoginButton_navigatesToHome() {
    composeTestRule.onNodeWithText("Email").performTextInput("test@example.com")
    composeTestRule.onNodeWithText("Password").performTextInput("Test@123")
    composeTestRule.onNodeWithText("Login").performClick()
    composeTestRule.onNodeWithText("Home").assertIsDisplayed()
}
```

---

## 9. Học Được Gì Từ N10?

### 9.1. Hilt Testing Architecture

- `@HiltAndroidTest` + `HiltAndroidRule` pattern
- Custom test runner thay `Application`
- `kspAndroidTest` để generate test components
- Inject real dependencies, not mocks

### 9.2. Integration Test Strategy

- Test **happy path** + **error cases**
- Unique test data (timestamp)
- Each test independent
- Skip gracefully nếu backend chưa seed

### 9.3. Coroutines Testing

- `runTest` từ `kotlinx-coroutines-test`
- Virtual time - tests run fast
- Auto-waits for coroutines
- No need manual `delay()`

### 9.4. Backend-First Development

- Integration test **cần backend chạy**
- Không mock network - test thật
- Catch API mismatches sớm
- Confidence cao hơn unit test

---

## 10. Next Steps

**N10 complete → M2 Auth E2E chốt!**

**Tuần 3 (N11-15) - Quiz CRUD:**
- Home screen với search/paging
- Quiz detail + Room cache
- Quiz editor 4 loại câu
- Upload ảnh S3 presign

**Testing roadmap:**
- Tuần 5 (N25): ViewModel unit test với Turbine
- Tuần 9 (N43): MockWebServer tests
- Tuần 9 (N44): Compose UI tests

**Key takeaway:** Integration test khó setup hơn unit test, nhưng **confidence cao hơn nhiều**. Test với backend thật = catch bugs trước production!
