package com.dajakov.daedaluslink

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.pow

class WebSocketManager(private val analyticsLogger: AnalyticsLogger?) { // Added analyticsLogger to constructor
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
    private var currentRobotName: String? = null
    // Removed currentAnalyticsLogger, will use the one from constructor

    private var connectionStartTime: Long = 0L

    private val sentPacketsInWindow: AtomicInteger = AtomicInteger(0)
    private val acksReceivedInWindow: AtomicInteger = AtomicInteger(0)

    companion object {
        private const val PACKET_LOSS_WINDOW_SIZE = 20 //TODO(make accessible to user)
        private const val PACKET_LOSS_THRESHOLD_PERCENT = 100 //TODO(make accessible to user)

//        private const val EVENT_CONNECTION_ESTABLISHED = "connection_established"
//        private const val EVENT_CONNECTION_LOST = "connection_lost"
//        private const val EVENT_RECONNECTION_ATTEMPT = "reconnection_attempt"
//        private const val EVENT_RECONNECTION_SUCCESS = "reconnection_success"
//        private const val EVENT_RECONNECTION_FAILURE = "reconnection_failure"
//        private const val EVENT_HIGH_PACKET_LOSS = "high_packet_loss_detected"
//
//        private const val PARAM_REASON = "reason"
//        private const val PARAM_DURATION_MS = "duration_ms"
//        private const val PARAM_ATTEMPT_NUMBER = "attempt_number"
//        private const val PARAM_LOSS_PERCENTAGE = "loss_percentage"
    }

