package dev.themselg.jellymusic.data.session

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.Jellyfin
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.AuthenticateUserByName
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the authenticated Jellyfin connection.
 *
 * Responsibilities:
 *  - Owns one [Jellyfin] SDK instance (built with persistent device id + client info).
 *  - Persists credentials in [EncryptedSharedPreferences].
 *  - Exposes the current [JellyfinSession] as a [StateFlow], restored on construction.
 *  - Builds + exposes an authenticated [ApiClient] for repositories via [requireApi].
 *  - Implements [JellyfinUrls] for artwork + stream URLs.
 *
 * The [JellyfinUrls] surface is bound separately in Hilt (see DataModule) but backed by
 * this same singleton, so URL builders always reflect the live session.
 */
@Singleton
class SessionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SessionManager, JellyfinUrls {

    private val prefs: SharedPreferences = run {
        // NOTE: MasterKey.Builder + EncryptedSharedPreferences API per security-crypto 1.1.0-alpha.
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Stable per-install device id; generated once and persisted. */
    private val deviceId: String = prefs.getString(KEY_DEVICE_ID, null) ?: run {
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        generated
    }

    private val jellyfin: Jellyfin = createJellyfin {
        clientInfo = ClientInfo(name = "JellyMusic", version = "0.1.0")
        context = this@SessionManagerImpl.context
        deviceInfo = DeviceInfo(
            id = deviceId,
            name = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
        )
    }

    private val _session = MutableStateFlow(loadSession())
    override val session: StateFlow<JellyfinSession?> = _session.asStateFlow()

    /** Cached authenticated client; rebuilt whenever the session changes. */
    @Volatile
    private var apiClient: ApiClient? = _session.value?.let { buildApi(it) }

    private fun loadSession(): JellyfinSession? {
        val serverUrl = prefs.getString(KEY_SERVER_URL, null) ?: return null
        val userId = prefs.getString(KEY_USER_ID, null) ?: return null
        val userName = prefs.getString(KEY_USER_NAME, null) ?: return null
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        return JellyfinSession(
            serverUrl = serverUrl,
            userId = userId,
            userName = userName,
            accessToken = accessToken,
            deviceId = deviceId,
            serverName = prefs.getString(KEY_SERVER_NAME, null).orEmpty(),
        )
    }

    private fun buildApi(s: JellyfinSession): ApiClient =
        jellyfin.createApi(baseUrl = s.serverUrl, accessToken = s.accessToken)

    override suspend fun signIn(serverUrl: String, username: String, password: String) {
        withContext(Dispatchers.IO) {
            val normalized = normalizeUrl(serverUrl)
            val api = jellyfin.createApi(baseUrl = normalized)

            // Response<T> supports `by` delegation to unwrap `.content`.
            // NOTE: there is also a username/password overload of authenticateUserByName in
            // UserApiExtensions; we pass the AuthenticateUserByName payload explicitly.
            val result by api.userApi.authenticateUserByName(
                data = AuthenticateUserByName(username = username, pw = password),
            )

            val token = result.accessToken
                ?: error("Authentication succeeded but no access token was returned")
            val user = result.user
                ?: error("Authentication succeeded but no user was returned")

            // Attach the token so subsequent requests on this client are authenticated.
            // ApiClient.accessToken is read-only on the abstract type; mutate via update().
            api.update(accessToken = token)

            // Friendly server name (e.g. "Homeserver"). Best-effort: don't fail sign-in if it errors.
            val serverName = runCatching {
                val info by api.systemApi.getPublicSystemInfo()
                info.serverName.orEmpty()
            }.getOrDefault("")

            val newSession = JellyfinSession(
                serverUrl = normalized,
                userId = user.id.toString(),
                userName = user.name ?: username,
                accessToken = token,
                deviceId = deviceId,
                serverName = serverName,
            )
            persist(newSession)
            apiClient = api
            _session.value = newSession
        }
    }

    override fun signOut() {
        prefs.edit()
            .remove(KEY_SERVER_URL)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_SERVER_NAME)
            // Intentionally keep KEY_DEVICE_ID stable across sign-outs.
            .apply()
        apiClient = null
        _session.value = null
    }

    /** Authenticated client for repositories. Throws if no active session. */
    fun requireApi(): ApiClient =
        apiClient ?: throw IllegalStateException("Not signed in")

    /**
     * Current authenticated user id as a SDK [UUID]. Throws if no active session.
     * (The SDK's ApiClient does not track the user id, so repositories source it here.)
     */
    fun requireUserId(): UUID =
        UUID.fromString(_session.value?.userId ?: throw IllegalStateException("Not signed in"))

    // --- JellyfinUrls -------------------------------------------------------

    override fun imageUrl(itemId: String, maxWidth: Int): String? {
        val s = _session.value ?: return null
        return "${s.serverUrl}/Items/$itemId/Images/Primary?fillWidth=$maxWidth&quality=90"
    }

    override fun userImageUrl(maxWidth: Int): String? {
        val s = _session.value ?: return null
        return "${s.serverUrl}/Users/${s.userId}/Images/Primary?fillWidth=$maxWidth&quality=90"
    }

    override fun streamUrl(songId: String): String? {
        val s = _session.value ?: return null
        // Direct/static stream: returns the original file (mp3/flac/aac/ogg/opus/wav…),
        // which ExoPlayer's progressive extractors play without the HLS module. The
        // `universal` endpoint can return an HLS playlist when it decides to transcode,
        // which would need `media3-exoplayer-hls`; static keeps playback dependency-light.
        return "${s.serverUrl}/Audio/$songId/stream" +
            "?static=true" +
            "&api_key=${s.accessToken}" +
            "&deviceId=${s.deviceId}"
    }

    // --- internals ----------------------------------------------------------

    private fun persist(s: JellyfinSession) {
        prefs.edit()
            .putString(KEY_SERVER_URL, s.serverUrl)
            .putString(KEY_USER_ID, s.userId)
            .putString(KEY_USER_NAME, s.userName)
            .putString(KEY_ACCESS_TOKEN, s.accessToken)
            .putString(KEY_DEVICE_ID, s.deviceId)
            .putString(KEY_SERVER_NAME, s.serverName)
            .apply()
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        return when {
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "http://$trimmed"
        }
    }

    private companion object {
        const val PREFS_FILE = "jellyfin_session_secure"
        const val KEY_SERVER_URL = "server_url"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_NAME = "user_name"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_SERVER_NAME = "server_name"
    }
}
