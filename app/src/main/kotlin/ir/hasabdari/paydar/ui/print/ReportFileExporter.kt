package ir.hasabdari.paydar.ui.print

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.domain.model.PartyDetailReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportFileExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun saveCsvToDownloads(csv: String, fileName: String): Unit =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS
                )
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IllegalStateException("امکان ایجاد فایل نیست")

            context.contentResolver.openOutputStream(uri)?.use { out ->
                // BOM برای UTF-8 تا Excel فارسی را درست باز کند
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(csv.toByteArray(Charsets.UTF_8))
            } ?: throw IllegalStateException("امکان نوشتن فایل نیست")
        }

    companion object {
        fun buildPartyCsv(report: PartyDetailReport): String {
            val sb = StringBuilder()
            // Header info
            sb.append("گزارش تفصیلی: ${report.partyName}\n")
            sb.append("از: ${JalaliDate.fromEpochMillis(report.fromDate).formatPersian()}\n")
            sb.append("تا: ${JalaliDate.fromEpochMillis(report.toDate).formatPersian()}\n")
            sb.append("مانده افتتاحیه: ${report.openingBalanceRials}\n")
            sb.append("\n")
            // Table
            sb.append("تاریخ,شرح,بدهکار,بستانکار,مانده\n")
            report.rows.forEach { line ->
                val date = JalaliDate.fromEpochMillis(line.date).formatPersian()
                val desc = "${line.documentType} - ${line.description ?: ""}".replace(",", "،")
                sb.append("$date,$desc,${line.debitRials},${line.creditRials},${line.runningBalanceRials}\n")
            }
            sb.append("\n")
            sb.append("جمع بدهکار: ${report.totalDebitRials}\n")
            sb.append("جمع بستانکار: ${report.totalCreditRials}\n")
            sb.append("مانده اختتامیه: ${report.closingBalanceRials}\n")
            return sb.toString()
        }
    }
}