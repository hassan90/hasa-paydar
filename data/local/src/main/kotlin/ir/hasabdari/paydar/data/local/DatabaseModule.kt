package ir.hasabdari.paydar.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.hasabdari.paydar.data.local.dao.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        openHelperFactory: SupportOpenHelperFactory
    ): PaydarDatabase =
        Room.databaseBuilder(context, PaydarDatabase::class.java, PaydarDatabase.DB_NAME)
            .openHelperFactory(openHelperFactory)
            .addMigrations(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
            )
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides fun provideProductDao(db: PaydarDatabase): ProductDao = db.productDao()
    @Provides fun providePartyDao(db: PaydarDatabase): PartyDao = db.partyDao()
    @Provides fun provideWarehouseDao(db: PaydarDatabase): WarehouseDao = db.warehouseDao()
    @Provides fun provideStockDao(db: PaydarDatabase): StockDao = db.stockDao()
    @Provides fun provideStockMovementDao(db: PaydarDatabase): StockMovementDao = db.stockMovementDao()
    @Provides fun provideProductGroupDao(db: PaydarDatabase): ProductGroupDao = db.productGroupDao()
    @Provides fun provideCashAccountDao(db: PaydarDatabase): CashAccountDao = db.cashAccountDao()
    @Provides fun provideCashTransactionDao(db: PaydarDatabase): CashTransactionDao = db.cashTransactionDao()
    @Provides fun provideAccountDao(db: PaydarDatabase): AccountDao = db.accountDao()
    @Provides fun provideFiscalYearDao(db: PaydarDatabase): FiscalYearDao = db.fiscalYearDao()
    @Provides fun provideJournalEntryDao(db: PaydarDatabase): JournalEntryDao = db.journalEntryDao()
    @Provides fun provideJournalLineDao(db: PaydarDatabase): JournalLineDao = db.journalLineDao()
    @Provides fun provideInvoiceDao(db: PaydarDatabase): InvoiceDao = db.invoiceDao()
    @Provides fun provideReportDao(db: PaydarDatabase): ReportDao = db.reportDao()
    @Provides fun provideChequeDao(db: PaydarDatabase): ChequeDao = db.chequeDao()
    @Provides fun provideDashboardDao(db: PaydarDatabase): DashboardDao = db.dashboardDao()

}