package com.dajakov.daedaluslink

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow

class WebSocketManager {
    private val client: OkHttpClient = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var resendDelay: Long = 1000L
    private var lastCommand: String? = null
    private var lastCommandTimestamp: Long = 0
    private var resendJob: Job? = null
    private var reconnectAttempts = 0
    private val isReconnecting = AtomicBoolean(false)

    suspend fun connectToWebSocket(url: String, sharedState: SharedState, heartbeatFrequency: Long,
                                   debugViewModel: DebugViewModel): Boolean {
        val request = Request.Builder().url(url).build()
        val connectionResult = CompletableDeferred<Boolean>()
        resendDelay = ((1F / heartbeatFrequency.toFloat()) * 1000F).toLong()

        lastCommand = null

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                sharedState.isConnected = true
                reconnectAttempts = 0  // Reset reconnect attempts on success
                isReconnecting.set(false)
                connectionResult.complete(true)
                startResendChecker(sharedState)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                sharedState.receivedMessages = (sharedState.receivedMessages + text).takeLast(100)
                processReceivedMessage(text, sharedState, debugViewModel)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                sharedState.isConnected = false
                connectionResult.complete(false)
                if (!isReconnecting.get()) reconnectWebSocket(url, sharedState)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                sharedState.isConnected = false
                webSocket.close(code, reason)
            }
        })

        return withTimeoutOrNull(5000) { connectionResult.await() } ?: false
    }

    private fun reconnectWebSocket(url: String, sharedState: SharedState) {
        if (isReconnecting.getAndSet(true)) return

        val delayTime = (5000L * 2.0.pow(reconnectAttempts.toDouble())).toLong().coerceAtMost(60000L)
        reconnectAttempts++
        Handler(Looper.getMainLooper()).postDelayed({
            CoroutineScope(Dispatchers.IO).launch {
                connectToWebSocket(url, sharedState, 1L, DebugViewModel())
            }
        }, delayTime)
    }

    fun disconnect() {
        resendJob?.cancel()
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
    }

    fun sendMovementCommand(x: Byte, y: Byte) {
        sendCommand("move $x,$y")
    }

    fun sendCommandToRobot(command: String) {
        sendCommand(command)
    }

    fun sendCommand(command: String) {
        webSocket?.send(command)?.let {
            updateLastCommand(command)
        } ?: println("Failed to send command: WebSocket is not initialized")
    }

    private fun updateLastCommand(command: String) {
        lastCommand = command
        lastCommandTimestamp = System.currentTimeMillis()
    }

    private fun startResendChecker(sharedState: SharedState) {
        resendJob?.cancel()
        resendJob = CoroutineScope(Dispatchers.IO).launch {
            while (sharedState.isConnected) {
                delay(resendDelay)
                val currentTime = System.currentTimeMillis()
                if (lastCommand != null && (currentTime - lastCommandTimestamp > resendDelay)) {
                    sendCommand(lastCommand!!)
                }
            }
        }
    }

    private fun processReceivedMessage(
        message: String,
        sharedState: SharedState,
        debugViewModel: DebugViewModel
    ) {
        try {
            val json = JSONObject(message)
            val type = json.getString("type")
            val payload = json.get("payload")

            when (type) {
                "config" -> {
                    sharedState.receivedJsonData = payload.toString()
                    sharedState.isJsonReceived = true
                    println("Received config: $payload")
                }

                "debug" -> {
                    if (payload is JSONObject) {
                        payload.keys().forEach { key ->
                            val value = payload.optDouble(key, Double.NaN).toFloat()
                            if (!value.isNaN()) {
                                debugViewModel.addDataPoint(key, value)
                            }
                        }
                        println("Received debug: $payload")
                    } else {
                        println("Debug payload is not a JSONObject")
                    }
                }

                "ack" -> {
                    println("Received ACK: $payload")
                }

                else -> {
                    println("Unknown type: $type")
                }
            }
        } catch (e: JSONException) {
            println("Failed to parse JSON: ${e.message}")
        }
    }
}

class SharedState {
    var isConnected: Boolean = false
    var receivedMessages: List<String> = emptyList()
    var receivedJsonData: String = ""
    var isJsonReceived: Boolean = false

    fun clear() {
        isConnected = false
        receivedMessages = emptyList()
        receivedJsonData = ""
        isJsonReceived = false
    }
}
