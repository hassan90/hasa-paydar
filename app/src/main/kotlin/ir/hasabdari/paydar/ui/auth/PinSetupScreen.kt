package ir.hasabdari.paydar.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.hasabdari.paydar.ui.theme.*

@Composable
fun PinSetupScreen(
    viewModel: AuthViewModel,
    isInitial: Boolean = true,
    onDone: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    var phase by remember { mutableIntStateOf(0) } // 0=enter, 1=confirm
    var firstPin by remember { mutableStateOf("") }
    var buffer by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun finish() {
        if (buffer.length < 4) { localError = "حداقل ۴ رقم"; return }
        if (phase == 0) {
            firstPin = buffer
            buffer = ""
            phase = 1
            localError = null
        } else {
            if (buffer != firstPin) {
                localError = "رمز و تکرار یکسان نیستند"
                buffer = ""
                return
            }
            viewModel.setupPin(buffer, onDone ?: {})
        }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 24.dp)) {
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(Teal100, Teal200))),
                contentAlignment = Alignment.Center
            ) { Text(if (phase == 0) "🔐" else "✓", fontSize = 48.sp) }

            Spacer(Modifier.height(20.dp))
            Text(
                if (phase == 0) "تعیین رمز ورود" else "تکرار رمز",
                fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Slate900
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (phase == 0)
                    "برای امنیت داده‌های مالی، یک رمز ۴ تا ۶ رقمی تعیین کنید."
                else
                    "رمز را مجدداً وارد کنید تا تأیید شود.",
                fontSize = 12.sp, color = Slate500, textAlign = TextAlign.Center,
                lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(28.dp))

            PinDots(filled = buffer.length, total = 6)

            if (localError != null || state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    localError ?: state.error ?: "",
                    color = Rose600, fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PinKeypad(
                onDigit = { d ->
                    if (buffer.length < 6) { buffer += d; localError = null; viewModel.clearError() }
                },
                onBackspace = { buffer = buffer.dropLast(1); localError = null },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onCancel != null) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("انصراف", color = Slate600, fontWeight = FontWeight.SemiBold) }
                }
                androidx.compose.material3.Button(
                    onClick = ::finish,
                    enabled = buffer.length >= 4,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Teal700)
                ) { Text(if (phase == 0) "بعدی" else "تأیید و ذخیره", fontWeight = FontWeight.Bold) }
            }
        }
    }
}