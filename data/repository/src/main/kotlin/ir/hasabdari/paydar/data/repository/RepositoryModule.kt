package ir.hasabdari.paydar.data.repository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.hasabdari.paydar.core.transaction.TransactionRunner
import ir.hasabdari.paydar.data.local.RoomTransactionRunner
import ir.hasabdari.paydar.data.repository.settings.SettingsRepositoryImpl
import ir.hasabdari.paydar.domain.repository.AccountRepository
import ir.hasabdari.paydar.domain.repository.CashAccountRepository
import ir.hasabdari.paydar.domain.repository.CashTransactionRepository
import ir.hasabdari.paydar.domain.repository.ChequeRepository
import ir.hasabdari.paydar.domain.repository.DashboardRepository
import ir.hasabdari.paydar.domain.repository.FiscalYearRepository
import ir.hasabdari.paydar.domain.repository.InvoiceRepository
import ir.hasabdari.paydar.domain.repository.JournalRepository
import ir.hasabdari.paydar.domain.repository.PartyRepository
import ir.hasabdari.paydar.domain.repository.ProductGroupRepository
import ir.hasabdari.paydar.domain.repository.ProductRepository
import ir.hasabdari.paydar.domain.repository.ReportRepository
import ir.hasabdari.paydar.domain.repository.SettingsRepository
import ir.hasabdari.paydar.domain.repository.StockRepository
import ir.hasabdari.paydar.domain.repository.WarehouseRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindProductRepository(impl: ProductRepositoryImpl): ProductRepository

    @Binds @Singleton
    abstract fun bindWarehouseRepository(impl: WarehouseRepositoryImpl): WarehouseRepository

    @Binds @Singleton
    abstract fun bindStockRepository(impl: StockRepositoryImpl): StockRepository

    @Binds @Singleton
    abstract fun bindPartyRepository(impl: PartyRepositoryImpl): PartyRepository

    @Binds @Singleton
    abstract fun bindProductGroupRepository(impl: ProductGroupRepositoryImpl): ProductGroupRepository

    @Binds @Singleton
    abstract fun bindCashAccountRepository(impl: CashAccountRepositoryImpl): CashAccountRepository

    @Binds @Singleton
    abstract fun bindCashTransactionRepository(impl: CashTransactionRepositoryImpl): CashTransactionRepository

    @Binds @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds @Singleton
    abstract fun bindFiscalYearRepository(impl: FiscalYearRepositoryImpl): FiscalYearRepository

    @Binds @Singleton
    abstract fun bindJournalRepository(impl: JournalRepositoryImpl): JournalRepository

    @Binds @Singleton
    abstract fun bindInvoiceRepository(impl: InvoiceRepositoryImpl): InvoiceRepository
    @Binds @Singleton
    abstract fun bindChequeRepository(impl: ChequeRepositoryImpl): ChequeRepository
    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds @Singleton
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner
    @Binds @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository
    @Binds @Singleton
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository
}