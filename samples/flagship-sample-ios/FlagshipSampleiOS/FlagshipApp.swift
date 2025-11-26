import SwiftUI
import Flagship

@main
struct FlagshipSampleiOSApp: App {
    init() {
        initializeFlagship()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
    
    private func initializeFlagship() {
        // Create HTTP client for REST provider
        // Note: In real app, you'd use URLSession or Ktor client
        // For this sample, we'll use a simple REST provider setup
        
        // Configure Flagship with REST provider
        // The actual REST provider setup would be done via Kotlin/Native framework
        // This is a simplified example - in practice, you'd use the full FlagshipSwift API
        
        // For demo purposes, we'll show the structure
        // In a real implementation, you'd use:
        // FlagshipSwift.shared.quickConfigure(
        //     appKey: "sample-ios-app",
        //     environment: "production",
        //     providers: [...]
        // )
    }
}

