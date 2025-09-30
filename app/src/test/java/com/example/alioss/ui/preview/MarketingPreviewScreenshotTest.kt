package com.example.alioss.ui.preview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MarketingPreviewScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun generateMarketingScreenshots() {
        check(MarketingPreviewSpecs.isNotEmpty()) { "No marketing previews registered" }

        val outputDir = File("build/marketing-previews").apply {
            deleteRecursively()
            mkdirs()
        }

        // Allow overriding density via system property or environment variable for higher-resolution exports.
        val screenshotDensity = System.getProperty("marketing.preview.density")?.toFloatOrNull()
            ?: System.getenv("MARKETING_PREVIEW_DENSITY")?.toFloatOrNull()
            ?: 1f

        check(screenshotDensity > 0f) { "Screenshot density must be greater than zero" }

        var currentSpec by mutableStateOf(MarketingPreviewSpecs.first())

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalInspectionMode provides true,
                LocalDensity provides Density(
                    density = screenshotDensity,
                    fontScale = baseDensity.fontScale,
                ),
            ) {
                val activeSpec = currentSpec
                Box(
                    modifier = Modifier
                        .size(activeSpec.widthDp.dp, activeSpec.heightDp.dp)
                        .background(Color.White),
                ) {
                    activeSpec.content()
                }
            }
        }

        MarketingPreviewSpecs.forEach { spec ->
            composeRule.runOnIdle { currentSpec = spec }
            composeRule.waitForIdle()

            val widthPx = (spec.widthDp * screenshotDensity).roundToInt()
            val heightPx = (spec.heightDp * screenshotDensity).roundToInt()

            val targetView = composeRule.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val measuredView = checkNotNull(targetView) { "No content view found for ${spec.displayName}" }

            composeRule.runOnIdle {
                measuredView.measure(
                    View.MeasureSpec.makeMeasureSpec(widthPx, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(heightPx, View.MeasureSpec.EXACTLY),
                )
                measuredView.layout(0, 0, widthPx, heightPx)
            }

            val expectedDimensions = "${widthPx}x${heightPx}"
            val actualDimensions = "${measuredView.width}x${measuredView.height}"

            check(measuredView.width == widthPx && measuredView.height == heightPx) {
                "${spec.displayName} rendered at $actualDimensions instead of $expectedDimensions"
            }

            val capturedBitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            composeRule.runOnIdle {
                val canvas = Canvas(capturedBitmap)
                measuredView.draw(canvas)
            }

            val outputFile = outputDir.resolve("${spec.id}.png")
            FileOutputStream(outputFile).use { stream ->
                capturedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }

            check(outputFile.length() > 0) { "${spec.displayName} screenshot was empty" }
        }
    }
}
