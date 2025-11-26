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
 * Компонент логотипа Flagship.
 * 
 * Переиспользуемый компонент для всех Flagship UI клиентов.
 * 
 * Пока использует текстовое представление.
 * В будущем можно заменить на SVG компонент из docs/images/flagship_icon.svg
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

