package com.example.alioss

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.junit.Ignore("Requires Robolectric runtime")
class LocalizationTest {
    @Test
    fun switchingLocaleUpdatesStrings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        assertEquals("Settings", context.getString(R.string.title_settings))
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
        val res = context.resources
        assertEquals("\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438", res.getString(R.string.title_settings))
    }
}