    suspend fun connectToWebSocket(url: String, sharedState: SharedState, heartbeatFrequency: Long,
                                   debugViewModel: DebugViewModel, robotName: String): Boolean {
        currentUrl = url
        currentSharedState = sharedState
        currentHeartbeatFrequency = heartbeatFrequency
        currentDebugViewModel = debugViewModel
        this.currentRobotName = robotName

        currentSharedState?.robotName = robotName
        sentPacketsInWindow.set(0)
        acksReceivedInWindow.set(0)

        val request = Request.Builder().url(url).build()
        val connectionResult = CompletableDeferred<Boolean>()
        resendDelay = ((1F / heartbeatFrequency.toFloat()) * 1000F).toLong().coerceAtLeast(10L)
        lastCommand = null

//        if (isReconnecting.get()) {
//            val params = mapOf(PARAM_ATTEMPT_NUMBER to reconnectAttempts)
//            analyticsLogger?.logEvent(EVENT_RECONNECTION_ATTEMPT, params)
//        }

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                println("WebSocket onOpen")
                currentSharedState?.isConnected = true
                currentSharedState?.packetLossPercentage = 0f

//                if (isReconnecting.get()) {
//                    analyticsLogger?.logEvent(EVENT_RECONNECTION_SUCCESS)
//                } else {
//                    analyticsLogger?.logEvent(EVENT_CONNECTION_ESTABLISHED)
//                }
                connectionStartTime = System.currentTimeMillis()
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
//                val previouslyConnected = currentSharedState?.isConnected == true
                currentSharedState?.isConnected = false
                currentSharedState?.packetLossPercentage = 0f

//                val duration = if (connectionStartTime > 0) System.currentTimeMillis() - connectionStartTime else 0L
//                val reason = t.message ?: "unknown_failure"
//                val params = mapOf(
//                    PARAM_REASON to reason,
//                    PARAM_DURATION_MS to duration
//                )
//                if (previouslyConnected) analyticsLogger?.logEvent(EVENT_CONNECTION_LOST, params)
                connectionStartTime = 0L

//                if (isReconnecting.get() && reconnectAttempts > 0) {
//                     val failureParams = mapOf(PARAM_ATTEMPT_NUMBER to reconnectAttempts)
//                     analyticsLogger?.logEvent(EVENT_RECONNECTION_FAILURE, failureParams)
//                }

                if (!connectionResult.isCompleted) {
                    connectionResult.complete(false)
                }
                if (!isReconnecting.get()) {
                    reconnectWebSocket()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("WebSocket onClosing: Code=$code, Reason='$reason'")
//                val previouslyConnected = currentSharedState?.isConnected == true
                currentSharedState?.isConnected = false
                currentSharedState?.packetLossPercentage = 0f

//                val duration = if (connectionStartTime > 0) System.currentTimeMillis() - connectionStartTime else 0L
//                val params = mapOf(
//                    PARAM_REASON to "closing_code_$code: $reason",
//                    PARAM_DURATION_MS to duration
//                )
//                if (previouslyConnected) analyticsLogger?.logEvent(EVENT_CONNECTION_LOST, params)
                connectionStartTime = 0L
            }
        })

        return withTimeoutOrNull(10000) { connectionResult.await() } ?: false
    }

    private fun reconnectWebSocket() {
        if (currentUrl == null || currentSharedState == null || currentDebugViewModel == null || currentRobotName == null) {
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
                // connectToWebSocket now uses the constructor-injected analyticsLogger
                connectToWebSocket(currentUrl!!, currentSharedState!!, currentHeartbeatFrequency, currentDebugViewModel!!, currentRobotName!!)
            }
        }, delayTime)
    }

    private fun evaluatePacketLoss() {
        val sent = sentPacketsInWindow.getAndSet(0)
        val receivedAcks = acksReceivedInWindow.getAndSet(0)

        if (sent == 0 && receivedAcks == 0) {
            currentSharedState?.packetLossPercentage = 0f
            return
        }
        if (sent < PACKET_LOSS_WINDOW_SIZE / 2 && receivedAcks == 0) {
             if(sent > 0) currentSharedState?.packetLossPercentage = (((sent - receivedAcks).toFloat() / sent.toFloat()) * 100).coerceIn(0f,100f)
             else currentSharedState?.packetLossPercentage = 0f
            return
        }

        val lossPercentage = if (sent > 0) (((sent - receivedAcks).toFloat() / sent.toFloat()) * 100) else 0f
        currentSharedState?.packetLossPercentage = lossPercentage.coerceIn(0f, 100f)
        println("Packet Loss Evaluation: Sent=$sent, ReceivedAcks=$receivedAcks, Loss=${lossPercentage.toInt()}%")

        if (sent > 0 && lossPercentage >= PACKET_LOSS_THRESHOLD_PERCENT) {
            println("High packet loss detected (${lossPercentage.toInt()}%). Disconnecting.")
//            val params = mapOf(PARAM_LOSS_PERCENTAGE to lossPercentage)
//            analyticsLogger?.logEvent(EVENT_HIGH_PACKET_LOSS, params)

//            val duration = if (connectionStartTime > 0) System.currentTimeMillis() - connectionStartTime else 0L
//            val reasonParams = mapOf(
//                PARAM_REASON to "high_packet_loss",
//                PARAM_DURATION_MS to duration
//            )
//            analyticsLogger?.logEvent(EVENT_CONNECTION_LOST, reasonParams)
            connectionStartTime = 0L

            currentSharedState?.isConnected = false
            webSocket?.close(1001, "High packet loss detected")
        }
    }

    fun disconnect() {
        println("Client initiated disconnect.")
        val previouslyConnected = currentSharedState?.isConnected == true
        resendJob?.cancel()
        webSocket?.close(1000, "Client disconnected")
        webSocket = null
        currentSharedState?.isConnected = false
        currentSharedState?.packetLossPercentage = 0f

//        if (previouslyConnected) {
//            val duration = if (connectionStartTime > 0) System.currentTimeMillis() - connectionStartTime else 0L
//            val params = mapOf(
//                PARAM_REASON to "user_initiated",
//                PARAM_DURATION_MS to duration
//            )
//            analyticsLogger?.logEvent(EVENT_CONNECTION_LOST, params)
//        }
        connectionStartTime = 0L
    }

    fun sendMovementCommand(command: String, x: Byte, y: Byte) {
        sendCommand("$command $x,$y")
    }

    fun sendSliderCommand(command: String, value: Byte) {
        sendCommand("$command $value")
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
                    sendCommand("ack")
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
                    val payload = json.opt("payload")

                    when (type) {
                        "auth_required" -> {
                            if (payload != null) {
                                ss.receivedChallenge = payload.toString()
                            }
                            ss.isAuthRequired = true
                        }
                        "config" -> {
                            if (payload != null) {
                                ss.receivedJsonData = payload.toString()
                            }
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
    var robotName by mutableStateOf("")
    var packetLossPercentage by mutableStateOf(0f)

    var receivedChallenge by mutableStateOf("")

    var isAuthRequired by mutableStateOf(false)

    fun clear() {
        isConnected = false
        receivedMessages = emptyList()
        receivedJsonData = ""
        isJsonReceived = false
        robotName = ""
        packetLossPercentage = 0f
    }
}
