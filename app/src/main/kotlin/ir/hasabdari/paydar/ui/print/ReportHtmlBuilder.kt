package ir.hasabdari.paydar.ui.print

import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.domain.model.PartyDetailReport
import ir.hasabdari.paydar.domain.model.PartyKind
import ir.hasabdari.paydar.domain.model.StoreInfo

object ReportHtmlBuilder {

    fun buildPartyReport(report: PartyDetailReport, store: StoreInfo): String {
        val rowsHtml = report.rows.joinToString("") { line ->
            val balanceText = if (line.runningBalanceRials >= 0) {
                "${fmt(line.runningBalanceRials)} بد"
            } else {
                "${fmt(-line.runningBalanceRials)} بس"
            }
            val balanceClass = if (line.runningBalanceRials >= 0) "debit" else "credit"
            """
            <tr>
                <td class="col-date">${JalaliDate.fromEpochMillis(line.date).formatPersian()}</td>
                <td class="col-desc">
                    <strong>${esc(line.documentType)}</strong>
                    ${line.description?.takeIf { it.isNotBlank() }?.let { "<br><span class='sub'>${esc(it)}</span>" } ?: ""}
                </td>
                <td class="col-amount">${if (line.debitRials > 0) fmt(line.debitRials) else "—"}</td>
                <td class="col-amount">${if (line.creditRials > 0) fmt(line.creditRials) else "—"}</td>
                <td class="col-amount $balanceClass">$balanceText</td>
            </tr>
            """.trimIndent()
        }

        val kindLabel = when (report.partyKind) {
            PartyKind.CUSTOMER -> "مشتری"
            PartyKind.SUPPLIER -> "تأمین‌کننده"
            PartyKind.BOTH -> "مشتری / تأمین‌کننده"
        }

        val openingText = if (report.openingBalanceRials >= 0) {
            "${fmt(report.openingBalanceRials)} بدهکار"
        } else {
            "${fmt(-report.openingBalanceRials)} بستانکار"
        }

        val closingText = if (report.closingBalanceRials >= 0) {
            "${fmt(report.closingBalanceRials)} بدهکار"
        } else {
            "${fmt(-report.closingBalanceRials)} بستانکار"
        }

        val closingClass = if (report.closingBalanceRials >= 0) "debit" else "credit"

        return """<!DOCTYPE html>
<html dir="rtl" lang="fa">
<head>
<meta charset="UTF-8">
<title>گزارش تفصیلی ${esc(report.partyName)}</title>
<style>
@import url('https://cdn.jsdelivr.net/gh/rastikerdar/vazirmatn@v33.003/Vazirmatn-font-face.css');
* { box-sizing: border-box; margin: 0; padding: 0; }
body {
    font-family: 'Vazirmatn', Tahoma, sans-serif;
    color: #0F172A;
    background: #EEF2F7;
    padding: 24px;
    font-size: 11px;
    line-height: 1.7;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
}
.report {
    max-width: 900px;
    margin: 0 auto;
    background: white;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 4px 24px rgba(15,23,42,0.10);
}
.header {
    background: linear-gradient(135deg, #0F766E 0%, #0D9488 60%, #14B8A6 100%);
    color: white;
    padding: 20px 28px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.brand { display: flex; align-items: center; gap: 12px; }
.logo {
    width: 48px; height: 48px;
    background: rgba(255,255,255,0.25);
    border-radius: 12px;
    display: flex; align-items: center; justify-content: center;
    font-size: 22px; font-weight: 800;
}
.brand h1 { font-size: 16px; font-weight: 800; }
.brand .tagline { font-size: 10px; opacity: 0.85; margin-top: 2px; }
.header-meta { text-align: left; font-size: 10px; opacity: 0.95; line-height: 1.8; }

.party-bar {
    background: #F8FAFC;
    padding: 16px 28px;
    display: flex; justify-content: space-between; align-items: center;
    border-bottom: 1px solid #E2E8F0;
}
.party-info { }
.party-info .name { font-size: 16px; font-weight: 800; color: #0F172A; }
.party-info .kind { font-size: 10px; color: #0F766E; font-weight: 700; margin-top: 2px; }
.party-info .meta { font-size: 10px; color: #64748B; margin-top: 3px; }
.range-info { text-align: left; font-size: 10px; color: #64748B; }
.range-info strong { color: #0F172A; font-weight: 700; }

.summary-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 0;
    border-bottom: 1px solid #E2E8F0;
    background: #F8FAFC;
}
.summary-cell {
    padding: 14px 16px;
    text-align: center;
    border-left: 1px solid #E2E8F0;
}
.summary-cell:first-child { border-left: none; }
.summary-cell .label { font-size: 9px; color: #64748B; margin-bottom: 4px; font-weight: 600; }
.summary-cell .value { font-size: 12px; font-weight: 800; }
.summary-cell .value.debit { color: #DC2626; }
.summary-cell .value.credit { color: #16A34A; }

table { width: 100%; border-collapse: collapse; }
thead th {
    background: #F1F5F9;
    padding: 10px 12px;
    text-align: right;
    font-size: 10px;
    font-weight: 700;
    color: #475569;
    border-bottom: 1px solid #E2E8F0;
}
tbody td {
    padding: 9px 12px;
    border-bottom: 1px solid #F1F5F9;
    font-size: 10px;
    vertical-align: middle;
}
tbody tr:nth-child(even) { background: #FAFBFC; }
.col-date { width: 90px; color: #475569; }
.col-desc { text-align: right; }
.col-desc strong { font-weight: 700; color: #0F172A; }
.col-desc .sub { font-size: 9px; color: #64748B; }
.col-amount { width: 90px; text-align: left; font-variant-numeric: tabular-nums; font-weight: 600; }
.col-amount.debit { color: #DC2626; }
.col-amount.credit { color: #16A34A; }

.total-row {
    background: #F8FAFC !important;
    font-weight: 800;
    border-top: 2px solid #E2E8F0;
    border-bottom: 2px solid #E2E8F0;
}
.total-row td { padding: 12px; font-size: 11px; color: #0F172A; }

.footer {
    padding: 14px 28px;
    background: white;
    text-align: center;
    font-size: 9px;
    color: #94A3B8;
}

@media print {
    body { padding: 0; background: white; }
    .report { box-shadow: none; border-radius: 0; max-width: 100%; }
    @page { size: A4; margin: 10mm; }
    tr { page-break-inside: avoid; }
}
</style>
</head>
<body>
<div class="report">
    <div class="header">
        <div class="brand">
            <div class="logo">${esc(store.name.firstOrNull()?.toString() ?: "🏪")}</div>
            <div>
                <h1>${esc(store.name.ifBlank { "فروشگاه شما" })}</h1>
                <div class="tagline">گزارش تفصیلی حساب</div>
            </div>
        </div>
        <div class="header-meta">
            ${store.phone.takeIf { it.isNotBlank() }?.let { "<div>📞 ${esc(it)}</div>" } ?: ""}
            ${store.address.takeIf { it.isNotBlank() }?.let { "<div>📍 ${esc(it)}</div>" } ?: ""}
        </div>
    </div>

    <div class="party-bar">
        <div class="party-info">
            <div class="name">${esc(report.partyName)}</div>
            <div class="kind">$kindLabel</div>
            ${report.mobile?.takeIf { it.isNotBlank() }?.let { "<div class='meta'>📱 ${esc(it)}</div>" } ?: ""}
            ${report.address?.takeIf { it.isNotBlank() }?.let { "<div class='meta'>📍 ${esc(it)}</div>" } ?: ""}
        </div>
        <div class="range-info">
            <div>از <strong>${JalaliDate.fromEpochMillis(report.fromDate).formatPersian()}</strong></div>
            <div>تا <strong>${JalaliDate.fromEpochMillis(report.toDate).formatPersian()}</strong></div>
        </div>
    </div>

    <div class="summary-grid">
        <div class="summary-cell">
            <div class="label">مانده افتتاحیه</div>
            <div class="value ${if (report.openingBalanceRials >= 0) "debit" else "credit"}">$openingText</div>
        </div>
        <div class="summary-cell">
            <div class="label">جمع بدهکار</div>
            <div class="value debit">${fmt(report.totalDebitRials)}</div>
        </div>
        <div class="summary-cell">
            <div class="label">جمع بستانکار</div>
            <div class="value credit">${fmt(report.totalCreditRials)}</div>
        </div>
        <div class="summary-cell">
            <div class="label">مانده اختتامیه</div>
            <div class="value $closingClass">$closingText</div>
        </div>
    </div>

    <table>
        <thead>
            <tr>
                <th class="col-date">تاریخ</th>
                <th class="col-desc">شرح</th>
                <th class="col-amount">بدهکار</th>
                <th class="col-amount">بستانکار</th>
                <th class="col-amount">مانده</th>
            </tr>
        </thead>
        <tbody>
            $rowsHtml
            <tr class="total-row">
                <td colspan="2">جمع کل دوره</td>
                <td class="col-amount debit">${fmt(report.totalDebitRials)}</td>
                <td class="col-amount credit">${fmt(report.totalCreditRials)}</td>
                <td class="col-amount $closingClass">$closingText</td>
            </tr>
        </tbody>
    </table>

    <div class="footer">
        گزارش صادر شده در ${JalaliDate.today().formatLong()} — نرم‌افزار حسابداری پایدار
    </div>
</div>
</body>
</html>""".trimIndent()
    }

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