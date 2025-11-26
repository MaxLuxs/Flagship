package io.maxluxs.flagship.sampleandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.maxluxs.flagship.core.Flagship
import kotlinx.coroutines.launch

/**
 * Пример использования сгенерированных классов из flagship-codegen с Firebase.
 * 
 * Этот пример показывает, как использовать codegen в Android приложении с Firebase Remote Config.
 * 
 * После запуска ./gradlew generateFlags, используйте:
 * ```kotlin
 * import io.maxluxs.flagship.generated.Flags
 * 
 * lifecycleScope.launch {
 *     // Boolean flag
 *     if (Flags.NewUi.enabled()) {
 *         showNewUI()
 *     }
 *     
 *     // Typed values
 *     val timeout = Flags.ApiTimeout.value()
 *     val message = Flags.WelcomeMessage.value()
 *     
 *     // Experiments
 *     val variant = Flags.CheckoutFlow.variant()
 *     when (variant) {
 *         Flags.CheckoutFlow.Variant.CONTROL -> showLegacyCheckout()
 *         Flags.CheckoutFlow.Variant.A -> showNewCheckout()
 *         Flags.CheckoutFlow.Variant.B -> showAlternativeCheckout()
 *     }
 * }
 * ```
 */
class CodegenExampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                CodegenExampleScreen()
            }
        }
    }
}

@Composable
fun CodegenExampleScreen() {
    var newUiEnabled by remember { mutableStateOf(false) }
    var apiTimeout by remember { mutableStateOf(5000) }
    var welcomeMessage by remember { mutableStateOf("Welcome!") }
    var checkoutVariant by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Загружаем флаги при первом рендере
    LaunchedEffect(Unit) {
        val activity = androidx.compose.ui.platform.LocalContext.current as? ComponentActivity
        activity?.lifecycleScope?.launch {
            // Используем сгенерированные классы из codegen
            // После запуска ./gradlew :flagship-sample-android:generateFlags
            try {
                // Boolean flag
                newUiEnabled = io.maxluxs.flagship.generated.Flags.NewUi.enabled()
                
                // Typed values
                apiTimeout = io.maxluxs.flagship.generated.Flags.ApiTimeout.value()
                welcomeMessage = io.maxluxs.flagship.generated.Flags.WelcomeMessage.value()
                
                // Experiments
                checkoutVariant = io.maxluxs.flagship.generated.Flags.CheckoutFlow.variant()
            } catch (e: Exception) {
                // Fallback to direct API if generated classes not available
                newUiEnabled = Flagship.isEnabled("new_ui", default = false)
                apiTimeout = Flagship.intValue("api_timeout", default = 5000)
                welcomeMessage = Flagship.stringValue("welcome_message", default = "Welcome!")
                
                val checkoutAssignment = Flagship.assign("checkout_flow")
                checkoutVariant = checkoutAssignment?.variant
            }
            
            isLoading = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Codegen Example with Firebase",
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "Пример использования сгенерированных классов из flagship-codegen с Firebase Remote Config",
            style = MaterialTheme.typography.bodyMedium
        )
        
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            // Boolean flag example
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Boolean Flag: new_ui",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enabled: $newUiEnabled",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Code: Flags.NewUi.enabled()",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Typed values example
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Typed Values",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "API Timeout: $apiTimeout ms",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Code: Flags.ApiTimeout.value()",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Welcome Message: $welcomeMessage",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Code: Flags.WelcomeMessage.value()",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Experiments example
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Experiments",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Checkout Flow: ${checkoutVariant ?: "Not assigned"}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Code: Flags.CheckoutFlow.variant()",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Как использовать:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Убедитесь, что flags.json существует",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. Запустите: ./gradlew generateFlags",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "3. Импортируйте: import io.maxluxs.flagship.generated.Flags",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "4. Используйте: Flags.NewUi.enabled()",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

