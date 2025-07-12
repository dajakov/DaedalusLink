# Getting Started

## Add a New Robot Connect-Config

<p style="display: flex; gap: 10px; justify-content: flex-start;">
  <img src="/images/home_add.png" width="200">
  <img src="/images/settings.png" width="200">
</p>

To connect the app to your robot, you need to set up a **Connect-Config**. This configuration includes the essential data required to establish communication:

- **Configuration Name**: This name appears on the app's home screen.
- **Icon**: An optional icon shown next to the configuration on the home screen.
- **Connection Type**: Currently, only **Wi-Fi** is supported.
- **IP/MAC Address**: The IP address of your robot's WebSocket server.
- **Heartbeat Frequency**: The frequency (in Hz) at which the app sends a “still connected” packet to the robot. The robot echoes this back to detect connection issues.  
  _Recommended default: **10 Hz**_

Click **Save**, and your new Connect-Config will appear on the home screen.

---

## Connect to Your Robot

<p style="display: flex; gap: 10px; justify-content: flex-start;">
  <img src="/images/home.png" width="200">
  <img src="/images/control.png" width="200">
  <img src="/images/charts.png" width="200">
</p>

To connect:

1. Select your robot from the list.
2. Tap the **power button**.
3. If all connection checks are successful, your custom dashboard will load.
4. You’re now ready to **control your robot** in real time.

---

## Basic Code example for ESP8266

```cpp
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <WiFiClient.h>
#include <WebSocketsServer.h>
#include <ArduinoJson.h>

// Network credentials
const char* ssid = "R2D2";
const char* password = "changeMe";

// WebSocket server
WebSocketsServer webSocket = WebSocketsServer(81); // 192.168.4.1:81

unsigned long lastDebugSendTime = 0;
const unsigned long debugInterval = 100; // 10Hz

void setup() {
  Serial.begin(115200);
  WiFi.softAP(ssid, password);

  webSocket.begin();
  webSocket.onEvent(webSocketEvent);
}

void loop() {
  webSocket.loop();

  unsigned long now = millis();
  if (now - lastDebugSendTime >= debugInterval) {
    lastDebugSendTime = now;
    sendRandomDebugData();
  }
}

// Sends LinkConfig JSON to a WebSocket client
void sendConfigJson(uint8_t clientNum) {
  String jsonConfig = R"rawliteral({
    "type": "config",
    "payload": {
      "linkId": 1,
      "name": "R2D2",
      "commandUpdateFrequency": 500,
      "sensorUpdateFrequency": 1000,
      "debugLogUpdateFrequency": 2000,
      "interfaceData": [
        {
          "type": "button",
          "label": "Left rotate",
          "position": [0, 6],
          "size": [4, 1],
          "pressCommand": "left"
        },
        {
          "type": "button",
          "label": "Right Rotate",
          "position": [4, 6],
          "size": [4, 1],
          "pressCommand": "right"
        },
        {
          "type": "joystick",
          "label": "Move",
          "position": [2, 8],
          "size": [6, 6],
          "axes": ["X", "Y"],
          "pressCommand": "m"
        }
      ]
    }
  })rawliteral";

  webSocket.sendTXT(clientNum, jsonConfig);
}

// Send simulated debug data to show example data in the charts
void sendRandomDebugData() {
  StaticJsonDocument<256> doc;
  doc["type"] = "debug";

  JsonObject payload = doc.createNestedObject("payload");
  payload["temp"] = random(30, 60);         // Example: temperature
  payload["battery"] = random(70, 100);     // Example: battery %
  payload["motorSpeed"] = random(1000, 3000); // Example: motor RPM
  payload["uptime"] = millis();

  String message;
  serializeJson(doc, message);

  webSocket.broadcastTXT(message); // Send to all clients
}

// WebSocket event handling
void webSocketEvent(uint8_t clientNum, WStype_t type, uint8_t * payload, size_t length) {
  if (type == WStype_CONNECTED) {
    sendConfigJson(clientNum);
  } 
  else if (type == WStype_DISCONNECTED) {
  } 
  else if (type == WStype_TEXT) {
    String message = String((char*)payload);

    char uartMessage[12];

    if (message.startsWith("move ")) {
      int commaIndex = message.indexOf(',');
      if (commaIndex > 5) {
        String xStr = message.substring(5, commaIndex);
        String yStr = message.substring(commaIndex + 1);
        int x = xStr.toInt();
        int y = yStr.toInt();
        snprintf(uartMessage, sizeof(uartMessage), "M%+04d%+04d\n", x, y);
        Serial.write(uartMessage);
      } 
    } else if (message == "left") {
      snprintf(uartMessage, sizeof(uartMessage), "L00000000\n");
      Serial.write(uartMessage);
    } else if (message == "right") {
      snprintf(uartMessage, sizeof(uartMessage), "R00000000\n");
      Serial.write(uartMessage);
    } else if (message == "!left") {
      snprintf(uartMessage, sizeof(uartMessage), "!L0000000\n");
      Serial.write(uartMessage);
    } else if (message == "!right") {
      snprintf(uartMessage, sizeof(uartMessage), "!R0000000\n");
      Serial.write(uartMessage);
    }

    webSocket.sendTXT(clientNum, (char*)payload); // Echo back
  }
}
```
