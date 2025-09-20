package com.example.alias

import com.example.alias.domain.DefaultGameEngine
import com.example.alias.domain.GameEngine
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

interface GameEngineFactory {
    fun create(words: List<String>, scope: CoroutineScope): GameEngine
}

@Singleton
class DefaultGameEngineFactory
    @Inject
    constructor() : GameEngineFactory {
        override fun create(words: List<String>, scope: CoroutineScope): GameEngine {
            return DefaultGameEngine(words, scope)
        }
    }
