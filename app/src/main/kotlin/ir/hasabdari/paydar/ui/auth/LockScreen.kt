package ir.hasabdari.paydar.ui.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ir.hasabdari.paydar.ui.theme.*

@Composable
fun LockScreen(viewModel: AuthViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val lockout = state.lockout
    val canUseBiometric = state.config.biometricEnabled && activity != null &&
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    var showForgotDialog by remember { mutableStateOf(false) }

    fun launchBiometric() {
        if (activity == null) return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.unlockBiometric()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    viewModel.clearError()
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("ورود با اثر انگشت")
            .setSubtitle("اثر انگشت خود را روی سنسور قرار دهید")
            .setNegativeButtonText("انصراف")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        prompt.authenticate(info)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate50)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
            Box(
                Modifier.size(96.dp).clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(Teal100, Teal200))),
                contentAlignment = Alignment.Center
            ) { Text("🔐", fontSize = 48.sp) }

            Spacer(Modifier.height(20.dp))
            Text("خوش آمدید 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Spacer(Modifier.height(8.dp))
            Text(
                if (lockout.isLocked) "لطفاً کمی صبر کنید"
                else "رمز ورود را وارد کنید",
                fontSize = 12.sp, color = Slate500
            )

            Spacer(Modifier.height(28.dp))

            if (lockout.isLocked) {
                val sec = lockout.remainingSeconds
                Text(
                    "%02d:%02d".format(sec / 60, sec % 60),
                    fontSize = 36.sp, fontWeight = FontWeight.Black, color = Rose600
                )
                Spacer(Modifier.height(8.dp))
                Text("ورود موقتاً قفل شد", fontSize = 12.sp, color = Rose600)
            } else {
                PinDots(filled = state.pinBuffer.length, total = 6)
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Rose600, fontSize = 12.sp, textAlign = TextAlign.Center)
            }

            if (canUseBiometric && !lockout.isLocked) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Teal100)
                        .clickable { launchBiometric() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👆", fontSize = 18.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("ورود با اثر انگشت", color = Teal700, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PinKeypad(
                onDigit = viewModel::onDigit,
                onBackspace = viewModel::onBackspace,
                enabled = !lockout.isLocked && !state.isVerifying,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = viewModel::submitPin,
                enabled = state.pinBuffer.length >= 4 && !lockout.isLocked && !state.isVerifying,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal700)
            ) {
                if (state.isVerifying) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("ورود", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))

            Text(
                "رمز را فراموش کردم",
                color = Violet600,
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { showForgotDialog = true }
            )
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = { Text("فراموشی رمز", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "برای بازیابی رمز، داده‌های اپ را از تنظیمات اندروید پاک کنید.\n\n" +
                            "⚠️ این عمل تمام داده‌های مالی (کالاها، فاکتورها، اسناد) را پاک می‌کند.\n\n" +
                            "اگر پشتیبان دارید، بعد از نصب مجدد می‌توانید بازیابی کنید.",
                    fontSize = 12.sp, lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showForgotDialog = false }) { Text("متوجه شدم") }
            }
        )
    }
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is FragmentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}