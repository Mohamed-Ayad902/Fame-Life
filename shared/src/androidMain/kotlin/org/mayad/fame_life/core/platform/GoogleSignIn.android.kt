package org.mayad.fame_life.core.platform

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import org.mayad.fame_life.domain.account.AuthErrorKind
import org.mayad.fame_life.domain.account.AuthException
import org.mayad.fame_life.domain.account.GoogleTokenProvider
import org.mayad.fame_life.domain.account.GoogleTokens

/**
 * Google sign-in through Credential Manager. The server client id is Firebase's web OAuth client, which the
 * google-services plugin writes to the app's `default_web_client_id` string from google-services.json.
 */
private class CredentialManagerGoogle : GoogleTokenProvider {
    override suspend fun requestTokens(): GoogleTokens {
        val activity = PulseAndroid.activity ?: throw AuthException(AuthErrorKind.UNKNOWN)
        val clientIdRes = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        if (clientIdRes == 0) throw AuthException(AuthErrorKind.NOT_CONFIGURED)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(activity.getString(clientIdRes)).build())
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw AuthException(AuthErrorKind.CANCELLED, e)
        } catch (e: NoCredentialException) {
            throw AuthException(AuthErrorKind.NO_GOOGLE_ACCOUNT, e)
        } catch (e: GetCredentialException) {
            throw AuthException(AuthErrorKind.UNKNOWN, e)
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw AuthException(AuthErrorKind.UNKNOWN)
        }
        return GoogleTokens(idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken)
    }
}

actual fun createGoogleTokenProvider(): GoogleTokenProvider = CredentialManagerGoogle()
