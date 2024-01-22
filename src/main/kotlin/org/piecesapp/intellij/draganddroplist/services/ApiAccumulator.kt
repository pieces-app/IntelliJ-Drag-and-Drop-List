package org.piecesapp.intellij.draganddroplist.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.util.SystemInfo
import kotlinx.coroutines.*
import org.piecesapp.client.apis.AssetApi
import org.piecesapp.client.apis.AssetsApi
import org.piecesapp.client.apis.ConnectorApi
import org.piecesapp.client.models.*

@Service
class ApiAccumulator {
    private val address = "http://localhost:" + if (SystemInfo.isLinux) "5323" else "1000"
    private var _context: Context? = null
    val context get() = _context
    val assets = AssetsApi(address)
    val asset = AssetApi(address)
    val connector = ConnectorApi(address)

    /**
     * This function establishes a connection using the SeededConnectorConnection.
     * It checks the current system's platform (Linux, MacOS, Windows, or Unknown)
     * and creates a SeededTrackedApplication with the corresponding platform.
     * If the _context is null or the force parameter is set to true,
     * it will force a new connection regardless of the current _context state.
     *
     * @param force a Boolean value that determines whether to force a new connection.
     */
    fun connect(force: Boolean = false) {
        if (_context == null || force)
            _context = connector.connect(SeededConnectorConnection(
                SeededTrackedApplication(
                    ApplicationNameEnum.uNKNOWN,
                    "unknown",
                    when {
                        SystemInfo.isLinux -> PlatformEnum.lINUX
                        SystemInfo.isMac -> PlatformEnum.mACOS
                        SystemInfo.isWindows -> PlatformEnum.wINDOWS
                        else -> PlatformEnum.uNKNOWN
                    }
                )
            ))
    }

    /**
     * This function launches a new coroutine on the IO dispatcher and executes the provided function within a try-catch block.
     * The function is an extension function on the ApiAccumulator class and can return any type of result.
     * The coroutine is named "PiecesAPICall" and is not tied to any lifecycle, so it needs to be managed properly to avoid memory leaks.
     *
     * @param func The function to be executed asynchronously. This function is an extension function on the ApiAccumulator class.
     * @return The coroutine job that is executing the function. This job can be used to cancel the execution if needed.
     */
    fun doAsync(func: ApiAccumulator.() -> Any?) = CoroutineScope(CoroutineName("PiecesAPICall") + Job())
        .launch(Dispatchers.IO) { kotlin.runCatching { func() } }
}