import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        FlagshipIOSInitializer.shared.initialize()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}