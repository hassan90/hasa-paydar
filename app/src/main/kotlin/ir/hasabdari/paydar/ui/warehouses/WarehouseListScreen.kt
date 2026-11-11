package ir.hasabdari.paydar.ui.warehouses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.ui.components.PDBottomNav
import ir.hasabdari.paydar.ui.components.PDEmptyState
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseListScreen(
    onBack: () -> Unit,
    onNavigateBottom: (String) -> Unit = {},
    viewModel: WarehouseViewModel = hiltViewModel()
) {
    val warehouses by viewModel.warehouses.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("انبارها", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("→", fontSize = 22.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal700,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Amber500
            ) { Icon(Icons.Default.Add, contentDescription = null, tint = Color.White) }
        },
        bottomBar = {
            PDBottomNav(
                currentRoute = "warehouses",
                onNavigate = onNavigateBottom,
                onFabClick = { showDialog = true }
            )
        }
    ) { padding ->
        if (warehouses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                PDEmptyState(
                    emoji = "🏭",
                    title = "هنوز انباری ندارید",
                    message = "اولین انبار را اضافه کنید",
                    actionText = "➕ افزودن انبار",
                    onAction = { showDialog = true }
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(Teal700, Teal600))).padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Text("🏭", fontSize = 26.sp) }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("${warehouses.size} انبار فعال", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("مدیریت انبارها", color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                            }
                        }
                    }
                }

                items(items = warehouses, key = { it.id }) { wh ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(Teal100, Teal200))),
                                contentAlignment = Alignment.Center
                            ) { Text("🏭", fontSize = 22.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(wh.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                                if (wh.isDefault) {
                                    Text("پیش‌فرض", fontSize = 10.sp, color = Teal700, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("افزودن انبار", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("نام انبار") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.add(name); showDialog = false },
                    enabled = name.isNotBlank()
                ) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("انصراف") } }
        )
    }
}