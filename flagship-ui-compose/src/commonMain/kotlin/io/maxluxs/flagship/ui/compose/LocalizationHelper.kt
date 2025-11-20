package io.maxluxs.flagship.ui.compose

import androidx.compose.runtime.Composable

/**
 * Helper object for accessing localized strings in Flagship UI.
 * Supports English (default) and Russian.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     Text(LocalizationHelper.flagsTitle)
 * }
 * ```
 */
object LocalizationHelper {
    @Composable
    fun flagsTitle() = "Flags"
    
    @Composable
    fun flagsSearch() = "Search flags..."
    
    @Composable
    fun flagsNoResults() = "No flags found"
    
    @Composable
    fun flagsLoading() = "Loading..."
    
    @Composable
    fun flagEnabled() = "Enabled"
    
    @Composable
    fun flagDisabled() = "Disabled"
    
    @Composable
    fun flagSource(source: String) = "Source: $source"
    
    @Composable
    fun overridesTitle() = "Overrides"
    
    @Composable
    fun overridesNoItems() = "No overrides"
    
    @Composable
    fun overridesAdd() = "Add Override"
    
    @Composable
    fun diagnosticsTitle() = "Diagnostics"
    
    @Composable
    fun settingsTitle() = "Settings"
    
    @Composable
    fun settingsRefresh() = "Refresh"
    
    @Composable
    fun commonRefresh() = "Refresh"
    
    @Composable
    fun commonClose() = "Close"
    
    @Composable
    fun commonSave() = "Save"
    
    @Composable
    fun commonCancel() = "Cancel"
}

