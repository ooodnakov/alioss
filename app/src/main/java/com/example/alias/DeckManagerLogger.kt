package com.example.alias

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

interface DeckManagerLogger {
    fun debug(message: String)
    fun warn(message: String, error: Throwable? = null)
    fun error(message: String, error: Throwable? = null)
}

@Singleton
class AndroidDeckManagerLogger
    @Inject
    constructor() : DeckManagerLogger {
        override fun debug(message: String) {
            Log.d(TAG, message)
        }

        override fun warn(message: String, error: Throwable?) {
            if (error != null) {
                Log.w(TAG, message, error)
            } else {
                Log.w(TAG, message)
            }
        }

        override fun error(message: String, error: Throwable?) {
            if (error != null) {
                Log.e(TAG, message, error)
            } else {
                Log.e(TAG, message)
            }
        }

        private companion object {
            private const val TAG = "DeckManager"
        }
    }
