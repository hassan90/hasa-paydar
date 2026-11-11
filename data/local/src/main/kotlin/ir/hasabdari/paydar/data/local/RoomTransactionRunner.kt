package ir.hasabdari.paydar.data.local

import androidx.room.withTransaction
import ir.hasabdari.paydar.core.transaction.TransactionRunner
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTransactionRunner @Inject constructor(
    private val db: PaydarDatabase
) : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T =
        db.withTransaction { block() }
}