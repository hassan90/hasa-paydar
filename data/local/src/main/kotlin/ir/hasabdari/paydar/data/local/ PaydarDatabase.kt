package ir.hasabdari.paydar.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.hasabdari.paydar.data.local.dao.*
import ir.hasabdari.paydar.data.local.entity.*

@Database(
    entities = [
        ProductEntity::class, PartyEntity::class,
        WarehouseEntity::class, StockEntity::class, StockMovementEntity::class,
        ProductGroupEntity::class,
        CashAccountEntity::class, CashTransactionEntity::class,
        AccountEntity::class, FiscalYearEntity::class,
        JournalEntryEntity::class, JournalLineEntity::class,
        InvoiceEntity::class, InvoiceLineEntity::class,
        ChequeEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class PaydarDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun partyDao(): PartyDao
    abstract fun warehouseDao(): WarehouseDao
    abstract fun stockDao(): StockDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun productGroupDao(): ProductGroupDao
    abstract fun cashAccountDao(): CashAccountDao
    abstract fun cashTransactionDao(): CashTransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun fiscalYearDao(): FiscalYearDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun journalLineDao(): JournalLineDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun reportDao(): ReportDao
    abstract fun chequeDao(): ChequeDao
    abstract fun dashboardDao(): DashboardDao
    companion object {
        const val DB_NAME = "hasabdari_paydar.db"
    }


}