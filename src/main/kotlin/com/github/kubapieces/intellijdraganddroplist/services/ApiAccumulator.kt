package com.github.kubapieces.intellijdraganddroplist.services

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

    fun doAsync(func: ApiAccumulator.() -> Any?) = CoroutineScope(CoroutineName("AnchorsProcessor") + Job())
        .launch(Dispatchers.IO) { kotlin.runCatching { func() } }
}