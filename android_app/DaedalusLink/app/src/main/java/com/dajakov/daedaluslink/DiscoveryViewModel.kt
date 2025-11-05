package com.dajakov.daedaluslink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class RobotDiscovery(
    val robotId: String,
    val ip: String,
    val wsPort: Int,
    val name: String,
    val lastSeen: Long
)

class DiscoveryViewModel : ViewModel() {
    private val _robots = MutableStateFlow<List<RobotDiscovery>>(emptyList())
    val robots: StateFlow<List<RobotDiscovery>> = _robots.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val socket = DatagramSocket(7777, InetAddress.getByName("0.0.0.0"))
            socket.broadcast = true
            val buffer = ByteArray(512)

            try {
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val message = String(packet.data, 0, packet.length)
                    val ip = packet.address.hostAddress!!
                    val now = System.currentTimeMillis()

                    try {
                        val json = JSONObject(message)
                        val robotId = json.optString("robotId", "Unknown")
                        val wsPort = json.optInt("wsPort", 8081)
                        val name = json.optString("name", robotId)

                        // Update state safely
                        _robots.update { current ->
                            val existing = current.indexOfFirst { it.robotId == robotId }
                            val updated = if (existing >= 0) {
                                current.mapIndexed { i, r ->
                                    if (i == existing) r.copy(lastSeen = now) else r
                                }
                            } else {
                                current + RobotDiscovery(robotId, ip, wsPort, name, now)
                            }
                            // Remove stale robots
                            updated.filter { now - it.lastSeen <= 5000 }
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } finally {
                socket.close()
            }
        }
    }
}
