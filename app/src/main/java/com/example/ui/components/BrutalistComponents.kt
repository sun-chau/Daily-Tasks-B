package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ImportanceLevel
import com.example.ui.theme.BrutalistAmber
import com.example.ui.theme.BrutalistBlack
import com.example.ui.theme.BrutalistCyan
import com.example.ui.theme.BrutalistLime
import com.example.ui.theme.BrutalistRed
import com.example.ui.theme.BrutalistWhite
import com.example.ui.theme.BrutalistYellow
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utilitarian Brutalist Card with thick solid border and hard offset zero-blur shadow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrutalistCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shadowColor: Color = MaterialTheme.colorScheme.outline,
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        // Hard drop shadow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(shadowColor, RoundedCornerShape(0.dp))
        )
        // Primary surface layer
        val surfaceModifier = if (onClick != null || onLongClick != null) {
            Modifier
                .background(backgroundColor, RoundedCornerShape(0.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(0.dp))
                .combinedClickable(
                    onClick = { onClick?.invoke() },
                    onLongClick = { onLongClick?.invoke() }
                )
        } else {
            Modifier
                .background(backgroundColor, RoundedCornerShape(0.dp))
                .border(borderWidth, borderColor, RoundedCornerShape(0.dp))
        }

        Box(
            modifier = surfaceModifier,
            content = content
        )
    }
}

/**
 * Chunky Brutalist Button with physical offset push effect.
 */
@Composable
fun BrutalistButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shadowOffset: Dp = 3.5.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOffset = if (isPressed) 1.dp else shadowOffset

    Box(modifier = modifier) {
        // Shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(borderColor, RoundedCornerShape(0.dp))
        )
        // Main Button Surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = if (isPressed) 2.dp else 0.dp, y = if (isPressed) 2.dp else 0.dp)
                .background(backgroundColor, RoundedCornerShape(0.dp))
                .border(2.5.dp, borderColor, RoundedCornerShape(0.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        com.example.util.FeedbackManager.performClickFeedback(context)
                        onClick()
                    }
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}

/**
 * Brutalist Checkbox with heavy border and crisp tactile fill.
 */
@Composable
fun BrutalistCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    checkedColor: Color = BrutalistYellow
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(28.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Checkbox
            ) {
                if (!checked) {
                    com.example.util.FeedbackManager.performTaskCompletedFeedback(context)
                } else {
                    com.example.util.FeedbackManager.performClickFeedback(context)
                }
                onCheckedChange(!checked)
            }
    ) {
        // Hard shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(2.dp, 2.dp)
                .background(borderColor)
        )
        // Checkbox box
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(if (checked) checkedColor else MaterialTheme.colorScheme.surface)
                .border(2.5.dp, borderColor),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = BrutalistBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Brutalist Tag / Badge with distinct colors.
 */
@Composable
fun BrutalistBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = BrutalistBlack,
    textColor: Color = BrutalistWhite,
    borderColor: Color = MaterialTheme.colorScheme.outline
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(0.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(0.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = text.uppercase(Locale.getDefault()),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Helper to display importance tag with distinct brutalist colors.
 */
@Composable
fun ImportanceBadge(
    importance: String,
    modifier: Modifier = Modifier
) {
    val isVeryImportant = importance == ImportanceLevel.VERY_IMPORTANT.name

    val (bg, fg) = when (importance) {
        ImportanceLevel.VERY_IMPORTANT.name -> BrutalistRed to BrutalistWhite
        ImportanceLevel.MEDIUM.name -> BrutalistAmber to BrutalistBlack
        ImportanceLevel.LOW.name -> BrutalistCyan to BrutalistBlack
        else -> BrutalistYellow to BrutalistBlack
    }

    val label = when (importance) {
        ImportanceLevel.VERY_IMPORTANT.name -> "★ VERY IMPORTANT"
        ImportanceLevel.MEDIUM.name -> "MEDIUM"
        ImportanceLevel.LOW.name -> "LOW"
        else -> importance
    }

    BrutalistBadge(
        text = label,
        backgroundColor = bg,
        textColor = fg,
        borderColor = if (isVeryImportant) BrutalistWhite else MaterialTheme.colorScheme.outline,
        modifier = modifier
    )
}

/**
 * Live chronometer countdown timer ticking every second.
 */
@Composable
fun ChronometerDisplay(
    targetEpochMillis: Long,
    modifier: Modifier = Modifier,
    isCompleted: Boolean = false
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(key1 = targetEpochMillis, key2 = isCompleted) {
        if (!isCompleted) {
            while (true) {
                currentTime = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val diffMillis = targetEpochMillis - currentTime
    val isOverdue = diffMillis < 0 && !isCompleted

    val totalSeconds = Math.abs(diffMillis) / 1000
    val days = totalSeconds / (24 * 3600)
    val hours = (totalSeconds % (24 * 3600)) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val timeFormatted = if (days > 0) {
        String.format(Locale.getDefault(), "%02dd %02dh %02dm %02ds", days, hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02dh %02dm %02ds", hours, minutes, seconds)
    }

    val badgeColor = when {
        isCompleted -> BrutalistLime
        isOverdue -> BrutalistRed
        days == 0L && hours < 2 -> BrutalistAmber
        else -> BrutalistCyan
    }

    val textColor = if (badgeColor == BrutalistRed) BrutalistWhite else BrutalistBlack

    Row(
        modifier = modifier
            .background(badgeColor)
            .border(1.5.dp, MaterialTheme.colorScheme.outline)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isCompleted) "[COMPLETED]" else if (isOverdue) "[OVERDUE: $timeFormatted]" else "[TIME LEFT: $timeFormatted]",
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
