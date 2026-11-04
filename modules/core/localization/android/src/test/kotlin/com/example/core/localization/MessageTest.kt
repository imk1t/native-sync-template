package com.example.core.localization

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageTest {
    private fun context(language: String): Context {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(target.resources.configuration)
        configuration.setLocales(LocaleList(Locale.forLanguageTag(language)))
        return target.createConfigurationContext(configuration)
    }

    // @scenario localization/language-change
    @Test
    fun nativeResourcesAndLocaleChanges() {
        val message = Message.CommonRetry
        assertEquals("再試行", message.resolve(context("ja")))
        assertEquals("Retry", message.resolve(context("en")))
        assertEquals("再試行", message.resolve(context("fr")))
    }

    // @scenario localization/plural-arguments
    @Test
    fun pluralAndArguments() {
        val english = context("en")
        assertEquals("1 star", Message.RepositoryStars(1).resolve(english))
        assertEquals("2 stars", Message.RepositoryStars(2).resolve(english))
        assertEquals("0 stars", Message.RepositoryStars(0).resolve(english))
        assertEquals("Star 2", Message.RepositoryStars(2).resolve(context("ja")))
        assertEquals("Language: Swift", Message.SearchFilter("Language", "Swift").resolve(english))
        assertEquals("Request failed (HTTP 503). Please try again later.", Message.ErrorServer(503).resolve(english))
    }
}
