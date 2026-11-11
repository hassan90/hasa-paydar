package ir.hasabdari.paydar.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `invoices` (
                `id` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `number` TEXT NOT NULL,
                `issue_date` INTEGER NOT NULL,
                `due_date` INTEGER,
                `party_id` TEXT,
                `warehouse_id` TEXT NOT NULL,
                `settlement_type` TEXT NOT NULL,
                `paid_amount` INTEGER NOT NULL DEFAULT 0,
                `subtotal` INTEGER NOT NULL DEFAULT 0,
                `discount_total` INTEGER NOT NULL DEFAULT 0,
                `tax_total` INTEGER NOT NULL DEFAULT 0,
                `shipping_cost` INTEGER NOT NULL DEFAULT 0,
                `other_costs` INTEGER NOT NULL DEFAULT 0,
                `grand_total` INTEGER NOT NULL DEFAULT 0,
                `status` TEXT NOT NULL DEFAULT 'CONFIRMED',
                `parent_id` TEXT,
                `notes` TEXT,
                `created_by` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `store_id` TEXT NOT NULL,
                `fiscal_year_id` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_invoices_store_id_fiscal_year_id_type_number` ON `invoices` (`store_id`, `fiscal_year_id`, `type`, `number`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_party_id` ON `invoices` (`party_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_issue_date` ON `invoices` (`issue_date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoices_store_id_type` ON `invoices` (`store_id`, `type`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `invoice_lines` (
                `id` TEXT NOT NULL,
                `invoice_id` TEXT NOT NULL,
                `line_no` INTEGER NOT NULL,
                `product_id` TEXT NOT NULL,
                `description` TEXT,
                `unit_id` TEXT NOT NULL,
                `quantity` INTEGER NOT NULL,
                `unit_price` INTEGER NOT NULL,
                `discount` INTEGER NOT NULL DEFAULT 0,
                `tax_rate` INTEGER NOT NULL DEFAULT 0,
                `line_total` INTEGER NOT NULL,
                `cost_at_sale` INTEGER,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_lines_invoice_id` ON `invoice_lines` (`invoice_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_invoice_lines_product_id` ON `invoice_lines` (`product_id`)")
    }
}