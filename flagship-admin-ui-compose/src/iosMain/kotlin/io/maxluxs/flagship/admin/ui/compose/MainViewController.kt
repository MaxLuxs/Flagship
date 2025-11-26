package io.maxluxs.flagship.admin.ui.compose

import androidx.compose.ui.window.ComposeUIViewController

/**
 * iOS entry point for Flagship Admin Panel.
 * 
 * This function creates a UIViewController that hosts the Compose UI.
 * It's called from SwiftUI code in the iOS app.
 * 
 * Usage in Swift:
 * ```swift
 * import ComposeApp
 * 
 * struct ContentView: View {
 *     var body: some View {
 *         ComposeView()
 *             .ignoresSafeArea()
 *     }
 * }
 * 
 * struct ComposeView: UIViewControllerRepresentable {
 *     func makeUIViewController(context: Context) -> UIViewController {
 *         MainViewControllerKt.MainViewController()
 *     }
 *     func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
 * }
 * ```
 */
fun MainViewController() = ComposeUIViewController { 
    // Use localhost for iOS simulator, or configure API URL
    val apiBaseUrl = "http://localhost:8080"
    AdminApp(apiBaseUrl = apiBaseUrl)
}

