package ir.hasabdari.paydar.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.ProductWithStock
import ir.hasabdari.paydar.ui.common.scaledToQuantityString
import ir.hasabdari.paydar.ui.common.toTomanString
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onNavigateBottom: (String) -> Unit = {},
    viewModel: ProductListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var gridMode by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = Slate50,
        topBar = {
            TopAppBar(
                title = { Text("کالاها", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("→", fontSize = 22.sp, color = Color.White)
                    }
                },
                actions = {
                    Text(
                        "${state.products.size} کالا",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal700,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = Amber500,
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "افزودن کالا") }
        },
        bottomBar = {
            PDBottomNav(
                currentRoute = "products",
                onNavigate = onNavigateBottom,
                onFabClick = onAddClick
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = { Text("🔍 جستجو نام یا کد کالا...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal700,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GridListToggle(active = gridMode, text = "⊞") { gridMode = true }
                    GridListToggle(active = !gridMode, text = "☰") { gridMode = false }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal700)
                }
                state.products.isEmpty() -> PDEmptyState(
                    emoji = "📦",
                    title = if (state.query.isBlank()) "هنوز کالایی ندارید" else "کالایی یافت نشد",
                    message = if (state.query.isBlank()) "اولین کالا را اضافه کنید" else "عبارت دیگری را جستجو کنید",
                    actionText = if (state.query.isBlank()) "➕ افزودن کالا" else null,
                    onAction = if (state.query.isBlank()) onAddClick else null
                )
                else -> {
                    if (gridMode) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            gridItems(state.products, key = { it.product.id }) { item ->
                                ProductGridItem(item, onClick = { onProductClick(item.product.id) })
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.products, key = { it.product.id }) { item ->
                                PDProductListRow(
                                    emoji = "📦",
                                    name = item.product.name,
                                    code = item.product.code,
                                    priceText = "${item.product.salePrice.rials.toTomanString()} ت",
                                    stockQuantityText = item.totalStockScaled.scaledToQuantityString(),
                                    stockStatus = stockStatusOf(item),
                                    onClick = { onProductClick(item.product.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductGridItem(item: ProductWithStock, onClick: () -> Unit) {
    val minS = item.product.minStockScaled.coerceAtLeast(1L)
    val maxS = (minS * 4).coerceAtLeast(item.totalStockScaled)
    val fraction = (item.totalStockScaled.toFloat() / maxS.toFloat()).coerceIn(0f, 1f)
    PDProductGridCard(
        emoji = "📦",
        name = item.product.name,
        priceText = "${item.product.salePrice.rials.toTomanString()} ت",
        stockQuantityText = item.totalStockScaled.scaledToQuantityString(),
        stockStatus = stockStatusOf(item),
        stockFraction = fraction,
        onClick = onClick
    )
}

private fun stockStatusOf(item: ProductWithStock): PDStockStatus = when {
    item.totalStockScaled <= 0 -> PDStockStatus.OUT
    item.isLowStock -> PDStockStatus.LOW
    else -> PDStockStatus.NORMAL
}

@Composable
private fun GridListToggle(active: Boolean, text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) Teal700 else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (active) Color.White else Slate400,
            fontSize = 15.sp
        )
    }
}