package ir.hasabdari.paydar.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import ir.hasabdari.paydar.domain.model.ProductGroupNode
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductGroupScreen(
    onBack: () -> Unit,
    onNavigateBottom: (String) -> Unit = {},
    viewModel: ProductGroupViewModel = hiltViewModel()
) {
    val tree by viewModel.tree.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    var parentIdForNew by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val totalGroups = remember(tree) { countNodes(tree) }

    Scaffold(
        containerColor = Slate50,
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("گروه‌بندی کالا", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("→", fontSize = 22.sp, color = Color.White)
                    }
                },
                actions = {
                    Text("$totalGroups گروه", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(end = 16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Teal700, titleContentColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { parentIdForNew = null; showAddDialog = true },
                containerColor = Amber500
            ) { Icon(Icons.Default.Add, contentDescription = "افزودن", tint = Color.White) }
        },
        bottomBar = {
            PDBottomNav(
                currentRoute = "more", onNavigate = onNavigateBottom,
                onFabClick = { parentIdForNew = null; showAddDialog = true }
            )
        }
    ) { padding ->
        if (tree.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                PDEmptyState(
                    emoji = "📂",
                    title = "هنوز گروهی نساخته‌اید",
                    message = "با ساخت گروه، کالاها را دسته‌بندی کنید",
                    actionText = "➕ افزودن گروه",
                    onAction = { parentIdForNew = null; showAddDialog = true }
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tree, key = { it.group.id }) { node ->
                    GroupRow(
                        node = node,
                        level = 0,
                        onAddChild = { parentIdForNew = node.group.id; showAddDialog = true }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (parentIdForNew == null) "افزودن گروه اصلی" else "افزودن زیرگروه", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("نام گروه") }, singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.add(name, parentIdForNew)
                        showAddDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text("ذخیره") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun GroupRow(node: ProductGroupNode, level: Int, onAddChild: () -> Unit) {
    val isMain = level == 0
    val iconBg = if (isMain) listOf(Teal100, Teal200) else listOf(Violet100, Violet200)
    val countBg = if (isMain) Teal50 else Violet50
    val countFg = if (isMain) Teal700 else Violet700

    Column(Modifier.padding(start = (level * 24).dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(if (isMain) 36.dp else 30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(iconBg)),
                contentAlignment = Alignment.Center
            ) { Text(if (isMain) "📂" else "📁", fontSize = if (isMain) 18.sp else 14.sp) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(node.group.name, fontSize = if (isMain) 13.sp else 12.sp, fontWeight = if (isMain) FontWeight.Bold else FontWeight.SemiBold, color = Slate900)
                Text(
                    "${node.totalProductCount} کالا" + if (node.children.isNotEmpty()) " · ${node.children.size} زیرگروه" else "",
                    fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(countBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) { Text("${node.totalProductCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = countFg) }
            if (isMain) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(Amber100)
                        .clickable(onClick = onAddChild),
                    contentAlignment = Alignment.Center
                ) { Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Amber700) }
            }
        }
        node.children.forEach { child ->
            Spacer(Modifier.height(4.dp))
            GroupRow(node = child, level = level + 1, onAddChild = {})
        }
    }
}

private fun countNodes(nodes: List<ProductGroupNode>): Int =
    nodes.sumOf { 1 + countNodes(it.children) }