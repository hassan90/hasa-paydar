package ir.hasabdari.paydar.ui.parties

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.ui.components.*
import ir.hasabdari.paydar.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartyListScreen(
    initialKind: PartyKind = PartyKind.CUSTOMER,
    onBack: () -> Unit,
    onAddClick: (PartyKind) -> Unit,
    onPartyClick: (String) -> Unit,
    onNavigateBottom: (String) -> Unit = {},
    viewModel: PartyListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        containerColor = Slate50,
        topBar = {
            TopAppBar(
                title = { Text("اشخاص", fontWeight = FontWeight.Bold) },
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
                onClick = { onAddClick(state.kind) },
                containerColor = Amber500,
                contentColor = Color.White,
                shape = CircleShape
            ) { Icon(Icons.Default.Add, contentDescription = "افزودن", tint = Color.White) }
        },
        bottomBar = {
            PDBottomNav(
                currentRoute = "more",
                onNavigate = onNavigateBottom,
                onFabClick = { onAddClick(state.kind) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // ═══ Tabs مشتری / تأمین‌کننده ═══
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PDChip(
                    "👥 مشتریان",
                    state.kind == PartyKind.CUSTOMER,
                    onClick = { viewModel.onKindChange(PartyKind.CUSTOMER) }
                )
                PDChip(
                    "🏭 تأمین‌کنندگان",
                    state.kind == PartyKind.SUPPLIER,
                    onClick = { viewModel.onKindChange(PartyKind.SUPPLIER) }
                )
            }

            // ═══ Search ═══
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("🔍 جستجو نام، موبایل...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Teal700,
                    unfocusedBorderColor = Slate200,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Spacer(Modifier.height(8.dp))

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Teal700)
                }
                state.parties.isEmpty() -> PDEmptyState(
                    emoji = if (state.kind == PartyKind.CUSTOMER) "👥" else "🏭",
                    title = if (state.query.isBlank())
                        if (state.kind == PartyKind.CUSTOMER) "هنوز مشتری ندارید" else "هنوز تأمین‌کننده ندارید"
                    else "نتیجه‌ای یافت نشد",
                    message = if (state.query.isBlank()) "با افزودن اولین شخص شروع کنید" else "عبارت دیگری را امتحان کنید",
                    actionText = if (state.query.isBlank())
                        if (state.kind == PartyKind.CUSTOMER) "➕ افزودن مشتری" else "➕ افزودن تأمین‌کننده"
                    else null,
                    onAction = if (state.query.isBlank()) ({ onAddClick(state.kind) }) else null
                )
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                "${state.parties.size} ${if (state.kind == PartyKind.CUSTOMER) "مشتری" else "تأمین‌کننده"}",
                                fontSize = 12.sp, color = Slate500, fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(state.parties, key = { it.id }) { party ->
                            PartyRow(party, isCustomer = state.kind == PartyKind.CUSTOMER) {
                                onPartyClick(party.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyRow(party: Party, isCustomer: Boolean, onClick: () -> Unit) {
    val accent = if (isCustomer) Rose500 else Violet500
    val avatar1 = if (isCustomer) Rose400 else Violet400
    val avatar2 = if (isCustomer) Rose600 else Violet600

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // نوار رنگی سمت چپ بصری
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(avatar1, avatar2)))
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(avatar1, avatar2))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                party.name.firstOrNull()?.toString() ?: "؟",
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(party.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
            Text(
                party.mobile?.takeIf { it.isNotBlank() } ?: party.phone ?: "بدون شماره",
                fontSize = 10.sp, color = Slate500, modifier = Modifier.padding(top = 2.dp)
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("۰", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Slate900)
            Spacer(Modifier.height(4.dp))
            PDBadge("تسویه", PDBadgeType.NEUTRAL)
        }
    }
}