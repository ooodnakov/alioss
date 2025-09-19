package com.example.alias.ui

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.alias.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

enum class WordCardAction { Correct, Skip }

private val COMMIT_DISTANCE = 112.dp
private const val ROTATION_DIVISOR = 20f
private const val HAPTIC_DURATION_MS = 10
private const val SWIPE_AWAY_MULTIPLIER = 4

// No-op helper removed; handled inline with a coroutine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordCard(
    word: String,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    vibrator: Vibrator?,
    hapticsEnabled: Boolean,
    onActionStart: () -> Unit,
    onAction: (WordCardAction) -> Unit,
    animateAppear: Boolean = true,
    allowSkip: Boolean = true,
    verticalMode: Boolean = false,
    testTag: String? = null,
    wordDifficulty: Int? = null,
    wordCategory: String? = null,
    wordClass: String? = null,
) {
    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }
    val fadeIn = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragX by remember { mutableStateOf(0f) }
    var dragY by remember { mutableStateOf(0f) }
    var hapticPlayed by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val commitPx = with(density) { COMMIT_DISTANCE.toPx() }
    val instructionsRes = if (allowSkip) {
        if (verticalMode) {
            R.string.tutorial_instructions_vertical
        } else {
            R.string.tutorial_instructions
        }
    } else {
        if (verticalMode) {
            R.string.tutorial_instructions_vertical_no_skip
        } else {
            R.string.tutorial_instructions_no_skip
        }
    }
    val instructions = stringResource(instructionsRes)

    LaunchedEffect(word) {
        animX.snapTo(0f)
        animY.snapTo(0f)
        fadeIn.snapTo(0f)
        hapticPlayed = false
        if (animateAppear) {
            fadeIn.animateTo(1f, tween(250))
        } else {
            fadeIn.snapTo(1f)
        }
    }

    val currentX = dragX + animX.value
    val currentY = dragY + animY.value
    val distance = if (verticalMode) abs(currentY) else abs(currentX)
    val fraction = (distance / commitPx).coerceIn(0f, 1f)
    val scale = 1f + fraction * 0.05f

    val metadataItems = buildList {
        wordDifficulty?.let { add(stringResource(R.string.word_difficulty_value, it) to true) }
        wordCategory?.takeIf { it.isNotBlank() }?.let { add(it to false) }
        wordClass?.takeIf { it.isNotBlank() }?.let { add(it to false) }
    }

    Surface(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .fillMaxWidth()
            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
            .graphicsLayer(rotationZ = if (verticalMode) 0f else currentX / ROTATION_DIVISOR)
            .scale(scale)
            .alpha(fadeIn.value)
            .semantics { contentDescription = "$word. $instructions" }
            .pointerInput(word, allowSkip, verticalMode) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { hapticPlayed = false },
                    onDrag = { _, drag ->
                        if (verticalMode) {
                            dragY += drag.y
                        } else {
                            dragX += drag.x
                        }
                        val commitAllowed = if (verticalMode) {
                            val up = dragY < -commitPx
                            val down = dragY > commitPx && allowSkip
                            up || down
                        } else {
                            val right = dragX > commitPx
                            val left = dragX < -commitPx && allowSkip
                            right || left
                        }
                        if (!hapticPlayed && commitAllowed) {
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
                        scope.launch {
                            // Convert accumulated drag into animatable position, then return to center
                            val endX = dragX + animX.value
                            val endY = dragY + animY.value
                            dragX = 0f
                            dragY = 0f
                            animX.snapTo(endX)
                            animY.snapTo(endY)
                            animX.animateTo(0f, tween(200))
                            animY.animateTo(0f, tween(200))
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            // Convert accumulated drag into animatable position, then decide action
                            val endX = dragX + animX.value
                            val endY = dragY + animY.value
                            dragX = 0f
                            dragY = 0f
                            animX.snapTo(endX)
                            animY.snapTo(endY)
                            val commit = if (verticalMode) abs(endY) > commitPx else abs(endX) > commitPx
                            val correctDir = if (verticalMode) endY < 0f else endX > 0f
                            val dir = if (correctDir) WordCardAction.Correct else WordCardAction.Skip
                            val allowed = (dir == WordCardAction.Correct) || (dir == WordCardAction.Skip && allowSkip)
                            if (commit && allowed) {
                                onActionStart()
                                if (verticalMode) {
                                    val targetY = if (dir == WordCardAction.Correct) -commitPx * SWIPE_AWAY_MULTIPLIER else commitPx * SWIPE_AWAY_MULTIPLIER
                                    animY.animateTo(targetY, tween(200))
                                } else {
                                    val targetX = if (dir == WordCardAction.Correct) commitPx * SWIPE_AWAY_MULTIPLIER else -commitPx * SWIPE_AWAY_MULTIPLIER
                                    animX.animateTo(targetX, tween(200))
                                }
                                onAction(dir)
                            } else {
                                animX.animateTo(0f, tween(200))
                                animY.animateTo(0f, tween(200))
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = word,
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            if (verticalMode) {
                Text(
                    text = stringResource(R.string.correct),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .alpha(if (currentY < 0f) fraction else 0f)
                )
                if (allowSkip) {
                    Text(
                        text = stringResource(R.string.skip),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .alpha(if (currentY > 0f) fraction else 0f)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.correct),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .alpha(if (currentX > 0f) fraction else 0f)
                )
                if (allowSkip) {
                    Text(
                        text = stringResource(R.string.skip),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .alpha(if (currentX < 0f) fraction else 0f)
                    )
                }
            }
            if (metadataItems.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    metadataItems.forEach { (text, highlighted) ->
                        WordMetadataChip(text = text, highlighted = highlighted)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordMetadataChip(
    text: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (highlighted) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
