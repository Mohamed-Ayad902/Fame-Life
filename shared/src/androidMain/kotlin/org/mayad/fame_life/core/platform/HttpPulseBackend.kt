package org.mayad.fame_life.core.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import org.mayad.fame_life.data.remote.BackendException
import org.mayad.fame_life.data.remote.PulseBackend
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** [PulseBackend] over HttpURLConnection, so the app needs no HTTP library. */
internal class HttpPulseBackend(
    baseUrl: String,
    private val idToken: suspend (forceRefresh: Boolean) -> String?,
) : PulseBackend {
    private val base = baseUrl.trimEnd('/')

    override suspend fun post(path: String, body: ByteArray, contentType: String): String {
        var response = exchange(path, body, contentType, idToken(false))
        // A token can be revoked or rotated before its expiry; one forced refresh covers that.
        if (response.first == 401) response = exchange(path, body, contentType, idToken(true))
        val (status, text) = response
        if (status !in 200..299) throw BackendException(status)
        return text
    }

    /**
     * Runs the blocking request on the IO pool. Cancelling the caller (the live session's timeout) disconnects the
     * socket, so a hung request can't hold the session past its deadline.
     */
    private suspend fun exchange(path: String, body: ByteArray, contentType: String, token: String?): Pair<Int, String> {
        if (token == null) return 401 to ""
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 5_000
            readTimeout = 20_000
            doOutput = true
            setFixedLengthStreamingMode(body.size)
            setRequestProperty("Content-Type", contentType)
            setRequestProperty("Authorization", "Bearer $token")
        }
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { connection.disconnect() }
            Dispatchers.IO.asExecutor().execute {
                try {
                    connection.outputStream.use { it.write(body) }
                    val status = connection.responseCode
                    val stream = if (status < 400) connection.inputStream else connection.errorStream
                    val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    continuation.resume(status to text)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                } finally {
                    connection.disconnect()
                }
            }
        }
    }
}
