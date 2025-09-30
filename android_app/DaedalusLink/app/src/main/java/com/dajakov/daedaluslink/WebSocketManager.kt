package com.dajakov.daedaluslink

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
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

    // Parameters for storing current connection details for reconnection
    private var currentUrl: String? = null
    private var currentSharedState: SharedState? = null
    private var currentHeartbeatFrequency: Long = 1L
    private var currentDebugViewModel: DebugViewModel? = null
    private var currentRobotName: String? = null // Added to store robot name

    // Packet loss tracking
    private val sentPacketsInWindow: AtomicInteger = AtomicInteger(0)
    private val acksReceivedInWindow: AtomicInteger = AtomicInteger(0)

    companion object {
        private const val PACKET_LOSS_WINDOW_SIZE = 20 // Check loss after every 20 sent packets
        private const val PACKET_LOSS_THRESHOLD_PERCENT = 50 // If 50% or more packets are lost
    }

    suspend fun connectToWebSocket(
        url: String, sharedState: SharedState, heartbeatFrequency: Long,
        debugViewModel: DebugViewModel, robotName: String?
    ): Boolean {
        // Store current connection parameters
        currentUrl = url
        currentSharedState = sharedState
        currentHeartbeatFrequency = heartbeatFrequency
        currentDebugViewModel = debugViewModel
        this.currentRobotName = robotName // Store current robot name

        if (robotName != null) {
            currentSharedState?.robotName = robotName
        } // Update SharedState with robot name

        // Reset packet loss counters for the new connection attempt
        sentPacketsInWindow.set(0)
        acksReceivedInWindow.set(0)

        val request = Request.Builder().url(url).build()
        val connectionResult = CompletableDeferred<Boolean>()
        resendDelay = ((1F / heartbeatFrequency.toFloat()) * 1000F).toLong().coerceAtLeast(10L)

        lastCommand = null

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("WebSocket onOpen")
                currentSharedState?.isConnected = true
                currentSharedState?.packetLossPercentage = 0f // Reset packet loss on new connection
                reconnectAttempts = 0
                isReconnecting.set(false)
                connectionResult.complete(true)
                currentSharedState?.let { startResendChecker(it) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                currentSharedState?.receivedMessages = (currentSharedState?.receivedMessages ?: emptyList()) + text
                currentSharedState?.receivedMessages = currentSharedState?.receivedMessages?.takeLast(100) ?: emptyList()
                processReceivedMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("WebSocket onFailure: ${t.message}")
                currentSharedState?.isConnected = false
                currentSharedState?.packetLossPercentage = 0f // Reset packet loss on failure
                if (!connectionResult.isCompleted) {
                    connectionResult.complete(false)
                }
                if (!isReconnecting.get()) {
                    reconnectWebSocket()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket onClosing: Code=$code, Reason='$reason'")
                currentSharedState?.isConnected = false
                currentSharedState?.packetLossPercentage = 0f // Reset packet loss on closing
            }
        })

        return withTimeoutOrNull(10000) { connectionResult.await() } ?: false
    }

    private fun reconnectWebSocket() {
        if (currentUrl == null || currentSharedState == null || currentDebugViewModel == null || currentRobotName == null) { // Added currentRobotName check
            println("Cannot reconnect: connection parameters not available.")
            isReconnecting.set(false)
            return
        }

        if (isReconnecting.getAndSet(true)) {
            println("Reconnection already in progress.")
            return
        }

        val delayTime = (5000L * 2.0.pow(reconnectAttempts.toDouble())).toLong().coerceAtMost(60000L)
        reconnectAttempts++
        println("Attempting to reconnect in ${delayTime / 1000}s (Attempt #$reconnectAttempts)")

        Handler(Looper.getMainLooper()).postDelayed({
            CoroutineScope(Dispatchers.IO).launch {
                connectToWebSocket(currentUrl!!, currentSharedState!!, currentHeartbeatFrequency, currentDebugViewModel!!, currentRobotName!!) // Pass currentRobotName
            }
        }, delayTime)
    }

    private fun evaluatePacketLoss() {
        val sent = sentPacketsInWindow.getAndSet(0)
        val receivedAcks = acksReceivedInWindow.getAndSet(0)

        if (sent == 0 && receivedAcks == 0) {
            currentSharedState?.packetLossPercentage = 0f // No activity, loss is 0
            return
        }
        if (sent < PACKET_LOSS_WINDOW_SIZE / 2 && receivedAcks == 0) {
            // Not enough packets for a reliable decision yet, could set to an intermediate state or keep last value
            // For now, let's reflect current calculation or set to 0 if it's the start.
             if(sent > 0) currentSharedState?.packetLossPercentage = (((sent - receivedAcks).toFloat() / sent.toFloat()) * 100).coerceIn(0f,100f)
             else currentSharedState?.packetLossPercentage = 0f
            return
        }

        val lossPercentage = if (sent > 0) (((sent - receivedAcks).toFloat() / sent.toFloat()) * 100) else 0f
        currentSharedState?.packetLossPercentage = lossPercentage.coerceIn(0f, 100f)
        println("Packet Loss Evaluation: Sent=$sent, ReceivedAcks=$receivedAcks, Loss=${lossPercentage.toInt()}%")

        if (sent > 0 && lossPercentage >= PACKET_LOSS_THRESHOLD_PERCENT) {
            println("High packet loss detected (${lossPercentage.toInt()}%). Disconnecting.")
            currentSharedState?.isConnected = false // This will trigger onFailure or onClosing where packetLossPercentage is also reset.
            webSocket?.close(1001, "High packet loss detected")
        }
    }

    fun disconnect() {
        println("Client initiated disconnect.")
        resendJob?.cancel()
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
        currentSharedState?.isConnected = false
        currentSharedState?.packetLossPercentage = 0f // Reset on explicit disconnect
    }

    fun sendMovementCommand(x: Byte, y: Byte) {
        sendCommand("move $x,$y")
    }

    fun sendSliderCommand(commandId: String, value: Byte) {
        sendCommand("slider$commandId $value")
    }

    fun sendCommandToRobot(command: String) {
        sendCommand(command)
    }

    fun sendCommand(command: String) {
        if (currentSharedState?.isConnected == false && command != lastCommand) {
            return
        }
        webSocket?.send(command)?.let {
            updateLastCommand(command)
            val currentSentCount = sentPacketsInWindow.incrementAndGet()
            if (currentSentCount >= PACKET_LOSS_WINDOW_SIZE) {
                evaluatePacketLoss()
            }
        } ?: println("Failed to send command: WebSocket is not initialized or send failed. Command: $command")
    }

    private fun updateLastCommand(command: String) {
        lastCommand = command
        lastCommandTimestamp = System.currentTimeMillis()
    }

    private fun startResendChecker(sharedState: SharedState) {
        resendJob?.cancel()
        resendJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && sharedState.isConnected) {
                delay(resendDelay)
                val currentTime = System.currentTimeMillis()
                if (sharedState.isConnected && lastCommand != null && (currentTime - lastCommandTimestamp > resendDelay)) {
                    println("Resending last command: $lastCommand")
                    sendCommand(lastCommand!!)
                }
            }
            println("ResendChecker stopped.")
        }
    }

    private fun processReceivedMessage(message: String) {
        currentSharedState?.let { ss ->
            currentDebugViewModel?.let { dvm ->
                try {
                    val json = JSONObject(message)
                    val type = json.getString("type")
                    val payload = json.get("payload")

                    when (type) {
                        "config" -> {
                            ss.receivedJsonData = payload.toString()
                            ss.isJsonReceived = true
                            println("Received config: $payload")
                        }
                        "debug" -> {
                            if (payload is JSONObject) {
                                payload.keys().forEach { key ->
                                    val value = payload.optDouble(key, Double.NaN).toFloat()
                                    if (!value.isNaN()) {
                                        dvm.addDataPoint(key, value)
                                    }
                                }
                            }
                        }
                        "ack" -> {
                            acksReceivedInWindow.incrementAndGet()
                        }
                        else -> {
                            println("Unknown type: $type, Payload: $payload")
                        }
                    }
                } catch (e: JSONException) {
                    println("Failed to parse JSON: ${e.message} from message: $message")
                }
            }
        }
    }
}

class SharedState {
    var isConnected by mutableStateOf(false)
    var receivedMessages by mutableStateOf(emptyList<String>())
    var receivedJsonData by mutableStateOf("")
    var isJsonReceived by mutableStateOf(false)
    var robotName by mutableStateOf("") // Added for robot name
    var packetLossPercentage by mutableFloatStateOf(0f) // Added for packet loss

    fun clear() {
        isConnected = false
        receivedMessages = emptyList()
        receivedJsonData = ""
        isJsonReceived = false
        robotName = "" // Clear robot name
        packetLossPercentage = 0f // Clear packet loss
    }
}
