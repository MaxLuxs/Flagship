package io.maxluxs.flagship.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.maxluxs.flagship.core.Flagship
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestProviderScreen(
    providerType: ProviderType,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val manager = Flagship.manager()

    var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test ${providerType.displayName}") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Provider Test Suite",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                "Testing integration with ${providerType.displayName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        testResults = runProviderTests(manager, providerType)
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Testing..." else "Run All Tests")
            }

            if (testResults.isNotEmpty()) {
                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                Text(
                    "Results:",
                    style = MaterialTheme.typography.titleMedium
                )

                testResults.forEach { result ->
                    TestResultCard(result)
                }

                val passed = testResults.count { it.passed }
                val total = testResults.size

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (passed == total) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Summary: $passed/$total tests passed",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home")
            }
        }
    }
}

@Composable
fun TestResultCard(result: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.passed) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${if (result.passed) "✅" else "❌"} ${result.name}",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                result.message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

data class TestResult(
    val name: String,
    val passed: Boolean,
    val message: String
)

suspend fun runProviderTests(
    manager: io.maxluxs.flagship.core.manager.FlagsManager,
    providerType: ProviderType
): List<TestResult> {
    val results = mutableListOf<TestResult>()

    // Test 1: Refresh
    try {
        manager.refresh()
        results.add(
            TestResult(
                "Provider Refresh",
                true,
                "Successfully refreshed from ${providerType.displayName}"
            )
        )
    } catch (e: Exception) {
        results.add(
            TestResult(
                "Provider Refresh",
                false,
                "Failed: ${e.message}"
            )
        )
    }

    // Test 2: Boolean flag
    try {
        val value = manager.isEnabled("new_feature", default = false)
        results.add(
            TestResult(
                "Boolean Flag (new_feature)",
                true,
                "Value: $value"
            )
        )
    } catch (e: Exception) {
        results.add(
            TestResult(
                "Boolean Flag",
                false,
                "Failed: ${e.message}"
            )
        )
    }

    // Test 3: Integer value
    try {
        val value = manager.value("max_retries", 0)
        results.add(
            TestResult(
                "Integer Value (max_retries)",
                true,
                "Value: $value"
            )
        )
    } catch (e: Exception) {
        results.add(
            TestResult(
                "Integer Value",
                false,
                "Failed: ${e.message}"
            )
        )
    }

    // Test 4: Double value
    try {
        val value = manager.value("api_timeout", 0.0)
        results.add(
            TestResult(
                "Double Value (api_timeout)",
                true,
                "Value: $value"
            )
        )
    } catch (e: Exception) {
        results.add(
            TestResult(
                "Double Value",
                false,
                "Failed: ${e.message}"
            )
        )
    }

    // Test 5: String value
    try {
        val value = manager.value("welcome_message", "")
        results.add(
            TestResult(
                "String Value (welcome_message)",
                true,
                "Value: $value"
            )
        )
    } catch (e: Exception) {
        results.add(
            TestResult(
                "String Value",
                false,
                "Failed: ${e.message}"
            )
        )
    }

    // Test 6: Experiment assignment
    try {
        val assignment = manager.assign("test_experiment")
        results.add(
            TestResult(
                "Experiment Assignment",
                assignment != null,
                "Variant: ${assignment?.variant ?: "not assigned"}"
            )
        )
    } catch (e: Exception) {
        results.add(
            TestResult(
                "Experiment Assignment",
                false,
                "Failed: ${e.message}"
            )
        )
    }

    return results
}


