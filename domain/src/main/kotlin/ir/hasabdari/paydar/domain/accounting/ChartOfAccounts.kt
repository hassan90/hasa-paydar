package ir.hasabdari.paydar.domain.accounting

import ir.hasabdari.paydar.domain.model.Account
import ir.hasabdari.paydar.domain.model.AccountCategory
import ir.hasabdari.paydar.domain.model.AccountNature

/**
 * نمودار حساب‌های استاندارد ایران — طبق الگوی سازمان حسابرسی.
 * ۴ سطح: گروه → کل → معین → تفصیلی
 * فقط سطح ۴ (کد ۶ رقمی) قابل ثبت است.
 */
object ChartOfAccounts {

    // ═══ کدهای کلیدی که در کدهای جاوا استفاده می‌شوند ═══
    const val CASHBOX = "110101"
    const val BANKS = "110102"
    const val ACCOUNTS_RECEIVABLE_CUSTOMERS = "110201"
    const val CHEQUES_RECEIVABLE = "110202"
    const val INVENTORY = "110301"
    const val PREPAID_SUPPLIERS = "110401"
    const val ACCOUNTS_PAYABLE_SUPPLIERS = "210101"
    const val CHEQUES_PAYABLE = "210102"
    const val VAT_PAYABLE = "210201"
    const val SALARIES_PAYABLE = "210301"
    const val CAPITAL = "3101"
    const val DRAWINGS = "3201"
    const val RETAINED_EARNINGS = "3301"
    const val SALES_GOODS = "4101"
    const val SALES_SERVICES = "4102"
    const val SALES_RETURN = "4201"
    const val MISC_REVENUE = "4301"
    const val COGS = "5101"
    const val INVENTORY_SHRINKAGE = "5201"
    const val SHIPPING_EXPENSE = "6101"
    const val ADVERTISING_EXPENSE = "6102"
    const val RENT_EXPENSE = "6201"
    const val SALARIES_EXPENSE = "6202"
    const val UTILITIES_EXPENSE = "6203"
    const val MISC_ADMIN_EXPENSE = "6204"
    const val BANK_FEES = "6301"

