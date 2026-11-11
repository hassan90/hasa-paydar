package ir.hasabdari.paydar.core.result

import ir.hasabdari.paydar.core.error.DomainError

/**
 * Result type سبک برای انتقال موفقیت/شکست از UseCaseها.
 * از Kotlin's built-in Result استفاده نمی‌کنیم چون برای خطاهای دامنه نیاز به type-safe داریم.
 *
 * نکته مهم: متدهای inline به دلیل محدودیت Kotlin در interfaceها، به صورت
 * top-level extension function تعریف شده‌اند.
 */
sealed interface AppResult<out T> {

    data class Success<T>(val data: T) : AppResult<T>

    data class Failure(val error: DomainError) : AppResult<Nothing>

    fun getOrNull(): T? = (this as? Success)?.data

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw IllegalStateException(error.message, error.cause)
    }
}

// ─── Extension Functions (inline مجاز) ───

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (DomainError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

inline fun <T> runCatchingDomain(block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: DomainException) {
    AppResult.Failure(e.error)
}

/** استثنای دامنه — برای ترجمه‌ی خطاهای سطح پایین به DomainError */
class DomainException(val error: DomainError, cause: Throwable? = null) : Exception(error.message, cause)