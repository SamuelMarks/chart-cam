import SwiftUI
import ChartCamShared

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onChange(of: scenePhase) { phase in
                    switch phase {
                    case .active:
                        AppPrivacyManagerKt.currentAppPrivacyManager.onAppMovedToForeground()
                    case .inactive, .background:
                        AppPrivacyManagerKt.currentAppPrivacyManager.onAppMovedToBackground()
                    @unknown default:
                        break
                    }
                }
        }
    }
}
