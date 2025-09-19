package com.example.alias.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.alias.R
import kotlin.math.roundToInt

@Composable
fun TutorialOverlay(
    verticalMode: Boolean,
    onDismiss: () -> Unit,
    cardBounds: Rect? = null,
    modifier: Modifier = Modifier,
) {
    val swipeBody = if (verticalMode) {
        stringResource(R.string.tutorial_instructions_vertical)
    } else {
        stringResource(R.string.tutorial_instructions)
    }
    val steps = listOf(
        TutorialStep(
            title = stringResource(R.string.tutorial_step_swipe_title),
            body = swipeBody,
            focus = TutorialFocus.Card,
            extraContent = { SwipeIllustration(vertical = verticalMode) }
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step_status_title),
            body = stringResource(R.string.tutorial_step_status_body),
            focus = TutorialFocus.Status
        ),
        TutorialStep(
            title = stringResource(R.string.tutorial_step_actions_title),
            body = stringResource(R.string.tutorial_step_actions_body),
            focus = TutorialFocus.Controls
        )
    )
    var stepIndex by rememberSaveable { mutableStateOf(0) }
    val step = steps[stepIndex]
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = interactionSource, indication = null) { }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val containerSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
            val focusRect = step.focus.calculateRect(containerSize, cardBounds)
            val density = LocalDensity.current
            val scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawRect(scrimColor)
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = focusRect.topLeft,
                        size = focusRect.size,
                        cornerRadius = CornerRadius(32.dp.toPx()),
                        blendMode = BlendMode.Clear
                    )
                }
            }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(focusRect.left.roundToInt(), focusRect.top.roundToInt())
                    }
                    .size(
                        width = with(density) { focusRect.width.toDp() },
                        height = with(density) { focusRect.height.toDp() }
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Transparent, RectangleShape)
                    .border(
                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                        RoundedCornerShape(32.dp)
                    )
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(stringResource(R.string.tutorial_skip))
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                shape = RoundedCornerShape(28.dp),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(steps.size) { index ->
                            val active = index == stepIndex
                            Box(
                                modifier = Modifier
                                    .size(if (active) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        }
                                    )
                            )
                        }
                    }
                    Text(step.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(
                        step.body,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    step.extraContent?.invoke()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { stepIndex-- },
                            enabled = stepIndex > 0
                        ) {
                            Text(stringResource(R.string.tutorial_back))
                        }
                        Button(
                            onClick = {
                                if (stepIndex == steps.lastIndex) {
                                    onDismiss()
                                } else {
                                    stepIndex++
                                }
                            }
                        ) {
                            Text(
                                if (stepIndex == steps.lastIndex) {
                                    stringResource(R.string.tutorial_finish)
                                } else {
                                    stringResource(R.string.tutorial_next)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeIllustration(vertical: Boolean) {
    val arrowStart: ImageVector
    val arrowEnd: ImageVector
    if (vertical) {
        arrowStart = Icons.Filled.KeyboardArrowUp
        arrowEnd = Icons.Filled.KeyboardArrowDown
    } else {
        arrowStart = Icons.AutoMirrored.Filled.KeyboardArrowLeft
        arrowEnd = Icons.AutoMirrored.Filled.KeyboardArrowRight
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(arrowStart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Icon(Icons.Filled.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Icon(arrowEnd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

private data class TutorialStep(
    val title: String,
    val body: String,
    val focus: TutorialFocus,
    val extraContent: (@Composable () -> Unit)? = null,
)

private sealed interface TutorialFocus {
    fun calculateRect(containerSize: Size, cardBounds: Rect?): Rect

    object Card : TutorialFocus {
        override fun calculateRect(containerSize: Size, cardBounds: Rect?): Rect {
            val measuredRect = cardBounds?.let { rect ->
                val left = rect.left.coerceIn(0f, containerSize.width)
                val top = rect.top.coerceIn(0f, containerSize.height)
                val right = rect.right.coerceIn(left, containerSize.width)
                val bottom = rect.bottom.coerceIn(top, containerSize.height)
                Rect(left, top, right, bottom)
            }
            return measuredRect ?: containerSize.rectangle(
                widthFraction = 0.78f,
                heightFraction = 0.38f,
                topFraction = 0.26f
            )
        }
    }

    object Status : TutorialFocus {
        @Suppress("UNUSED_PARAMETER")
        override fun calculateRect(containerSize: Size, cardBounds: Rect?): Rect = containerSize.rectangle(
            widthFraction = 0.9f,
            heightFraction = 0.2f,
            topFraction = 0.08f
        )
    }

    object Controls : TutorialFocus {
        @Suppress("UNUSED_PARAMETER")
        override fun calculateRect(containerSize: Size, cardBounds: Rect?): Rect = containerSize.rectangle(
            widthFraction = 0.9f,
            heightFraction = 0.22f,
            topFraction = 0.68f
        )
    }
}

private fun Size.rectangle(
    widthFraction: Float,
    heightFraction: Float,
    topFraction: Float,
): Rect {
    val width = width * widthFraction
    val height = height * heightFraction
    val clampedWidth = width.coerceIn(0f, this.width)
    val clampedHeight = height.coerceIn(0f, this.height)
    val left = ((this.width - clampedWidth) / 2f).coerceIn(0f, this.width - clampedWidth)
    val top = (this.height * topFraction).coerceIn(0f, this.height - clampedHeight)
    return Rect(Offset(left, top), Size(clampedWidth, clampedHeight))
}
