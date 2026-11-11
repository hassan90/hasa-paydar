package ir.hasabdari.paydar.data.local

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseSecurityModule {

    // ⚠️ دیگر provideSecureKeyManager لازم نیست — چون @Inject constructor دارد
    // و در همان ماژول data/local با Hilt KSP پردازش می‌شود.

    @Provides
    @Singleton
    fun provideOpenHelperFactory(
        @ApplicationContext context: Context,
        keyProvider: AppDatabaseKeyProvider
    ): SupportOpenHelperFactory {
        System.loadLibrary("sqlcipher")
        return SupportOpenHelperFactory(keyProvider.passphrase())
    }
}