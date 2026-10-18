import SwiftUI
import FirebaseCore
import GoogleSignIn
import Shared

@main
struct iOSApp: App {
    init() {
        // Firebase reads GoogleService-Info.plist from the app bundle. Without it, configure() would crash, so the
        // app starts without accounts and the sign-in step explains that this build isn't set up yet.
        if Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil {
            FirebaseApp.configure()
            PulseIos.shared.googleSignIn = GoogleSignInBridge()
        } else {
            print("Pulse: GoogleService-Info.plist is missing; accounts are unavailable in this build.")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                // Google sign-in finishes by reopening the app through the reversed client id URL scheme.
                .onOpenURL { url in GIDSignIn.sharedInstance.handle(url) }
        }
    }
}
