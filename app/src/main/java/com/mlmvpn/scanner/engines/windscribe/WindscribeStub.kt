package com.mlmvpn.scanner.engines.windscribe

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * OilVPN stub. The Windscribe engine (its SDK, `com.wsnet.lib`) was not shipped in the
 * public source release and cannot be rebuilt here, so this stub keeps the API surface
 * every other file references — the engine reports signed-out and never connects.
 * The Windscribe tile is hidden (see ScreenVisibility/HomeDestinations edits).
 */
enum class WsPhase { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING }

data class WsConnection(
    val phase: WsPhase = WsPhase.DISCONNECTED,
    val testing: Boolean = false,
    val active: Boolean = false,
    val since: Long = 0L,
)

object WindscribeEngine {
    const val PROFILE_PREFIX = "ws-"
    const val NODE_ID = "windscribe"

    private val _connection = MutableStateFlow(WsConnection())
    val connection: StateFlow<WsConnection> = _connection

    @JvmStatic fun signedIn(context: Context): Boolean = false

    @JvmStatic fun wireGuardIds(context: Context): Map<String, String> = emptyMap()

    private val _data = MutableStateFlow(WsData())
    val data: StateFlow<WsData> = _data

    @JvmStatic fun init(context: Context) { /* stub: no SDK */ }

    @JvmStatic fun selectServer(id: String) { /* stub: no SDK */ }

    @JvmStatic fun refresh() { /* stub: no SDK */ }

    @JvmStatic fun connect(context: Context) { /* stub: no SDK */ }

    @JvmStatic fun disconnect() { _connection.value = WsConnection() }
}

/** Server record shape the widget paths read; always empty in the stub. */
data class WsServer(
    val id: String = "",
    val country: String? = null,
    val load: Int? = null,
)

data class WsData(
    val servers: List<WsServer> = emptyList(),
    val selectedServer: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

/** Outline's shared-WARP test session: nothing to close in the stub. */
object WsWarpCarrier {
    @JvmStatic fun closeShared() { /* stub */ }
}

/**
 * Windscribe's WsProxy client stub. Real one runs its Go proxy over the profile's
 * hop; here `start` opens nothing — a Windscribe profile cannot exist anyway
 * because sign-in is impossible without the SDK.
 */
class WsProxyClient(
    context: android.content.Context,
    onError: () -> Unit,
) {
    fun start(wsProxy: com.mlmvpn.scanner.openvpn.WsProxy?): Int = -1

    fun close() { /* stub */ }
}
