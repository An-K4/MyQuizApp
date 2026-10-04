package android.kma.myquizzapp.core.common.network

import kotlinx.coroutines.flow.StateFlow

/** Trạng thái Internet đã được Android xác thực, dùng chung ở cấp ứng dụng. */
interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}
