package com.decathlon.smartnutristock.presentation.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.decathlon.smartnutristock.presentation.ui.theme.ShadowColor

/**
 * ShimmerLoading - Shimmer effect for loading states during Supabase sync.
 *
 * Infinite transition animation with gradient sweep using MaterialTheme colors.
 * Animation duration: 1500ms.
 */
@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "shimmer_alpha"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            Color.White.copy(alpha = 0.3f),
            MaterialTheme.colorScheme.surfaceVariant
        ),
        start = Offset(x = 0f, y = 0f),
        end = Offset(x = Float.POSITIVE_INFINITY, y = 0f)
    )

    Box(
        modifier = modifier
            .background(shimmerBrush)
            .fillMaxWidth()
    )
}

/**
 * ShimmerCard - Shimmer placeholder that mimics NutriCard dimensions.
 *
 * Used as a loading placeholder while data loads from Supabase.
 */
@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        ShimmerLoading(modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Preview for ShimmerLoading component
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun ShimmerLoadingPreview() {
    androidx.compose.material3.MaterialTheme {
        ShimmerCard()
    }
}

/**
 * Preview for multiple shimmer cards (list loading state)
 */
@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFFF8FAFC
)
@Composable
fun ShimmerCardListPreview() {
    androidx.compose.material3.Surface {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                ShimmerCard()
            }
        }
    }
}
