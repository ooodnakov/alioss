package com.example.alias.ui

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alias.R

/**
 * Fullscreen countdown overlay with animated ticks and accessibility announcements.
 *
 * Pass `null` or <= 0 to hide it.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CountdownOverlay(
    value: Int?,
    modifier: Modifier = Modifier,
    scrimColor: Color = MaterialTheme.colorScheme.scrim,
    scrimAlpha: Float = 0.7f,
    textStyle: TextStyle = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    consumeTouches: Boolean = true,
    performHaptics: Boolean = true,
) {
    if (value == null || value <= 0) return

    val announcement = stringResource(R.string.countdown_announcement, value)

    val haptics = LocalHapticFeedback.current
    LaunchedEffect(value) {
        if (performHaptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    val interaction = remember { MutableInteractionSource() }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scrimColor.copy(alpha = scrimAlpha))
            .onSizeChanged { size ->
                Log.d("CountdownOverlay", "Overlay size: ${size.width}x${size.height}")
                Log.d("CountdownOverlay", "Screen size: ${screenWidthPx.toInt()}x${screenHeightPx.toInt()}")
            }
            // Swallow taps so the game underneath doesn't get accidental clicks.
            .then(
                if (consumeTouches) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = {},
                    )
                } else {
                    Modifier
                },
            )
            // Screen reader announces each tick.
            .semantics {
                contentDescription = announcement
                liveRegion = LiveRegionMode.Assertive
            },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                // Pop in with bounce, then scale/fade out.
                (
                    scaleIn(
                        initialScale = 0.6f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessLow,
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                        ),
                    ) + fadeIn()
                    ) togetherWith
                    (
                        scaleOut(
                            targetScale = 1.1f,
                            animationSpec = tween(200, easing = LinearOutSlowInEasing),
                        ) + fadeOut(tween(150))
                        )
            },
            label = "countdown",
        ) { number ->
            Text(
                text = number.toString(),
                style = textStyle,
                color = textColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
