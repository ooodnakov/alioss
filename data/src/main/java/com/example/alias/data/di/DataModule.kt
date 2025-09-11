package com.example.alias.data.di

import android.content.Context
import androidx.room.Room
import com.example.alias.data.DeckRepository
import com.example.alias.data.DeckRepositoryImpl
import com.example.alias.data.db.AliasDatabase
import com.example.alias.data.db.DeckDao
import com.example.alias.data.db.WordDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AliasDatabase =
        Room.databaseBuilder(context, AliasDatabase::class.java, "alias.db").build()

    @Provides
    fun provideDeckDao(db: AliasDatabase): DeckDao = db.deckDao()

    @Provides
    fun provideWordDao(db: AliasDatabase): WordDao = db.wordDao()

    @Provides
    @Singleton
    fun provideDeckRepository(deckDao: DeckDao, wordDao: WordDao): DeckRepository =
        DeckRepositoryImpl(deckDao, wordDao)
}
