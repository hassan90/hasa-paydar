package ir.hasabdari.paydar.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.ui.theme.*

@Composable
fun ChangePinScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var phase by remember { mutableIntStateOf(0) } // 0=old, 1=new, 2=confirm
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var buffer by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    fun next() {
        if (buffer.length < 4) {
            localError = "حداقل ۴ رقم"
            return
        }
        when (phase) {
            0 -> { oldPin = buffer; buffer = ""; phase = 1; localError = null }
            1 -> { newPin = buffer; buffer = ""; phase = 2; localError = null }
            2 -> {
                if (buffer != newPin) {
                    localError = "رمز جدید و تکرار یکسان نیستند"
                    buffer = ""
                    return
                }
                viewModel.changePin(oldPin, newPin, onDone)
            }
        }
    }

    val titles = listOf("رمز فعلی", "رمز جدید", "تکرار رمز جدید")
    val subtitles = listOf(
        "رمز فعلی خود را وارد کنید",
        "رمز ۴ تا ۶ رقمی جدید را تعیین کنید",
        "رمز جدید را مجدداً وارد کنید"
    )

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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // دکمه بازگشت
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "→",
                    fontSize = 22.sp,
                    color = Slate900,
                    modifier = Modifier.clickable(onClick = onBack).padding(8.dp)
                )
            }

            Box(
                Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Teal100, Teal200))),
                contentAlignment = Alignment.Center
            ) { Text("🔑", fontSize = 36.sp) }

            Spacer(Modifier.height(16.dp))
            Text(titles[phase], fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Spacer(Modifier.height(6.dp))
            Text(subtitles[phase], fontSize = 12.sp, color = Slate500, textAlign = TextAlign.Center)

            Spacer(Modifier.height(24.dp))
            PinDots(filled = buffer.length, total = 6)

            val err = localError ?: state.error
            if (err != null) {
                Spacer(Modifier.height(12.dp))
                Text(err, color = Rose600, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PinKeypad(
                onDigit = { d ->
                    if (buffer.length < 6) {
                        buffer += d
                        localError = null
                        viewModel.clearError()
                    }
                },
                onBackspace = {
                    buffer = buffer.dropLast(1)
                    localError = null
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = ::next,
                enabled = buffer.length >= 4,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (phase == 2) Green600 else Teal700
                )
            ) {
                Text(
                    if (phase == 2) "ذخیره رمز جدید" else "بعدی",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}