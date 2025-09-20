package com.example.alias

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    @Singleton
    abstract fun bindGameEngineFactory(factory: DefaultGameEngineFactory): GameEngineFactory

    @Binds
    @Singleton
    abstract fun bindBundledDeckProvider(provider: AssetBundledDeckProvider): BundledDeckProvider

    @Binds
    @Singleton
    abstract fun bindDeckManagerLogger(logger: AndroidDeckManagerLogger): DeckManagerLogger
}
