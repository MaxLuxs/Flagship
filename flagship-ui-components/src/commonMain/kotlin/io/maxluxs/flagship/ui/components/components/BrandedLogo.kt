package io.maxluxs.flagship.ui.components.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Flagship logo component.
 * 
 * Reusable component for all Flagship UI clients.
 * 
 * Currently uses text representation.
 * Can be replaced with SVG component from docs/images/flagship_icon.svg in the future.
 */
@Composable
fun BrandedLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Text(
        text = "🚩 Flagship",
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = size.value.sp,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