    val seedAccounts: List<Account> = listOf(
        // ═══ سطح ۱: گروه‌ها ═══
        Account("1", "دارایی‌ها", null, 1, AccountNature.DEBIT, AccountCategory.ASSET, false, true),
        Account("2", "بدهی‌ها", null, 1, AccountNature.CREDIT, AccountCategory.LIABILITY, false, true),
        Account("3", "حقوق مالکانه", null, 1, AccountNature.CREDIT, AccountCategory.EQUITY, false, true),
        Account("4", "درآمدها", null, 1, AccountNature.CREDIT, AccountCategory.REVENUE, false, true),
        Account("5", "بهای تمام‌شده", null, 1, AccountNature.DEBIT, AccountCategory.COGS, false, true),
        Account("6", "هزینه‌ها", null, 1, AccountNature.DEBIT, AccountCategory.EXPENSE, false, true),

        // ═══ سطح ۲: کل ═══
        Account("11", "دارایی جاری", "1", 2, AccountNature.DEBIT, AccountCategory.ASSET, false, true),
        Account("12", "دارایی غیرجاری", "1", 2, AccountNature.DEBIT, AccountCategory.ASSET, false, true),
        Account("21", "بدهی جاری", "2", 2, AccountNature.CREDIT, AccountCategory.LIABILITY, false, true),
        Account("31", "سرمایه", "3", 2, AccountNature.CREDIT, AccountCategory.EQUITY, false, true),
        Account("32", "برداشت", "3", 2, AccountNature.DEBIT, AccountCategory.EQUITY, false, true),
        Account("33", "سود و زیان انباشته", "3", 2, AccountNature.CREDIT, AccountCategory.EQUITY, false, true),
        Account("41", "فروش", "4", 2, AccountNature.CREDIT, AccountCategory.REVENUE, false, true),
        Account("42", "برگشت از فروش", "4", 2, AccountNature.DEBIT, AccountCategory.REVENUE, false, true),
        Account("43", "درآمد متفرقه", "4", 2, AccountNature.CREDIT, AccountCategory.REVENUE, false, true),
        Account("51", "بهای تمام‌شده کالای فروش‌رفته", "5", 2, AccountNature.DEBIT, AccountCategory.COGS, false, true),
        Account("52", "کسری و ضایعات انبار", "5", 2, AccountNature.DEBIT, AccountCategory.COGS, false, true),
        Account("61", "هزینه‌های فروش", "6", 2, AccountNature.DEBIT, AccountCategory.EXPENSE, false, true),
        Account("62", "هزینه‌های اداری", "6", 2, AccountNature.DEBIT, AccountCategory.EXPENSE, false, true),
        Account("63", "هزینه‌های مالی", "6", 2, AccountNature.DEBIT, AccountCategory.EXPENSE, false, true),

        // ═══ سطح ۳: معین ═══
        Account("1101", "موجودی نقد و بانک", "11", 3, AccountNature.DEBIT, AccountCategory.ASSET, false, true),
        Account("1102", "حساب‌های دریافتنی", "11", 3, AccountNature.DEBIT, AccountCategory.ASSET, false, true),
        Account("1103", "موجودی کالا", "11", 3, AccountNature.DEBIT, AccountCategory.ASSET, false, true),
        Account("1104", "پیش‌پرداخت‌ها", "11", 3, AccountNature.DEBIT, AccountCategory.ASSET, false, true),
        Account("2101", "حساب‌های پرداختنی", "21", 3, AccountNature.CREDIT, AccountCategory.LIABILITY, false, true),
        Account("2102", "مالیات و عوارض", "21", 3, AccountNature.CREDIT, AccountCategory.LIABILITY, false, true),
        Account("2103", "حقوق پرداختنی", "21", 3, AccountNature.CREDIT, AccountCategory.LIABILITY, false, true),

        // ═══ سطح ۴: تفصیلی (postable) ═══
        Account("110101", "صندوق", "1101", 4, AccountNature.DEBIT, AccountCategory.ASSET, true, true),
        Account("110102", "بانک‌ها", "1101", 4, AccountNature.DEBIT, AccountCategory.ASSET, true, true),
        Account("110201", "حساب دریافتنی مشتریان", "1102", 4, AccountNature.DEBIT, AccountCategory.ASSET, true, true),
        Account("110202", "اسناد دریافتنی (چک)", "1102", 4, AccountNature.DEBIT, AccountCategory.ASSET, true, true),
        Account("110301", "موجودی انبار", "1103", 4, AccountNature.DEBIT, AccountCategory.ASSET, true, true),
        Account("110401", "پیش‌پرداخت به تأمین‌کنندگان", "1104", 4, AccountNature.DEBIT, AccountCategory.ASSET, true, true),
        Account("210101", "حساب پرداختنی تأمین‌کنندگان", "2101", 4, AccountNature.CREDIT, AccountCategory.LIABILITY, true, true),
        Account("210102", "اسناد پرداختنی (چک)", "2101", 4, AccountNature.CREDIT, AccountCategory.LIABILITY, true, true),
        Account("210201", "مالیات بر ارزش افزوده", "2102", 4, AccountNature.CREDIT, AccountCategory.LIABILITY, true, true),
        Account("210301", "حقوق پرداختنی", "2103", 4, AccountNature.CREDIT, AccountCategory.LIABILITY, true, true),
        Account("3101", "سرمایه اولیه", "31", 4, AccountNature.CREDIT, AccountCategory.EQUITY, true, true),
        Account("3201", "برداشت مالک", "32", 4, AccountNature.DEBIT, AccountCategory.EQUITY, true, true),
        Account("3301", "سود و زیان انباشته", "33", 4, AccountNature.CREDIT, AccountCategory.EQUITY, true, true),
        Account("4101", "فروش کالا", "41", 4, AccountNature.CREDIT, AccountCategory.REVENUE, true, true),
        Account("4102", "فروش خدمات", "41", 4, AccountNature.CREDIT, AccountCategory.REVENUE, true, true),
        Account("4201", "برگشت از فروش کالا", "42", 4, AccountNature.DEBIT, AccountCategory.REVENUE, true, true),
        Account("4301", "درآمد متفرقه", "43", 4, AccountNature.CREDIT, AccountCategory.REVENUE, true, true),
        Account("5101", "بهای تمام‌شده کالای فروش‌رفته", "51", 4, AccountNature.DEBIT, AccountCategory.COGS, true, true),
        Account("5201", "کسری و ضایعات انبار", "52", 4, AccountNature.DEBIT, AccountCategory.COGS, true, true),
        Account("6101", "هزینه حمل و نقل", "61", 4, AccountNature.DEBIT, AccountCategory.EXPENSE, true, true),
        Account("6102", "هزینه تبلیغات", "61", 4, AccountNature.DEBIT, AccountCategory.EXPENSE, true, true),
        Account("6201", "هزینه اجاره", "62", 4, AccountNature.DEBIT, AccountCategory.EXPENSE, true, true),
        Account("6202", "هزینه حقوق و دستمزد", "62", 4, AccountNature.DEBIT, AccountCategory.EXPENSE, true, true),
        Account("6203", "هزینه آب، برق و گاز", "62", 4, AccountNature.DEBIT, AccountCategory.EXPENSE, true, true),
        Account("6204", "سایر هزینه‌های اداری", "62", 4, AccountNature.DEBIT, AccountCategory.EXPENSE, true, true),
        Account("6301", "کارمزد بانکی", "63", 4, AccountNature.DEBIT, AccountCategory.EXPENSE, true, true)
    )

    fun getAccount(code: String): Account? = seedAccounts.find { it.code == code }
    fun getPostableAccounts(): List<Account> = seedAccounts.filter { it.isPostable && it.isActive }
}