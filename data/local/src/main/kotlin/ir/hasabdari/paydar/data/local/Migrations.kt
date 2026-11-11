package ir.hasabdari.paydar.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `warehouses` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL,
                `is_default` INTEGER NOT NULL DEFAULT 0,
                `is_active` INTEGER NOT NULL DEFAULT 1,
                `store_id` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_warehouses_store_id_name` ON `warehouses` (`store_id`, `name`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stock` (
                `product_id` TEXT NOT NULL, `warehouse_id` TEXT NOT NULL,
                `quantity` INTEGER NOT NULL DEFAULT 0, `avg_cost` INTEGER NOT NULL DEFAULT 0,
                `last_updated` INTEGER NOT NULL,
                PRIMARY KEY(`product_id`, `warehouse_id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_warehouse_id` ON `stock` (`warehouse_id`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `stock_movements` (
                `id` TEXT NOT NULL, `product_id` TEXT NOT NULL, `warehouse_id` TEXT NOT NULL,
                `type` TEXT NOT NULL, `quantity_delta` INTEGER NOT NULL, `unit_cost` INTEGER NOT NULL,
                `reference_type` TEXT, `reference_id` TEXT, `note` TEXT,
                `occurred_at` INTEGER NOT NULL, `store_id` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_product_id_warehouse_id_occurred_at` ON `stock_movements` (`product_id`, `warehouse_id`, `occurred_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_stock_movements_store_id` ON `stock_movements` (`store_id`)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `product_groups` (
                `id` TEXT NOT NULL, `name` TEXT NOT NULL, `parent_id` TEXT,
                `is_active` INTEGER NOT NULL DEFAULT 1, `store_id` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, `deleted_at` INTEGER,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_groups_store_id_parent_id` ON `product_groups` (`store_id`, `parent_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_product_groups_store_id_name` ON `product_groups` (`store_id`, `name`)")

        db.execSQL("ALTER TABLE parties ADD COLUMN economic_code TEXT")
        db.execSQL("ALTER TABLE parties ADD COLUMN province TEXT")
        db.execSQL("ALTER TABLE parties ADD COLUMN city TEXT")
        db.execSQL("ALTER TABLE parties ADD COLUMN group_name TEXT")
    }
}

/** F3: خزانه */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cash_accounts` (
                `id` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `bank_name` TEXT,
                `account_number` TEXT,
                `opening_balance` INTEGER NOT NULL DEFAULT 0,
                `is_active` INTEGER NOT NULL DEFAULT 1,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `store_id` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_accounts_store_id_name` ON `cash_accounts` (`store_id`, `name`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `cash_transactions` (
                `id` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `method` TEXT NOT NULL,
                `cash_account_id` TEXT NOT NULL,
                `to_cash_account_id` TEXT,
                `party_id` TEXT,
                `amount` INTEGER NOT NULL,
                `occurred_at` INTEGER NOT NULL,
                `description` TEXT,
                `created_at` INTEGER NOT NULL,
                `store_id` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_transactions_cash_account_id_occurred_at` ON `cash_transactions` (`cash_account_id`, `occurred_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_transactions_to_cash_account_id` ON `cash_transactions` (`to_cash_account_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_cash_transactions_store_id` ON `cash_transactions` (`store_id`)")
    }
}