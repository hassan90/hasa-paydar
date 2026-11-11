package ir.hasabdari.paydar.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.domain.model.AppSettings
import ir.hasabdari.paydar.domain.model.StoreInfo
import ir.hasabdari.paydar.ui.components.PDAppBar
import ir.hasabdari.paydar.ui.components.PDBottomNav
import ir.hasabdari.paydar.ui.components.PDTextField
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToSecurity: () -> Unit = {},
    onNavigateBottom: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? -> uri?.let { viewModel.performBackup(it) } }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.requestRestore(it) } }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // ریستارت خودکار بعد از بازیابی
    LaunchedEffect(uiState.shouldRestart) {
        if (uiState.shouldRestart) {
            kotlinx.coroutines.delay(600)
            viewModel.doRestart()
        }
    }

    Scaffold(
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            Box(Modifier.statusBarsPadding().background(Slate50)) {
                PDAppBar(title = "تنظیمات", onBack = onBack)
            }
        },
        bottomBar = {
            Box(Modifier.navigationBarsPadding()) {
                PDBottomNav(currentRoute = "more", onNavigate = onNavigateBottom)
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══ Store Info ═══
            item {
                StoreSection(
                    storeInfo = settings.storeInfo,
                    isEditing = uiState.isEditingStore,
                    draft = uiState.storeDraft,
                    isSaving = uiState.isSavingStore,
                    onStartEdit = viewModel::startEditStore,
                    onCancel = viewModel::cancelEditStore,
                    onDraftChange = viewModel::updateDraft,
                    onSave = viewModel::saveStore
                )
            }

            // ═══ Appearance ═══
            item {
                SectionCard("ظاهر و نمایش") {
                    ToggleRow(
                        icon = "🌙", label = "حالت تاریک",
                        subtitle = "پس‌زمینه تیره برای شب",
                        checked = settings.darkMode,
                        onChange = viewModel::toggleDarkMode
                    )
                    Divider(Modifier.padding(vertical = 6.dp))
                    ToggleRow(
                        icon = "🎯", label = "حالت حرفه‌ای",
                        subtitle = "نمایش دفتر روزنامه، تراز، حساب‌ها",
                        checked = settings.proMode,
                        onChange = viewModel::toggleProMode
                    )
                }
            }

            item {
                SectionCard("امنیت") {
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Slate50)
                            .clickable(onClick = onNavigateToSecurity)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(Rose100, Rose200))),
                            contentAlignment = Alignment.Center
                        ) { Text("🔒", fontSize = 20.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("امنیت و رمز ورود", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                            Text("رمز، بیومتریک، قفل خودکار", fontSize = 10.sp, color = Slate500)
                        }
                        Text("‹", fontSize = 16.sp, color = Slate400)
                    }
                }
            }
            // ═══ Tax ═══
            item {
                TaxSection(
                    currentBasisPoints = settings.defaultTaxRateBasisPoints,
                    onChange = viewModel::changeTaxRate
                )
            }

            // ═══ Backup ═══
            item {
                SectionCard("پشتیبان‌گیری") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("آخرین پشتیبان", fontSize = 11.sp, color = Slate500)
                            Text(
                                settings.lastBackupAt?.let {
                                    JalaliDate.fromEpochMillis(it).formatPersian()
                                } ?: "هرگز",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900
                            )
                        }
                        Text("💾", fontSize = 28.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val name = "hasabdari_backup_${System.currentTimeMillis()}.zip"
                            backupLauncher.launch(name)
                        },
                        enabled = !uiState.isBackupInProgress && !uiState.isRestoreInProgress,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal700)
                    ) {
                        if (uiState.isBackupInProgress) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("💾 پشتیبان جدید", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/zip", "*/*")) },
                        enabled = !uiState.isBackupInProgress && !uiState.isRestoreInProgress,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Rose500)
                    ) {
                        if (uiState.isRestoreInProgress) CircularProgressIndicator(Modifier.size(20.dp), color = Rose600, strokeWidth = 2.dp)
                        else Text("↩️ بازیابی از پشتیبان", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Rose600)
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "⚠️ بازیابی تمام داده‌های فعلی را پاک می‌کند. اپ پس از بازیابی بسته و باز می‌شود.",
                        fontSize = 10.sp, color = Slate500, lineHeight = 16.sp
                    )
                }
            }

            // ═══ About ═══
            item {
                SectionCard("درباره") {
                    AboutRow("نام محصول", "حسابداری پایدار")
                    AboutRow("نسخه", "1.0.0")
                    AboutRow("پایگاه داده", "نسخه ۷")
                    AboutRow("حالت", if (settings.proMode) "حرفه‌ای" else "ساده")
                }
            }
        }
    }

    // ═══ Dialog تأیید بازیابی ═══
    if (uiState.showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("بازیابی از پشتیبان", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "همه‌ی داده‌های فعلی (کالاها، فاکتورها، اسناد حسابداری، اشخاص) پاک و با داده‌های پشتیبان جایگزین می‌شوند.\n\n" +
                            "این عمل قابل بازگشت نیست. ادامه می‌دهید؟",
                    fontSize = 13.sp, lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmRestore,
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) { Text("بله، بازیابی کن") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) { Text("انصراف") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Sections
// ═══════════════════════════════════════════════════════════

@Composable
private fun StoreSection(
    storeInfo: StoreInfo,
    isEditing: Boolean,
    draft: StoreInfo?,
    isSaving: Boolean,
    onStartEdit: () -> Unit,
    onCancel: () -> Unit,
    onDraftChange: ((StoreInfo) -> StoreInfo) -> Unit,
    onSave: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Teal800, Teal600))).padding(18.dp)
    ) {
        if (!isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) { Text("🏪", fontSize = 28.sp) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        storeInfo.name.ifBlank { "فروشگاه شما" },
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                    )
                    if (storeInfo.phone.isNotBlank() || storeInfo.mobile.isNotBlank()) {
                        Text(
                            storeInfo.mobile.ifBlank { storeInfo.phone },
                            color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp
                        )
                    }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = onStartEdit)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) { Text("✏️ ویرایش", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        } else if (draft != null) {
            Text("ویرایش اطلاعات فروشگاه", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            WhiteField("نام فروشگاه *", draft.name) { v -> onDraftChange { it.copy(name = v) } }
            WhiteField("تلفن", draft.phone, KeyboardType.Phone) { v -> onDraftChange { it.copy(phone = v.filter { c -> c.isDigit() || c == '-' }) } }
            WhiteField("موبایل", draft.mobile, KeyboardType.Phone) { v -> onDraftChange { it.copy(mobile = v.filter { c -> c.isDigit() }) } }
            WhiteField("نشانی", draft.address) { v -> onDraftChange { it.copy(address = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { WhiteField("استان", draft.province) { v -> onDraftChange { it.copy(province = v) } } }
                Box(Modifier.weight(1f)) { WhiteField("شهر", draft.city) { v -> onDraftChange { it.copy(city = v) } } }
            }
            WhiteField("شناسه ملی", draft.nationalId, KeyboardType.Number) { v -> onDraftChange { it.copy(nationalId = v.filter { c -> c.isDigit() }) } }
            WhiteField("کد اقتصادی", draft.economicCode, KeyboardType.Number) { v -> onDraftChange { it.copy(economicCode = v.filter { c -> c.isDigit() }) } }

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
                ) { Text("انصراف", color = Color.White, fontSize = 12.sp) }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Teal700)
                ) {
                    if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Teal700, strokeWidth = 2.dp)
                    else Text("ذخیره", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun WhiteField(label: String, value: String, type: KeyboardType = KeyboardType.Text, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f)) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White,
            focusedBorderColor = Color.White,
            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
            focusedContainerColor = Color.White.copy(alpha = 0.1f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
        ),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = type)
    )
}

@Composable
private fun TaxSection(currentBasisPoints: Int, onChange: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    SectionCard("مالیات") {
        Row(
            Modifier.fillMaxWidth().clickable { showDialog = true },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("💰 نرخ مالیات پیش‌فرض", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                Text("روی فاکتورهای جدید اعمال می‌شود", fontSize = 10.sp, color = Slate500)
            }
            Text("${currentBasisPoints / 100}٪", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Teal700)
        }
    }

    if (showDialog) {
        TaxRateDialog(
            current = currentBasisPoints,
            onConfirm = { onChange(it); showDialog = false },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TaxRateDialog(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var selected by remember { mutableIntStateOf(current) }
    val options = listOf(0, 600, 900, 1000, 1200)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نرخ مالیات", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نرخ مالیات بر ارزش افزوده:", fontSize = 12.sp, color = Slate600)
                options.forEach { bp ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected == bp) Teal50 else Color.Transparent)
                            .clickable { selected = bp }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == bp, onClick = { selected = bp })
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (bp == 0) "معاف از مالیات" else "${bp / 100}٪",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selected) }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

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
private fun ToggleRow(
    icon: String, label: String, subtitle: String,
    checked: Boolean, onChange: (Boolean) -> Unit
) {
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
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Teal600,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Slate300
            )
        )
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 11.sp, color = Slate500)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate900)
    }
}