package android.kma.myquizzapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.kma.myquizzapp.navigation.AppNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private var deepLinkToken by mutableStateOf<String?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle deep link from initial intent
        handleDeepLink(intent)
        
        setContent {
            AppNavGraph(initialDeepLinkToken = deepLinkToken)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link when app is already running
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        
        // Check if this is a password reset deep link
        if (uri.scheme == "https" && 
            uri.host == "myquizz.dpdns.org" && 
            uri.pathSegments.firstOrNull() == "reset-password") {
            
            // Extract token from query parameter
            val token = uri.getQueryParameter("token")
            if (token != null) {
                deepLinkToken = token
            }
        }
    }
}