package ir.hasabdari.paydar.ui.settings

import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    onSetupPin: () -> Unit,
    onChangePin: () -> Unit,
    viewModel: SecuritySettingsViewModel = hiltViewModel()
) {
    val config by viewModel.config.collectAsState()
    val context = LocalContext.current
    val canUseBiometric = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    var showDisableConfirm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "امنیت", onBack = onBack)
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionCard("قفل ورود") {
                ToggleRow(
                    icon = "🔐", label = "رمز ورود",
                    subtitle = "قفل اپ با رمز",
                    checked = config.pinEnabled,
                    onChange = { enabled ->
                        if (!enabled) showDisableConfirm = true
                        else if (!config.pinEnabled) onSetupPin()
                    }
                )
                if (config.pinEnabled) {
                    Divider(Modifier.padding(vertical = 6.dp))
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Slate50)
                            .clickable(onClick = onChangePin)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔑", fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("تغییر رمز", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900, modifier = Modifier.weight(1f))
                        Text("‹", fontSize = 16.sp, color = Slate400)
                    }
                }
            }

            if (config.pinEnabled) {
                SectionCard("بیومتریک") {
                    ToggleRow(
                        icon = "👆", label = "اثر انگشت",
                        subtitle = if (canUseBiometric) "ورود سریع با اثر انگشت" else "دستگاه پشتیبانی نمی‌کند",
                        checked = config.biometricEnabled && canUseBiometric,
                        onChange = { enabled -> if (canUseBiometric) viewModel.toggleBiometric(enabled) }
                    )
                }

                SectionCard("قفل خودکار") {
                    Text("قفل بعد از بی‌کاری", fontSize = 12.sp, color = Slate500)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1, 5, 15, 30).forEach { m ->
                            AutoLockChip(
                                minutes = m,
                                selected = config.autoLockMinutes == m,
                                onClick = { viewModel.changeAutoLock(m) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Amber50).padding(12.dp)
            ) {
                Text(
                    "ℹ️ داده‌های شما با SQLCipher (AES-256) رمزنگاری می‌شوند. کلید رمزنگاری در " +
                            "Android Keystore نگه‌داری می‌شود و قابل استخراج نیست.",
                    fontSize = 10.sp, color = Amber700, lineHeight = 16.sp
                )
            }
        }
    }

    if (showDisableConfirm) {
        AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            title = { Text("غیرفعال‌سازی رمز", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "با غیرفعال‌سازی رمز، هر کسی که به گوشی شما دسترسی داشته باشد می‌تواند " +
                            "اطلاعات مالی را ببیند. ادامه می‌دهید؟",
                    fontSize = 13.sp, lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.togglePin(false); showDisableConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) { Text("بله، غیرفعال کن") }
            },
            dismissButton = { TextButton(onClick = { showDisableConfirm = false }) { Text("انصراف") } }
        )
    }
}

// ─── helper composables (بدون تغییر نسبت به قبل) ───
@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(Teal600))
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun ToggleRow(icon: String, label: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
            Text(subtitle, fontSize = 10.sp, color = Slate500)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = Teal600,
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Slate300
            )
        )
    }
}

@Composable
private fun AutoLockChip(minutes: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (selected) Teal700 else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            "$minutes دقیقه", fontSize = 12.sp,
            color = if (selected) Color.White else Slate600,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}