package com.example.alias.ui

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WordCardAction { Correct, Skip }

private val COMMIT_DISTANCE = 112.dp
private const val ROTATION_DIVISOR = 20f
private const val HAPTIC_DURATION_MS = 10
private const val SWIPE_AWAY_MULTIPLIER = 4

private suspend fun resetCard(
    offsetX: Animatable<Float, *>,
    rotationZ: Animatable<Float, *>
) {
    coroutineScope {
        launch { offsetX.animateTo(0f) }
        launch { rotationZ.animateTo(0f) }
    }
}

@Composable
fun WordCard(
    word: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    vibrator: Vibrator?,
    hapticsEnabled: Boolean,
    onActionStart: () -> Unit,
    onAction: (WordCardAction) -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val rotationZ = remember { Animatable(0f) }
    val fadeIn = remember { Animatable(0f) }
    var hapticPlayed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val commitPx = with(density) { COMMIT_DISTANCE.toPx() }

    LaunchedEffect(word) {
        offsetX.snapTo(0f)
        rotationZ.snapTo(0f)
        fadeIn.snapTo(0f)
        hapticPlayed = false
        fadeIn.animateTo(1f, tween(250))
    }

    val fraction = (abs(offsetX.value) / commitPx).coerceIn(0f, 1f)
    val scale = 1f + fraction * 0.05f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer { this.rotationZ = rotationZ.value; shadowElevation = 8.dp.toPx() }
            .scale(scale)
            .alpha(fadeIn.value)
            .semantics { contentDescription = "$word. Swipe right for Correct, left for Skip." }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { hapticPlayed = false },
                    onDrag = { change, drag ->
                        change.consume()
                        val newX = offsetX.value + drag.x
                        offsetX.snapTo(newX)
                        rotationZ.snapTo(newX / ROTATION_DIVISOR)
                        if (!hapticPlayed && abs(newX) > commitPx) {
                            hapticPlayed = true
                            if (hapticsEnabled) {
                                vibrator?.vibrate(
                                    VibrationEffect.createOneShot(
                                        HAPTIC_DURATION_MS.toLong(),
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        resetCard(offsetX, rotationZ)
                    },
                    onDragEnd = {
                        if (abs(offsetX.value) > commitPx) {
                            onActionStart()
                            val dir = if (offsetX.value > 0f) WordCardAction.Correct else WordCardAction.Skip
                            val target = if (dir == WordCardAction.Correct) {
                                commitPx * SWIPE_AWAY_MULTIPLIER
                            } else {
                                -commitPx * SWIPE_AWAY_MULTIPLIER
                            }
                            coroutineScope {
                                launch { offsetX.animateTo(target, tween(200)) }
                                launch { rotationZ.animateTo(target / ROTATION_DIVISOR, tween(200)) }
                            }
                            onAction(dir)
                        } else {
                            resetCard(offsetX, rotationZ)
                        }
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Correct",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .alpha(if (offsetX.value > 0f) fraction else 0f)
            )
            Text(
                text = "Skip",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .alpha(if (offsetX.value < 0f) fraction else 0f)
            )
        }
    }
}
