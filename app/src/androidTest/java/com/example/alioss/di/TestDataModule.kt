package com.example.alioss.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.alioss.data.DeckRepository
import com.example.alioss.data.DeckRepositoryImpl
import com.example.alioss.data.TurnHistoryRepository
import com.example.alioss.data.TurnHistoryRepositoryImpl
import com.example.alioss.data.achievements.AchievementsRepository
import com.example.alioss.data.achievements.AchievementsRepositoryImpl
import com.example.alioss.data.db.AliossDatabase
import com.example.alioss.data.db.DeckDao
import com.example.alioss.data.db.TurnHistoryDao
import com.example.alioss.data.db.WordDao
import com.example.alioss.data.di.DataModule
import com.example.alioss.data.download.PackDownloader
import com.example.alioss.data.settings.SettingsRepository
import com.example.alioss.data.settings.SettingsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataModule::class],
)
object TestDataModule {
    private val dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AliossDatabase {
        return Room.inMemoryDatabaseBuilder(context, AliossDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    fun provideDeckDao(db: AliossDatabase): DeckDao = db.deckDao()

    @Provides
    fun provideWordDao(db: AliossDatabase): WordDao = db.wordDao()

    @Provides
    fun provideTurnHistoryDao(db: AliossDatabase): TurnHistoryDao = db.turnHistoryDao()

    @Provides
    @Singleton
    fun provideDeckRepository(db: AliossDatabase, deckDao: DeckDao, wordDao: WordDao): DeckRepository =
        DeckRepositoryImpl(db, deckDao, wordDao)

    @Provides
    @Singleton
    fun provideTurnHistoryRepository(dao: TurnHistoryDao): TurnHistoryRepository =
        TurnHistoryRepositoryImpl(dao)

    @Provides
    @Singleton
    @Named("settings")
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("instrumentation_settings") },
        )

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @Named("settings") dataStore: DataStore<Preferences>,
    ): SettingsRepository =
        SettingsRepositoryImpl(dataStore)

    @Provides
    @Singleton
    @Named("achievements")
    fun provideAchievementsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("instrumentation_achievements") },
        )

    @Provides
    @Singleton
    fun provideAchievementsRepository(
        @Named("achievements") dataStore: DataStore<Preferences>,
    ): AchievementsRepository =
        AchievementsRepositoryImpl(dataStore)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun providePackDownloader(
        client: OkHttpClient,
        settingsRepository: SettingsRepository,
    ): PackDownloader =
        PackDownloader(client, settingsRepository)
}
