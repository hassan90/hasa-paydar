package ir.hasabdari.paydar.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.hasabdari.paydar.domain.model.CashTransactionType
import ir.hasabdari.paydar.domain.model.ChequeKind
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.ui.accounting.AddManualJournalScreen
import ir.hasabdari.paydar.ui.accounting.JournalDetailScreen
import ir.hasabdari.paydar.ui.accounting.JournalListScreen
import ir.hasabdari.paydar.ui.accounting.TrialBalanceScreen
import ir.hasabdari.paydar.ui.groups.ProductGroupScreen
import ir.hasabdari.paydar.ui.home.HomeScreen
import ir.hasabdari.paydar.ui.more.MoreScreen
import ir.hasabdari.paydar.ui.parties.AddPartyScreen
import ir.hasabdari.paydar.ui.parties.PartyListScreen
import ir.hasabdari.paydar.ui.products.AddProductScreen
import ir.hasabdari.paydar.ui.products.ProductDetailScreen
import ir.hasabdari.paydar.ui.products.ProductListScreen
import ir.hasabdari.paydar.ui.purchases.AddPurchaseScreen
import ir.hasabdari.paydar.ui.purchases.PurchaseDetailScreen
import ir.hasabdari.paydar.ui.purchases.PurchaseListScreen
import ir.hasabdari.paydar.ui.reports.ReportsScreen
import ir.hasabdari.paydar.ui.sales.PosScreen
import ir.hasabdari.paydar.ui.sales.SaleDetailScreen
import ir.hasabdari.paydar.ui.sales.SaleListScreen
import ir.hasabdari.paydar.ui.treasury.AddCashAccountScreen
import ir.hasabdari.paydar.ui.treasury.AddTransactionScreen
import ir.hasabdari.paydar.ui.treasury.CashLedgerScreen
import ir.hasabdari.paydar.ui.treasury.TreasuryScreen
import ir.hasabdari.paydar.ui.warehouses.WarehouseListScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object ProductList : Screen("products")
    data object AddProduct : Screen("products/add")
    data object ProductDetail : Screen("products/{productId}") {
        fun create(id: String) = "products/$id"
    }
    data object WarehouseList : Screen("warehouses")
    data object More : Screen("more")

    data object PartyList : Screen("parties?kind={kind}") {
        fun create(kind: PartyKind) = "parties?kind=${kind.name}"
    }
    data object AddParty : Screen("parties/add?kind={kind}") {
        fun create(kind: PartyKind) = "parties/add?kind=${kind.name}"
    }
    data object ProductGroup : Screen("groups")

    data object Treasury : Screen("treasury")
    data object AddCashAccount : Screen("treasury/add-account")
    data object AddTransaction : Screen("treasury/add-txn?type={type}") {
        fun create(type: CashTransactionType) = "treasury/add-txn?type=${type.name}"
    }
    data object CashLedger : Screen("treasury/{accountId}") {
        fun create(id: String) = "treasury/$id"
    }

    data object JournalList : Screen("accounting/journal")
    data object JournalDetail : Screen("accounting/journal/{entryId}") {
        fun create(id: String) = "accounting/journal/$id"
    }
    data object AddManualJournal : Screen("accounting/journal/add")
    data object TrialBalance : Screen("accounting/trial-balance")

    data object PurchaseList : Screen("purchases")
    data object AddPurchase : Screen("purchases/add")
    data object PurchaseDetail : Screen("purchases/{invoiceId}") {
        fun create(id: String) = "purchases/$id"
    }

    data object SaleList : Screen("sales")
    data object Pos : Screen("sales/pos")
    data object SaleDetail : Screen("sales/{invoiceId}") {
        fun create(id: String) = "sales/$id"
    }

    data object Reports : Screen("reports")

    data object AddReturn : Screen("return/{originalInvoiceId}") {
        fun create(invoiceId: String) = "return/$invoiceId"
    }
    data object ChequeList : Screen("cheques")
    data object AddCheque : Screen("cheques/add?kind={kind}") {
        fun create(kind: ChequeKind) = "cheques/add?kind=${kind.name}"
    }
    data object ChequeDetail : Screen("cheques/{chequeId}") {
        fun create(id: String) = "cheques/$id"
    }
    data object ChequeCalendar : Screen("cheques/calendar")

    data object Settings : Screen("settings")
    data object SecuritySettings : Screen("security")
    data object ChangePin : Screen("security/change-pin")
    data object SetupPinFromSettings : Screen("security/setup-pin")
    data object PartyDetailReport : Screen("reports/party/{partyId}") {
        fun create(partyId: String) = "reports/party/$partyId"
    }
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    val navigateBottom: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProducts = { navController.navigate(Screen.ProductList.route) },
                onNavigateToWarehouses = { navController.navigate(Screen.WarehouseList.route) },
                onNavigateToAddProduct = { navController.navigate(Screen.AddProduct.route) },
                onNavigateToCustomers = { navController.navigate(Screen.PartyList.create(PartyKind.CUSTOMER)) },
                onNavigateToSuppliers = { navController.navigate(Screen.PartyList.create(PartyKind.SUPPLIER)) },
                onNavigateToTreasury = { navController.navigate(Screen.Treasury.route) },
                onNavigateToAddReceipt = { navController.navigate(Screen.AddTransaction.create(CashTransactionType.RECEIPT)) },
                onNavigateToAddPayment = { navController.navigate(Screen.AddTransaction.create(CashTransactionType.PAYMENT)) },
                onNavigateToSales = { navController.navigate(Screen.SaleList.route) },
                onNavigateToPos = { navController.navigate(Screen.Pos.route) },
                onNavigateBottom = navigateBottom
            )
        }

        composable(Screen.ProductList.route) {
            ProductListScreen(
                onBack = { navController.popBackStack() },
                onAddClick = { navController.navigate(Screen.AddProduct.route) },
                onProductClick = { id -> navController.navigate(Screen.ProductDetail.create(id)) },
                onNavigateBottom = navigateBottom
            )
        }
        composable(Screen.AddProduct.route) {
            AddProductScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }
        composable(Screen.ProductDetail.route, arguments = listOf(navArgument("productId") { type = NavType.StringType })) {
            ProductDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.WarehouseList.route) {
            WarehouseListScreen(onBack = { navController.popBackStack() }, onNavigateBottom = navigateBottom)
        }

        composable(Screen.More.route) {
            MoreScreen(
                onNavigateToCustomers = { navController.navigate(Screen.PartyList.create(PartyKind.CUSTOMER)) },
                onNavigateToSuppliers = { navController.navigate(Screen.PartyList.create(PartyKind.SUPPLIER)) },
                onNavigateToGroups = { navController.navigate(Screen.ProductGroup.route) },
                onNavigateToTreasury = { navController.navigate(Screen.Treasury.route) },
                onNavigateToJournal = { navController.navigate(Screen.JournalList.route) },
                onNavigateToTrialBalance = { navController.navigate(Screen.TrialBalance.route) },
                onNavigateToPurchases = { navController.navigate(Screen.PurchaseList.route) },
                onNavigateBottom = navigateBottom,
                onNavigateToSales = { navController.navigate(Screen.SaleList.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onNavigateToCheques = { navController.navigate(Screen.ChequeList.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPos = { navController.navigate(Screen.Pos.route) }

            )
        }

        composable(Screen.PartyList.route, arguments = listOf(navArgument("kind") { type = NavType.StringType; defaultValue = "CUSTOMER" })) { be ->
            val kind = runCatching { PartyKind.valueOf(be.arguments?.getString("kind") ?: "CUSTOMER") }.getOrDefault(PartyKind.CUSTOMER)
            PartyListScreen(
                initialKind = kind,
                onBack = { navController.popBackStack() },
                onAddClick = { k -> navController.navigate(Screen.AddParty.create(k)) },
                onPartyClick = { id -> navController.navigate(Screen.PartyDetailReport.create(id)) },
                onNavigateBottom = navigateBottom
            )
        }

        composable(
            Screen.PartyDetailReport.route,
            arguments = listOf(navArgument("partyId") { type = NavType.StringType })
        ) {
            ir.hasabdari.paydar.ui.reports.PartyDetailReportScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AddParty.route, arguments = listOf(navArgument("kind") { type = NavType.StringType; defaultValue = "CUSTOMER" })) { be ->
            val kind = runCatching { PartyKind.valueOf(be.arguments?.getString("kind") ?: "CUSTOMER") }.getOrDefault(PartyKind.CUSTOMER)
            AddPartyScreen(initialKind = kind, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }
        composable(Screen.ProductGroup.route) {
            ProductGroupScreen(onBack = { navController.popBackStack() }, onNavigateBottom = navigateBottom)
        }

        composable(Screen.Treasury.route) {
            TreasuryScreen(
                onBack = { navController.popBackStack() },
                onAddAccount = { navController.navigate(Screen.AddCashAccount.route) },
                onAddReceipt = { navController.navigate(Screen.AddTransaction.create(CashTransactionType.RECEIPT)) },
                onAddPayment = { navController.navigate(Screen.AddTransaction.create(CashTransactionType.PAYMENT)) },
                onAddTransfer = { navController.navigate(Screen.AddTransaction.create(CashTransactionType.TRANSFER)) },
                onAccountClick = { id -> navController.navigate(Screen.CashLedger.create(id)) },
                onNavigateBottom = navigateBottom
            )
        }
        composable(Screen.AddCashAccount.route) {
            AddCashAccountScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }
        composable(Screen.AddTransaction.route, arguments = listOf(navArgument("type") { type = NavType.StringType; defaultValue = "RECEIPT" })) { be ->
            val type = runCatching { CashTransactionType.valueOf(be.arguments?.getString("type") ?: "RECEIPT") }.getOrDefault(CashTransactionType.RECEIPT)
            AddTransactionScreen(initialType = type, onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }
        composable(Screen.CashLedger.route, arguments = listOf(navArgument("accountId") { type = NavType.StringType })) {
            CashLedgerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.JournalList.route) {
            JournalListScreen(
                onBack = { navController.popBackStack() },
                onAddManual = { navController.navigate(Screen.AddManualJournal.route) },
                onEntryClick = { id -> navController.navigate(Screen.JournalDetail.create(id)) },
                onNavigateBottom = navigateBottom
            )
        }
        composable(Screen.AddManualJournal.route) {
            AddManualJournalScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }
        composable(Screen.JournalDetail.route, arguments = listOf(navArgument("entryId") { type = NavType.StringType })) {
            JournalDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.TrialBalance.route) {
            TrialBalanceScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ChequeList.route) {
            ir.hasabdari.paydar.ui.cheques.ChequeListScreen(
                onBack = { navController.popBackStack() },
                onAddClick = { kind -> navController.navigate(Screen.AddCheque.create(kind)) },
                onChequeClick = { id -> navController.navigate(Screen.ChequeDetail.create(id)) },
                onCalendarClick = { navController.navigate(Screen.ChequeCalendar.route) },
                onNavigateBottom = navigateBottom
            )
        }

        composable(
            Screen.AddCheque.route,
            arguments = listOf(navArgument("kind") { type = NavType.StringType; defaultValue = "RECEIVED" })
        ) { be ->
            val kind = runCatching { ir.hasabdari.paydar.domain.model.ChequeKind.valueOf(be.arguments?.getString("kind") ?: "RECEIVED") }
                .getOrDefault(ir.hasabdari.paydar.domain.model.ChequeKind.RECEIVED)
            ir.hasabdari.paydar.ui.cheques.AddChequeScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            Screen.ChequeDetail.route,
            arguments = listOf(navArgument("chequeId") { type = NavType.StringType })
        ) {
            ir.hasabdari.paydar.ui.cheques.ChequeDetailScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChequeCalendar.route) {
            ir.hasabdari.paydar.ui.cheques.ChequeCalendarScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.PurchaseList.route) {
            PurchaseListScreen(
                onBack = { navController.popBackStack() },
                onAddClick = { navController.navigate(Screen.AddPurchase.route) },
                onInvoiceClick = { id -> navController.navigate(Screen.PurchaseDetail.create(id)) },
                onNavigateBottom = navigateBottom
            )
        }
        composable(Screen.AddPurchase.route) {
            AddPurchaseScreen(onBack = { navController.popBackStack() }, onSaved = { navController.popBackStack() })
        }
        composable(Screen.PurchaseDetail.route, arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })) {
            ir.hasabdari.paydar.ui.purchases.PurchaseDetailScreen(
                onBack = { navController.popBackStack() },
                onReturn = { id -> navController.navigate(Screen.AddReturn.create(id)) }
            )
        }

        composable(Screen.Pos.route) {
            PosScreen(
                onBack = { navController.popBackStack() },
                onNavigateBottom = navigateBottom
            )
        }
        composable(Screen.SaleList.route) {
            SaleListScreen(
                onBack = { navController.popBackStack() },
                onNewSale = { navController.navigate(Screen.Pos.route) },
                onInvoiceClick = { id -> navController.navigate(Screen.SaleDetail.create(id)) },
                onNavigateBottom = navigateBottom
            )
        }
        composable(Screen.SaleDetail.route, arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })) {
            ir.hasabdari.paydar.ui.sales.SaleDetailScreen(
                onBack = { navController.popBackStack() },
                onReturn = { id -> navController.navigate(Screen.AddReturn.create(id)) }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                onBack = { navController.popBackStack() },
                onNavigateBottom = navigateBottom
            )
        }
        composable(
            Screen.AddReturn.route,
            arguments = listOf(navArgument("originalInvoiceId") { type = NavType.StringType })
        ) {
            ir.hasabdari.paydar.ui.returns.AddReturnScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            ir.hasabdari.paydar.ui.settings.SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToSecurity = { navController.navigate(Screen.SecuritySettings.route) },
                onNavigateBottom = navigateBottom
            )
        }

        composable(Screen.SecuritySettings.route) {
            ir.hasabdari.paydar.ui.settings.SecuritySettingsScreen(
                onBack = { navController.popBackStack() },
                onSetupPin = { navController.navigate(Screen.SetupPinFromSettings.route) },
                onChangePin = { navController.navigate(Screen.ChangePin.route) }
            )
        }

        composable(Screen.ChangePin.route) {
            ir.hasabdari.paydar.ui.auth.ChangePinScreen(
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }

        composable(Screen.SetupPinFromSettings.route) {
            // از AuthViewModel همان‌طور که هست استفاده می‌شود
            val setupVm: ir.hasabdari.paydar.ui.auth.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            ir.hasabdari.paydar.ui.auth.PinSetupScreen(
                viewModel = setupVm,
                isInitial = false,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}