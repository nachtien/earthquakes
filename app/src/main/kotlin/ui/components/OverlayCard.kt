package com.achtien.codingtemplate.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.achtien.codingtemplate.ui.theme.CardBackground
import com.achtien.codingtemplate.ui.theme.CardShape
import com.achtien.codingtemplate.ui.theme.MagnitudeRed
import com.achtien.codingtemplate.ui.theme.OverlayCardMargin

/** The dark rounded card that floats over a map. */
@Composable
fun OverlayCard(
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) = Card(
    shape = CardShape,
    colors = CardDefaults.cardColors(containerColor = CardBackground, contentColor = Color.White),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
    modifier = modifier.padding(OverlayCardMargin),
) {
    if (dragHandle != null) Box(Modifier.align(Alignment.CenterHorizontally)) { dragHandle() }
    content()
}

@Composable
fun MagnitudeBadge(
    mag: Double?,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fontSize: TextUnit = 16.sp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(MagnitudeRed, CircleShape),
    ) {
        Text(
            text = mag?.let { "%.2f".format(it) } ?: "–",
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
