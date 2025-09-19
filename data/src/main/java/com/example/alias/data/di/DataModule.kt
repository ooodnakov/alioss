package com.example.alias.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.alias.data.DeckRepository
import com.example.alias.data.DeckRepositoryImpl
import com.example.alias.data.TurnHistoryRepository
import com.example.alias.data.TurnHistoryRepositoryImpl
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
    fun provideDatabase(@ApplicationContext context: Context): AliasDatabase =
        Room.databaseBuilder(context, AliasDatabase::class.java, "alias.db")
            .fallbackToDestructiveMigration()
            .build()

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
