package com.example.daedaluslink

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import okhttp3.*

class WebSocketManager
{
    private var client: OkHttpClient = OkHttpClient()
    private lateinit var webSocket: WebSocket

    private var resendDelay: Long = 0L

    private var lastCommand: String? = null
    private var lastCommandTimestamp: Long = 0
    private var resendJob: Job? = null

    suspend fun connectToWebSocket(url: String, sharedState: SharedState, heartbeatFrequency: Long): Boolean {
        val request = Request.Builder().url(url).build()
        val maxMessages = 100
        val connectionResult = CompletableDeferred<Boolean>()

        resendDelay = heartbeatFrequency

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                sharedState.isConnected = true
                connectionResult.complete(true) // ✅ Connection successful
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (sharedState.receivedMessages.size >= maxMessages) {
                    sharedState.receivedMessages = sharedState.receivedMessages.drop(1) + text
                } else {
                    sharedState.receivedMessages += text
                }
                processReceivedMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                sharedState.isConnected = false
                connectionResult.complete(false) // ❌ Connection failed
                reconnectWebSocket()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                sharedState.isConnected = false
            }
        })

        startResendChecker()

        return withTimeoutOrNull(5000) { connectionResult.await() } ?: false
    }

    fun reconnectWebSocket() {
        Handler(Looper.getMainLooper()).postDelayed({
            CoroutineScope(Dispatchers.IO).launch {
//                connectToWebSocket()
            }
        }, 5000)
    }


    fun sendMovementCommand(x: Byte, y: Byte) {
        val command = "move $x,$y"
        sendCommand(command)
    }

    fun sendCommandToRobot(command: String) {
        sendCommand(command)
    }

    private fun sendCommand(command: String) {
        if (::webSocket.isInitialized) {
            webSocket.send(command)
            updateLastCommand(command)
        }
    }

    private fun updateLastCommand(command: String) {
        lastCommand = command
        lastCommandTimestamp = System.currentTimeMillis()
    }

    private fun startResendChecker() {
        resendJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val currentTime = System.currentTimeMillis()
                if (lastCommand != null && (currentTime - lastCommandTimestamp > resendDelay)) {
                    lastCommand?.let { command ->
                        webSocket.send(command)
                        updateLastCommand(command)
                    }
                }
                delay(resendDelay)
            }
        }
    }

    private fun processReceivedMessage(message: String) {
        try {
            // Save received JSON in shared state
            sharedState.receivedJsonData = message
            // ✅ Notify observers that JSON was received
            sharedState.isJsonReceived = true
        } catch (e: Exception) {
            println("Failed to parse JSON: ${e.message}")
        }
    }
}

// Shared state class
class SharedState {
    var isConnected: Boolean = false
    var receivedMessages: List<String> = emptyList()
    var receivedJsonData: String = ""
    var isJsonReceived: Boolean = false // ✅ Track JSON reception

    fun clear() {
        isConnected = false
        receivedMessages = emptyList()
        receivedJsonData = ""
        isJsonReceived = false
    }
}