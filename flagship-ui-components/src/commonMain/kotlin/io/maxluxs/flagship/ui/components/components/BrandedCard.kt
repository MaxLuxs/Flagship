package io.maxluxs.flagship.ui.components.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Branded card in unified Flagship style.
 * 
 * Reusable component for all Flagship UI clients.
 * 
 * Uses:
 * - Rounded corners
 * - Shadow for elevation
 * - Brand colors
 */
@Composable
fun BrandedCard(
    modifier: Modifier = Modifier,
    elevation: androidx.compose.material3.CardElevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = elevation,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.padding(contentPadding)
        ) {
            content()
        }
    }
}

