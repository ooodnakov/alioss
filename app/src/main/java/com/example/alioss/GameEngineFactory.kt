package com.example.alioss

import com.example.alioss.domain.DefaultGameEngine
import com.example.alioss.domain.GameEngine
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
