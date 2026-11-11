package ir.hasabdari.paydar.data.local.backup

import android.content.Context
import android.net.Uri
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.data.local.PaydarDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val db: PaydarDatabase
) {
    suspend fun backupTo(uri: Uri): AppResult<Long> = withContext(Dispatchers.IO) {
        try {
            // checkpoint WAL برای اطمینان از ذخیره همه‌ی داده‌ها در فایل اصلی
            try {
                db.openHelper.writableDatabase
                    .query("PRAGMA wal_checkpoint(TRUNCATE)")
                    .use { it.moveToFirst() }
            } catch (_: Exception) { /* WAL ممکن است غیرفعال باشد */ }

            val dbFile = context.getDatabasePath(PaydarDatabase.DB_NAME)
            if (!dbFile.exists()) return@withContext AppResult.Failure(
                DomainError.NotFound("دیتابیس", dbFile.path)
            )

            val timestamp = System.currentTimeMillis()

            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                    // فایل دیتابیس
                    zip.putNextEntry(ZipEntry(PaydarDatabase.DB_NAME))
                    dbFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()

                    // metadata
                    val meta = """
                        backup_version=1
                        backup_date=$timestamp
                        package_name=${context.packageName}
                        db_name=${PaydarDatabase.DB_NAME}
                    """.trimIndent()
                    zip.putNextEntry(ZipEntry("metadata.txt"))
                    zip.write(meta.toByteArray())
                    zip.closeEntry()
                }
            } ?: return@withContext AppResult.Failure(DomainError.Unknown())

            AppResult.Success(timestamp)
        } catch (e: Exception) {
            AppResult.Failure(DomainError.Unknown(e))
        }
    }

    suspend fun restoreFrom(uri: Uri): AppResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(PaydarDatabase.DB_NAME)
            val tempFile = File(context.cacheDir, "restore_temp.db")
            if (tempFile.exists()) tempFile.delete()

            // استخراج دیتابیس از zip
            var foundDb = false
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(BufferedInputStream(input)).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == PaydarDatabase.DB_NAME) {
                            FileOutputStream(tempFile).use { out -> zip.copyTo(out) }
                            foundDb = true
                            break
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext AppResult.Failure(DomainError.Unknown())

            if (!foundDb || !tempFile.exists()) {
                return@withContext AppResult.Failure(
                    DomainError.InvalidInput("فایل پشتیبان", "دیتابیس در بکاپ پیدا نشد")
                )
            }

            // ⚠️ از این لحظه به بعد DB باید بی‌استفاده باشد
            try { db.close() } catch (_: Exception) { }

            // حذف فایل‌های قبلی
            listOf(dbFile, File(dbFile.parent, "${dbFile.name}-wal"), File(dbFile.parent, "${dbFile.name}-shm"))
                .forEach { if (it.exists()) it.delete() }

            // جایگزینی
            if (!tempFile.renameTo(dbFile)) {
                tempFile.copyTo(dbFile, overwrite = true)
                tempFile.delete()
            }

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(DomainError.Unknown(e))
        }
    }

    fun restartApp() {
        // استفاده از AlarmManager برای راه‌اندازی مجدد مطمئن
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            val pending = android.app.PendingIntent.getActivity(
                context, 0, intent,
                android.app.PendingIntent.FLAG_CANCEL_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val am = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            am.set(android.app.AlarmManager.RTC, System.currentTimeMillis() + 200, pending)
        } catch (_: Exception) { }
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}