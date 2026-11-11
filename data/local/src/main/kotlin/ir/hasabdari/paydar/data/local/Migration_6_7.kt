package ir.hasabdari.paydar.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cheques` (
                `id` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `number` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `issue_date` INTEGER NOT NULL,
                `due_date` INTEGER NOT NULL,
                `bank_name` TEXT,
                `branch` TEXT,
                `account_holder` TEXT,
                `account_number` TEXT,
                `party_id` TEXT,
                `status` TEXT NOT NULL,
                `related_invoice_id` TEXT,
                `note` TEXT,
                `created_at` INTEGER NOT NULL,
                `store_id` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cheques_store_id_kind` ON `cheques` (`store_id`, `kind`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cheques_store_id_status` ON `cheques` (`store_id`, `status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cheques_due_date` ON `cheques` (`due_date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cheques_party_id` ON `cheques` (`party_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cheques_number` ON `cheques` (`number`)")
    }
}