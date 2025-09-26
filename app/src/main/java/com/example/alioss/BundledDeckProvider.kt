package com.example.alioss

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface BundledDeckProvider {
    fun listBundledDeckFiles(): List<String>
    fun readDeckAsset(fileName: String): String?
}

@Singleton
class AssetBundledDeckProvider
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : BundledDeckProvider {
    override fun listBundledDeckFiles(): List<String> {
        return context.assets.list("decks")?.filter { it.endsWith(".json") } ?: emptyList()
    }

    override fun readDeckAsset(fileName: String): String? {
        return runCatching {
            context.assets.open("decks/$fileName").bufferedReader().use { it.readText() }
        }.getOrElse { error ->
            Log.e(TAG, "Failed to read bundled deck asset $fileName", error)
            null
        }
    }

    private companion object {
        private const val TAG = "BundledDeckProvider"
    }
}
