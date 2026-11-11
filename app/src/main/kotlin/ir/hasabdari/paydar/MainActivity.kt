package ir.hasabdari.paydar

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import ir.hasabdari.paydar.domain.usecase.LockAppUseCase
import ir.hasabdari.paydar.domain.usecase.ObserveDarkModeUseCase
import ir.hasabdari.paydar.domain.usecase.RecordLastActiveAtUseCase
import ir.hasabdari.paydar.domain.usecase.ShouldAutoLockUseCase
import ir.hasabdari.paydar.ui.auth.AuthGate
import ir.hasabdari.paydar.ui.theme.PaydarTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var observeDarkMode: ObserveDarkModeUseCase
    @Inject lateinit var shouldAutoLock: ShouldAutoLockUseCase
    @Inject lateinit var recordActive: RecordLastActiveAtUseCase
    @Inject lateinit var lockApp: LockAppUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkMode by observeDarkMode().collectAsState(initial = false)
            PaydarTheme(darkTheme = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthGate()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            if (shouldAutoLock()) {
                lockApp()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            recordActive()
        }
    }
}