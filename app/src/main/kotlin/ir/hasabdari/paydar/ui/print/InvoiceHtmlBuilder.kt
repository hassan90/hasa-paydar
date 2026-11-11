package ir.hasabdari.paydar.ui.print

import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.domain.model.InvoiceWithLines
import ir.hasabdari.paydar.domain.model.Party
import ir.hasabdari.paydar.domain.model.SettlementType
import ir.hasabdari.paydar.domain.model.StoreInfo

/**
 * ساخت HTML حرفه‌ای برای فاکتور — با طراحی مدرن، RTL، فونت Vazirmatn.
 * خروجی به WebView داده می‌شود و از Android Print Framework چاپ می‌شود.
 */
object InvoiceHtmlBuilder {

    fun build(
        invoice: InvoiceWithLines,
        party: Party?,
        productNames: Map<String, String>,
        store: StoreInfo
    ): String {
        val inv = invoice.invoice
        val isPurchase = inv.type.name.startsWith("PURCHASE")
        val isReturn = inv.type.name.endsWith("_RETURN")

        val title = when {
            isReturn && isPurchase -> "برگشت به خرید"
            isReturn -> "برگشت از فروش"
            isPurchase -> "فاکتور خرید"
            else -> "فاکتور فروش"
        }

        val partyLabel = if (isPurchase) "تأمین‌کننده" else "مشتری"

        val itemsHtml = invoice.lines.joinToString("") { line ->
            val name = productNames[line.productId] ?: "کالا ${line.productId.take(6)}"
            val qty = line.quantityScaled / 1000
            val unitPrice = line.unitPrice.rials / 10
            val lineTotal = line.lineTotal.rials / 10
            """
            <tr>
                <td class="col-no">${fa(line.lineNo.toString())}</td>
                <td class="col-name">${esc(name)}</td>
                <td class="col-qty">${fa(qty.toString())}</td>
                <td class="col-price">${fmt(unitPrice)}</td>
                <td class="col-total">${fmt(lineTotal)}</td>
            </tr>
            """.trimIndent()
        }

        val subtotal = inv.subtotal.rials / 10
        val discount = inv.discountTotal.rials / 10
        val tax = inv.taxTotal.rials / 10
        val shipping = inv.shippingCost.rials / 10
        val otherCosts = inv.otherCosts.rials / 10
        val grandTotal = inv.grandTotal.rials / 10
        val paid = inv.paidAmount.rials / 10
        val remaining = inv.remaining.rials / 10

        val settlementLabel = when (inv.settlementType) {
            SettlementType.CASH -> "نقدی"
            SettlementType.CREDIT -> "نسیه"
            SettlementType.MIXED -> "ترکیبی"
        }

        val paymentBadge = when {
            remaining <= 0 -> """<span class="badge badge-paid">✓ تسویه شده</span>"""
            paid > 0 -> """<span class="badge badge-partial">پرداخت جزئی</span>"""
            else -> """<span class="badge badge-unpaid">پرداخت‌نشده</span>"""
        }

        val discountRow = if (discount > 0) """
            <div class="total-row">
                <span>تخفیف</span>
                <span class="amount">− ${fmt(discount)}</span>
            </div>
        """ else ""

        val taxRow = if (tax > 0) """
            <div class="total-row">
                <span>مالیات بر ارزش افزوده</span>
                <span class="amount">+ ${fmt(tax)}</span>
            </div>
        """ else ""

        val shippingRow = if (shipping > 0) """
            <div class="total-row">
                <span>هزینه حمل</span>
                <span class="amount">+ ${fmt(shipping)}</span>
            </div>
        """ else ""

        val otherRow = if (otherCosts > 0) """
            <div class="total-row">
                <span>سایر هزینه‌ها</span>
                <span class="amount">+ ${fmt(otherCosts)}</span>
            </div>
        """ else ""

        val remainingRow = if (remaining > 0) """
            <div class="pay-amount warn">
                <span>مانده:</span>
                <strong>${fmt(remaining)} تومان</strong>
            </div>
        """ else ""

        val logoText = store.name.firstOrNull()?.toString() ?: "🏪"

        return """<!DOCTYPE html>
<html dir="rtl" lang="fa">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>$title ${esc(inv.number)}</title>
<style>
@import url('https://cdn.jsdelivr.net/gh/rastikerdar/vazirmatn@v33.003/Vazirmatn-font-face.css');
* { box-sizing: border-box; margin: 0; padding: 0; }
html, body { direction: rtl; }
body {
    font-family: 'Vazirmatn', Tahoma, sans-serif;
    color: #0F172A;
    background: #EEF2F7;
    padding: 24px;
    font-size: 12px;
    line-height: 1.7;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
}
.invoice {
    max-width: 780px;
    margin: 0 auto;
    background: white;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 4px 24px rgba(15,23,42,0.10);
}
.header {
    background: linear-gradient(135deg, #0F766E 0%, #0D9488 60%, #14B8A6 100%);
    color: white;
    padding: 22px 28px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.brand {
    display: flex;
    align-items: center;
    gap: 14px;
}
.logo {
    width: 56px;
    height: 56px;
    background: rgba(255,255,255,0.25);
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    font-weight: 800;
    backdrop-filter: blur(8px);
}
.brand h1 { font-size: 19px; font-weight: 800; letter-spacing: -0.3px; }
.brand .tagline { font-size: 10px; opacity: 0.85; margin-top: 2px; }
.header-meta { text-align: left; font-size: 11px; line-height: 1.9; opacity: 0.95; }
.header-meta strong { font-weight: 700; }

.info-strip {
    display: grid;
    grid-template-columns: 1fr 1fr;
    border-bottom: 1px solid #E2E8F0;
    background: #F8FAFC;
}
.info-cell { padding: 16px 24px; }
.info-cell:first-child { border-left: 1px solid #E2E8F0; }
.info-cell .label {
    font-size: 10px;
    color: #64748B;
    font-weight: 600;
    margin-bottom: 4px;
}
.info-cell .value {
    font-size: 13px;
    font-weight: 700;
    color: #0F172A;
}
.info-cell .sub {
    font-size: 10px;
    color: #64748B;
    margin-top: 3px;
}

.badge {
    display: inline-block;
    padding: 3px 10px;
    border-radius: 6px;
    font-size: 10px;
    font-weight: 700;
    margin-top: 4px;
}
.badge-paid { background: #DCFCE7; color: #16A34A; }
.badge-partial { background: #FEF3C7; color: #D97706; }
.badge-unpaid { background: #FFE4E6; color: #E11D48; }

table { width: 100%; border-collapse: collapse; }
thead th {
    background: #F1F5F9;
    padding: 12px 14px;
    text-align: right;
    font-size: 11px;
    font-weight: 700;
    color: #475569;
    border-bottom: 1px solid #E2E8F0;
}
tbody td {
    padding: 12px 14px;
    border-bottom: 1px solid #F1F5F9;
    font-size: 12px;
    color: #0F172A;
}
tbody tr:last-child td { border-bottom: none; }
.col-no { width: 36px; text-align: center; color: #64748B; }
.col-name { text-align: right; }
.col-qty { width: 70px; text-align: center; font-weight: 600; }
.col-price { width: 110px; text-align: left; font-variant-numeric: tabular-nums; }
.col-total { width: 110px; text-align: left; font-variant-numeric: tabular-nums; font-weight: 700; color: #0F766E; }

.totals {
    padding: 16px 24px 8px;
    background: #F8FAFC;
    border-top: 2px solid #E2E8F0;
}
.total-row {
    display: flex;
    justify-content: space-between;
    padding: 5px 0;
    font-size: 12px;
    color: #475569;
}
.total-row .amount { font-variant-numeric: tabular-nums; color: #0F172A; font-weight: 600; }
.total-row.grand {
    padding: 12px 0 4px;
    border-top: 1.5px dashed #CBD5E1;
    margin-top: 8px;
    font-size: 15px;
    font-weight: 800;
    color: #0F766E;
}
.total-row.grand .amount { color: #0F766E; font-size: 16px; }

.pay-strip {
    padding: 14px 24px 18px;
    background: #F8FAFC;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #E2E8F0;
}
.pay-amount { font-size: 11px; color: #475569; }
.pay-amount strong { font-size: 13px; color: #16A34A; font-weight: 800; }
.pay-amount.warn strong { color: #E11D48; }

.footer {
    padding: 18px 24px;
    text-align: center;
    background: white;
    font-size: 10px;
    color: #64748B;
    line-height: 1.9;
}
.footer .thanks {
    font-size: 14px;
    font-weight: 800;
    color: #0F766E;
    margin-bottom: 6px;
}
.footer .credit { font-size: 9px; color: #94A3B8; margin-top: 6px; }

@media print {
    body { padding: 0; background: white; }
    .invoice { box-shadow: none; border-radius: 0; max-width: 100%; }
    @page { size: A4; margin: 12mm; }
}
</style>
</head>
<body>
<div class="invoice">
    <div class="header">
        <div class="brand">
            <div class="logo">${esc(logoText)}</div>
            <div>
                <h1>${esc(store.name.ifBlank { "فروشگاه شما" })}</h1>
                <div class="tagline">$title</div>
            </div>
        </div>
        <div class="header-meta">
            ${store.phone.takeIf { it.isNotBlank() }?.let { "<div>📞 ${esc(it)}</div>" } ?: ""}
            ${store.mobile.takeIf { it.isNotBlank() }?.let { "<div>📱 ${esc(it)}</div>" } ?: ""}
            ${store.address.takeIf { it.isNotBlank() }?.let { "<div>📍 ${esc(it)}</div>" } ?: ""}
            ${store.economicCode.takeIf { it.isNotBlank() }?.let { "<div>🔢 کد اقتصادی: ${esc(it)}</div>" } ?: ""}
        </div>
    </div>

    <div class="info-strip">
        <div class="info-cell">
            <div class="label">$partyLabel</div>
            <div class="value">${esc(party?.name ?: "متفرقه")}</div>
            ${party?.mobile?.takeIf { it.isNotBlank() }?.let { "<div class='sub'>📱 ${esc(it)}</div>" } ?: ""}
            ${party?.address?.takeIf { it.isNotBlank() }?.let { "<div class='sub'>📍 ${esc(it)}</div>" } ?: ""}
            ${party?.nationalId?.takeIf { it.isNotBlank() }?.let { "<div class='sub'>🆔 ${esc(it)}</div>" } ?: ""}
        </div>
        <div class="info-cell">
            <div class="label">شماره فاکتور</div>
            <div class="value">${esc(inv.number)}</div>
            <div class="sub">📅 ${JalaliDate.fromEpochMillis(inv.issueDate).formatPersian()}</div>
            <div class="sub">💳 $settlementLabel</div>
            $paymentBadge
        </div>
    </div>

    <table>
        <thead>
            <tr>
                <th class="col-no">#</th>
                <th class="col-name">شرح کالا / خدمات</th>
                <th class="col-qty">تعداد</th>
                <th class="col-price">قیمت واحد (ت)</th>
                <th class="col-total">جمع (ت)</th>
            </tr>
        </thead>
        <tbody>
            $itemsHtml
        </tbody>
    </table>

    <div class="totals">
        <div class="total-row">
            <span>جمع اقلام</span>
            <span class="amount">${fmt(subtotal)}</span>
        </div>
        $discountRow
        $shippingRow
        $otherRow
        $taxRow
        <div class="total-row grand">
            <span>جمع کل قابل پرداخت</span>
            <span class="amount">${fmt(grandTotal)} تومان</span>
        </div>
    </div>

    <div class="pay-strip">
        <div class="pay-amount">پرداخت‌شده: <strong>${fmt(paid)} تومان</strong></div>
        $remainingRow
    </div>

    <div class="footer">
        <div class="thanks">از اعتماد شما سپاسگزاریم</div>
        <div>این فاکتور به‌صورت خودکار توسط نرم‌افزار «حسابداری پایدار» صادر شده است.</div>
        <div class="credit">${esc(store.name)} · ${JalaliDate.today().formatLong()}</div>
    </div>
</div>
</body>
</html>""".trimIndent()
    }

    // ─── helpers ───
    private val persianDigits = mapOf(
        '0' to '۰', '1' to '۱', '2' to '۲', '3' to '۳', '4' to '۴',
        '5' to '۵', '6' to '۶', '7' to '۷', '8' to '۸', '9' to '۹'
    )

    private fun fa(s: String): String = s.map { persianDigits[it] ?: it }.joinToString("")

    private fun fmt(n: Long): String {
        val s = n.toString()
        val withSep = s.reversed().chunked(3).joinToString("٬").reversed()
        return fa(withSep)
    }

    private fun esc(s: String?): String = s
        ?.replace("&", "&amp;")
        ?.replace("<", "&lt;")
        ?.replace(">", "&gt;")
        ?.replace("\"", "&quot;")
        ?: ""
}