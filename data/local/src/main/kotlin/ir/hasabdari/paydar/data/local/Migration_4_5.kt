package ir.hasabdari.paydar.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ir.hasabdari.paydar.domain.accounting.ChartOfAccounts

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ═══ ۱. جدول حساب‌ها ═══
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `accounts` (
                `code` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `parent_code` TEXT,
                `level` INTEGER NOT NULL,
                `nature` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `is_postable` INTEGER NOT NULL DEFAULT 1,
                `is_active` INTEGER NOT NULL DEFAULT 1,
                PRIMARY KEY(`code`)
            )
        """.trimIndent())

        // ═══ ۲. سال مالی ═══
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `fiscal_years` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `start_date` INTEGER NOT NULL,
                `end_date` INTEGER NOT NULL,
                `is_closed` INTEGER NOT NULL DEFAULT 0,
                `store_id` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_fiscal_years_store_id_start_date` ON `fiscal_years` (`store_id`, `start_date`)")

        // ═══ ۳. سربرگ سند ═══
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `journal_entries` (
                `id` TEXT NOT NULL,
                `number` INTEGER NOT NULL,
                `fiscal_year_id` TEXT NOT NULL,
                `entry_date` INTEGER NOT NULL,
                `description` TEXT NOT NULL,
                `source_type` TEXT NOT NULL,
                `source_id` TEXT,
                `status` TEXT NOT NULL DEFAULT 'POSTED',
                `reversal_of` TEXT,
                `created_by` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `store_id` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_journal_entries_store_id_fiscal_year_id_number` ON `journal_entries` (`store_id`, `fiscal_year_id`, `number`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_source_type_source_id` ON `journal_entries` (`source_type`, `source_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entries_entry_date` ON `journal_entries` (`entry_date`)")

        // ═══ ۴. ردیف‌های سند ═══
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `journal_lines` (
                `id` TEXT NOT NULL,
                `entry_id` TEXT NOT NULL,
                `line_no` INTEGER NOT NULL,
                `account_code` TEXT NOT NULL,
                `party_id` TEXT,
                `debit` INTEGER NOT NULL DEFAULT 0,
                `credit` INTEGER NOT NULL DEFAULT 0,
                `description` TEXT,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_lines_entry_id` ON `journal_lines` (`entry_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_lines_account_code` ON `journal_lines` (`account_code`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_lines_party_id` ON `journal_lines` (`party_id`)")

        // ═══ ۵. افزودن journal_entry_id به cash_transactions ═══
        db.execSQL("ALTER TABLE `cash_transactions` ADD COLUMN `journal_entry_id` TEXT")

        // ═══ ۶. Seed نمودار حساب‌ها ═══
        ChartOfAccounts.seedAccounts.forEach { acc ->
            val parent = acc.parentCode?.let { "'$it'" } ?: "NULL"
            val postable = if (acc.isPostable) 1 else 0
            val active = if (acc.isActive) 1 else 0
            db.execSQL(
                "INSERT OR IGNORE INTO `accounts` " +
                        "(`code`, `name`, `parent_code`, `level`, `nature`, `category`, `is_postable`, `is_active`) " +
                        "VALUES ('${acc.code}', '${acc.name.replace("'", "''")}', $parent, ${acc.level}, " +
                        "'${acc.nature.name}', '${acc.category.name}', $postable, $active)"
            )
        }
    }
}