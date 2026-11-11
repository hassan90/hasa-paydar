package ir.hasabdari.paydar.domain.model

import ir.hasabdari.paydar.core.money.Money

enum class CashAccountKind { CASHBOX, BANK }

data class CashAccount(
    val id: String,
    val kind: CashAccountKind,
    val name: String,
    val bankName: String? = null,
    val accountNumber: String? = null,
    val openingBalance: Money = Money.ZERO,
    val isActive: Boolean = true,
    val storeId: String
) {
    init {
        require(name.isNotBlank()) { "نام حساب نمی‌تواند خالی باشد" }
        if (kind == CashAccountKind.BANK) {
            require(!bankName.isNullOrBlank()) { "برای حساب بانکی، نام بانک الزامی است" }
        }
    }

    val displayName: String get() = when (kind) {
        CashAccountKind.CASHBOX -> "💵 $name"
        CashAccountKind.BANK -> "🏦 $name" + (accountNumber?.let { " · $it" } ?: "")
    }
}

/** حساب + موجودی محاسبه‌شده */
data class CashAccountWithBalance(
    val account: CashAccount,
    val currentBalance: Money
)

enum class CashTransactionType { RECEIPT, PAYMENT, TRANSFER }

enum class CashTransactionMethod { CASH, CARD, TRANSFER, CHEQUE }

data class CashTransaction(
    val id: String,
    val type: CashTransactionType,
    val method: CashTransactionMethod,
    val cashAccountId: String,
    val toCashAccountId: String?,
    val partyId: String?,
    val amount: Money,
    val occurredAt: Long,
    val description: String?,
    val storeId: String
) {
    init {
        require(!amount.isZero) { "مبلغ نمی‌تواند صفر باشد" }
        require(!amount.isNegative) { "مبلغ نمی‌تواند منفی باشد" }
        if (type == CashTransactionType.TRANSFER) {
            require(toCashAccountId != null) { "برای انتقال، حساب مقصد الزامی است" }
            require(cashAccountId != toCashAccountId) { "حساب مبدأ و مقصد نمی‌توانند یکسان باشند" }
        }
    }
}

/** نمای کلی خزانه */
data class TreasuryOverview(
    val totalBalance: Money = Money.ZERO,
    val cashboxes: List<CashAccountWithBalance> = emptyList(),
    val banks: List<CashAccountWithBalance> = emptyList(),
    val recentTransactions: List<TreasuryTransactionRow> = emptyList()
)

data class TreasuryTransactionRow(
    val id: String,
    val type: CashTransactionType,
    val accountName: String,
    val partyName: String?,
    val amount: Money,
    val occurredAt: Long,
    val description: String?
)