import SwiftUI
import Flagship

struct ContentView: View {
    @State private var flags: [String: Bool] = [:]
    @State private var isLoading = true
    @State private var error: String?
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                if isLoading {
                    ProgressView("Loading flags...")
                } else if let error = error {
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 50))
                            .foregroundColor(.orange)
                        Text("Error")
                            .font(.headline)
                        Text(error)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .padding()
                } else {
                    FlagsListView(flags: flags)
                }
            }
            .navigationTitle("🚩 Flagship iOS Sample")
            .task {
                await loadFlags()
            }
        }
    }
    
    private func loadFlags() async {
        isLoading = true
        defer { isLoading = false }
        
        do {
            // Example: Check some flags
            // In real implementation, you'd use FlagshipSwift.shared.manager
            let manager = FlagshipSwift.shared.manager
            
            // Get all flags (this would be implemented in the framework)
            // For now, we'll show a placeholder structure
            flags = [
                "new_feature": await manager.isEnabled(key: "new_feature", default: false),
                "dark_mode": await manager.isEnabled(key: "dark_mode", default: false),
                "premium_features": await manager.isEnabled(key: "premium_features", default: false)
            ]
        } catch {
            self.error = error.localizedDescription
        }
    }
}

struct FlagsListView: View {
    let flags: [String: Bool]
    
    var body: some View {
        List {
            ForEach(Array(flags.keys.sorted()), id: \.self) { key in
                HStack {
                    Text(key)
                        .font(.body)
                    Spacer()
                    if let value = flags[key] {
                        Image(systemName: value ? "checkmark.circle.fill" : "xmark.circle.fill")
                            .foregroundColor(value ? .green : .red)
                    }
                }
            }
        }
    }
}

#Preview {
    ContentView()
}

