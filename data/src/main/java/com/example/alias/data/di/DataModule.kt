package com.example.alias.data.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.alias.data.DeckRepository
import com.example.alias.data.DeckRepositoryImpl
import com.example.alias.data.TurnHistoryRepository
import com.example.alias.data.TurnHistoryRepositoryImpl
import com.example.alias.data.db.ALL_MIGRATIONS
import com.example.alias.data.db.AliasDatabase
import com.example.alias.data.db.DeckDao
import com.example.alias.data.db.TurnHistoryDao
import com.example.alias.data.db.WordDao
import com.example.alias.data.download.PackDownloader
import com.example.alias.data.settings.SettingsRepository
import com.example.alias.data.settings.SettingsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AliasDatabase {
        Log.i("DataModule", "Creating database with name: alias.db")
        Log.i("DataModule", "Database schema version: 7")
        Log.i("DataModule", "Number of migrations: ${ALL_MIGRATIONS.size}")

        val builder = Room.databaseBuilder(context, AliasDatabase::class.java, "alias.db")
            .addMigrations(*ALL_MIGRATIONS)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    Log.i("DataModule", "Database created successfully")
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    Log.i("DataModule", "Database opened successfully")
                }

                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    Log.w("DataModule", "DESTRUCTIVE MIGRATION OCCURRED - All user data will be lost!")
                }
            })

        // Only enable destructive fallback for development builds to preserve user data
        // Build configuration:
        // - debug: ENABLE_DESTRUCTIVE_MIGRATION_FALLBACK = true (allows destructive fallback)
        // - devRelease: ENABLE_DESTRUCTIVE_MIGRATION_FALLBACK = false (preserves user data)
        // - release: ENABLE_DESTRUCTIVE_MIGRATION_FALLBACK = false (preserves user data)

        // Use a more robust approach to determine if we should enable destructive fallback
        val shouldEnableDestructiveFallback = try {
            // First try to access BuildConfig using reflection
            val appBuildConfig = Class.forName("com.example.alias.BuildConfig")
            Log.d("DataModule", "Successfully accessed BuildConfig class: $appBuildConfig")

            val field = appBuildConfig.getField("ENABLE_DESTRUCTIVE_MIGRATION_FALLBACK")
            Log.d("DataModule", "Successfully accessed field: $field")

            val enableDestructiveFallback = field.getBoolean(null)
            Log.d("DataModule", "ENABLE_DESTRUCTIVE_MIGRATION_FALLBACK value: $enableDestructiveFallback")
            enableDestructiveFallback
        } catch (e: Exception) {
            Log.e("DataModule", "Could not access BuildConfig, checking alternative methods. Error: ${e.message}", e)

            // Fallback: Check if we're in a debuggable build using ApplicationInfo
            try {
                val appInfo = context.packageManager.getApplicationInfo(context.packageName, 0)
                val isDebuggable = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                Log.d("DataModule", "Application is debuggable: $isDebuggable")

                // Only enable destructive fallback for debuggable builds
                // This preserves user data in release and devRelease builds
                isDebuggable
            } catch (e2: Exception) {
                Log.e("DataModule", "Could not determine if app is debuggable, preserving user data by default. Error: ${e2.message}", e2)
                // If we can't determine anything, preserve user data
                false
            }
        }

        // Apply the decision
        if (shouldEnableDestructiveFallback) {
            Log.w("DataModule", "Enabling destructive migration fallback - user data will be lost!")
            builder.fallbackToDestructiveMigration()
        } else {
            Log.d("DataModule", "Destructive migration fallback disabled - preserving user data")
        }

        // Additional safety: Log the final database configuration
        Log.i("DataModule", "Database configuration complete. Destructive fallback enabled: $shouldEnableDestructiveFallback")

        return builder.build()
    }

    @Provides
    fun provideDeckDao(db: AliasDatabase): DeckDao = db.deckDao()

    @Provides
    fun provideWordDao(db: AliasDatabase): WordDao = db.wordDao()

    @Provides
    fun provideTurnHistoryDao(db: AliasDatabase): TurnHistoryDao = db.turnHistoryDao()

    @Provides
    @Singleton
    fun provideDeckRepository(db: AliasDatabase, deckDao: DeckDao, wordDao: WordDao): DeckRepository =
        DeckRepositoryImpl(db, deckDao, wordDao)

    @Provides
    @Singleton
    fun provideTurnHistoryRepository(dao: TurnHistoryDao): TurnHistoryRepository =
        TurnHistoryRepositoryImpl(dao)

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("alias_settings") },
        )

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        SettingsRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideOkHttpClient(): okhttp3.OkHttpClient =
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun providePackDownloader(client: okhttp3.OkHttpClient, settings: SettingsRepository): PackDownloader =
        PackDownloader(client, settings)
}
