import UIKit
import FirebaseCore
import GoogleSignIn
import Shared

/// Google sign-in for the shared Kotlin code, which has no binding to the GoogleSignIn SDK.
/// Kotlin calls `signIn`; exactly one callback fires. Tokens go straight to Firebase and are never stored here.
final class GoogleSignInBridge: IosGoogleSignIn {
    func signIn(
        onSuccess: @escaping (String, String) -> Void,
        onCancel: @escaping () -> Void,
        onError: @escaping (String) -> Void
    ) {
        DispatchQueue.main.async {
            guard let clientID = FirebaseApp.app()?.options.clientID else {
                onError("Firebase is not configured")
                return
            }
            guard let presenter = Self.topViewController() else {
                onError("No window to present Google sign-in")
                return
            }
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
                if let error = error as NSError? {
                    if error.domain == kGIDSignInErrorDomain && error.code == GIDSignInError.canceled.rawValue {
                        onCancel()
                    } else {
                        onError(error.localizedDescription)
                    }
                    return
                }
                guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                    onError("Google returned no token")
                    return
                }
                onSuccess(idToken, user.accessToken.tokenString)
            }
        }
    }

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }
        var top = scenes.flatMap(\.windows).first(where: \.isKeyWindow)?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}
