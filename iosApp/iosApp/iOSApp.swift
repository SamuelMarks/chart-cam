import SwiftUI
import ChartCamShared

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onChange(of: scenePhase) { phase in
                    let nowMs = Int64(Date().timeIntervalSince1970 * 1000)
                    switch phase {
                    case .active:
                        AppPrivacyManagerKt.currentAppPrivacyManager.onAppMovedToForeground(nowMs: nowMs)
                    case .inactive, .background:
                        AppPrivacyManagerKt.currentAppPrivacyManager.onAppMovedToBackground(nowMs: nowMs)
                    @unknown default:
                        break
                    }
                }
        }
    }
}
