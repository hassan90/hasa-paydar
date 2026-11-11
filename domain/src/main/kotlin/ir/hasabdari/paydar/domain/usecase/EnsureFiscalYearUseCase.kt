package ir.hasabdari.paydar.domain.usecase

import ir.hasabdari.paydar.core.AppConstants
import ir.hasabdari.paydar.core.date.JalaliDate
import ir.hasabdari.paydar.core.error.DomainError
import ir.hasabdari.paydar.core.result.AppResult
import ir.hasabdari.paydar.domain.model.FiscalYear
import ir.hasabdari.paydar.domain.repository.FiscalYearRepository
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

/**
 * سال مالی جاری را برمی‌گرداند؛ اگر نبود، آن را می‌سازد.
 * از ۱ فروردین تا ۲۹/۳۰ اسفند همان سال شمسی.
 */
class EnsureFiscalYearUseCase @Inject constructor(
    private val repo: FiscalYearRepository
) {
    suspend operator fun invoke(storeId: String = AppConstants.DEFAULT_STORE_ID): AppResult<FiscalYear> {
        val current = repo.getCurrent(storeId)
        val currentData = (current as? AppResult.Success)?.data
        if (currentData != null) return AppResult.Success(currentData)

        val today = JalaliDate.today()
        val zone = ZoneId.of("Asia/Tehran")

        // تخمین: اول فروردین ≈ ۲۰ مارس
        val startGregorian = LocalDate.of(today.year + 621, 3, 21)
        val endGregorian = startGregorian.plusDays(364)
        val startMs = startGregorian.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = endGregorian.atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()

        val fy = FiscalYear(
            id = UUID.randomUUID().toString(),
            title = "سال مالی ${JalaliDate.toPersianDigits(today.year.toString())}",
            startDate = startMs,
            endDate = endMs,
            isClosed = false,
            storeId = storeId
        )

        return when (val saveResult = repo.save(fy)) {
            is AppResult.Success -> AppResult.Success(fy)
            is AppResult.Failure -> saveResult
        }
    }
}