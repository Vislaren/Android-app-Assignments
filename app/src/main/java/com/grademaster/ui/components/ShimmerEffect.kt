package com.grademaster.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grademaster.ui.theme.GradeMasterColors

// ─────────────────────────────────────────────────────────────────────────────
// Shimmer Brush — animated gradient brush for skeleton loading
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun shimmerBrush(isDark: Boolean = false): Brush {
    val shimmerColors = if (isDark) {
        listOf(
            GradeMasterColors.ShimmerBaseDark,
            GradeMasterColors.ShimmerHighlightDark,
            GradeMasterColors.ShimmerBaseDark
        )
    } else {
        listOf(
            GradeMasterColors.ShimmerBase,
            GradeMasterColors.ShimmerHighlight,
            GradeMasterColors.ShimmerBase
        )
    }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 200f, translateAnimation - 200f),
        end = Offset(translateAnimation, translateAnimation)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// ShimmerBox — a generic shimmer placeholder rectangle
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    width: Dp? = null,
    cornerRadius: Dp = 8.dp,
    isDark: Boolean = false
) {
    val brush = shimmerBrush(isDark)
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// StudentCardShimmer — skeleton placeholder for a single student card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StudentCardShimmer(isDark: Boolean = false) {
    val brush = shimmerBrush(isDark)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF2A2C2A) else Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(brush)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerBox(height = 14.dp, width = 140.dp, isDark = isDark)
            ShimmerBox(height = 12.dp, width = 200.dp, isDark = isDark)
        }
        // Grade badge shimmer
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(brush)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ShimmerList — a stack of ShimmerCard placeholders
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ShimmerList(count: Int = 6, isDark: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            StudentCardShimmer(isDark = isDark)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VaultSessionShimmer — skeleton for vault session cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun VaultSessionShimmer(isDark: Boolean = false) {
    val brush = shimmerBrush(isDark)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF2A2C2A) else Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerBox(height = 16.dp, width = 160.dp, isDark = isDark)
            ShimmerBox(height = 16.dp, width = 60.dp, isDark = isDark)
        }
        ShimmerBox(height = 12.dp, width = 220.dp, isDark = isDark)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isDark) Color(0xFF3A3A3A) else Color(0xFFDDDDDD))
                    )
                }
            }
        }
    }
}
