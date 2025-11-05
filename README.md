# DaedalusLink — Robot Control App

Easily connect, monitor, and control your robots in real time using the DaedalusLink App and ESP32 library.
Create fully interactive dashboards with buttons, sliders, joysticks, and live data charts — all through a simple Wi-Fi connection.

# Join Our Community

Need help or want to share your project?  
Join our **official Discord server** to get support, ask questions, and showcase your builds:

[**Join the DaedalusLink Discord**](https://discord.gg/gX54bnCeTJ)

# Getting Started

## Add a New Robot Connect-Config

<p style="display: flex; gap: 10px; justify-content: flex-start;">
  <img src="/images/home_add.png" width="200">
  <img src="/images/settings.png" width="200">
</p>

To connect the app to your robot, you need to set up a **Connect-Config**. This configuration includes the essential data required to establish communication:

- **Configuration Name** – The name displayed on the home screen.  
- **Icon** – *(Optional)* Image shown beside the configuration.  
- **Connection Type** – Currently only **Wi-Fi** is supported.  
- **IP/MAC Address** – The IP address of your robot’s WebSocket server.  
  _(ESP32 default: `192.168.4.1:8081`)_
- **Heartbeat Frequency** – Frequency (in Hz) of the app’s “still connected” packets.  
  The robot echoes these packets back to detect disconnections.  
  💡 *Recommended default: **10 Hz***

Click **Save**, and your new Connect-Config will appear on the home screen.

---

## Connect to Your Robot

<p style="display: flex; gap: 10px; justify-content: flex-start;">
  <img src="/images/home.png" width="200">
  <img src="/images/control.png" width="200">
  <img src="/images/charts.png" width="200">
</p>

To connect:

1. Make sure you are in the same Network as the Websocket server. The ping test will fail if not. 
2. Select your robot from the list.
3. Tap the **power button**.
4. If all connection checks are successful, your custom dashboard will load.
5. You’re now ready to **control your robot** in real time.

---

## Quick Start for ESP32

Set up your ESP32 with the **DaedalusLink-ESP32** library and connect it to the app in minutes.

### 1. Install the Library
Download the official `DaedalusLink-ESP32` library using the **Arduino IDE Library Manager**.

### 2. Load the Example
Copy the following code or open the **BasicDemo** example included in the library.

```cpp
#include <WiFi.h>
#include "DaedalusLink.h"

DaedalusLink gui("ESP32-Robot", 1);

void setup() {
  Serial.begin(115200);

  const char* ssid = "R2D2-Control";
  const char* password = "robot123";
  WiFi.mode(WIFI_AP);
  WiFi.softAP(ssid, password);

  Serial.println();
  Serial.println("=== WiFi AP Mode ===");
  Serial.print("SSID: "); Serial.println(ssid);
  Serial.print("Password: "); Serial.println(password);
  Serial.print("IP Address: "); Serial.println(WiFi.softAPIP());

  gui.addButton("Forward", "MOVE_FORWARD", 0, 2, 4, 1);
  gui.addButton("Backward", "MOVE_BACKWARD", 4, 2, 4, 1);
  gui.addJoystick("Joystick", "move", 2, 7, 6, 6);
  gui.addSlider("s2", "s2", 0, 3, 1, 9);
  gui.addSlider("s1", "s1", 1, 3, 1, 9);

  gui.on("MOVE_FORWARD", [](String data) {
    Serial.println("Moving forward!");
  });

  gui.on("MOVE_BACKWARD", [](String data) {
    Serial.println("Moving backward!");
  });

  gui.on("move", [](String data) {
    Serial.printf("Joystick move: %s\n", data.c_str());
  });

  gui.on("s2", [](String data) {
    Serial.printf("Speed set: %s\n", data.c_str());
  });

  gui.on("s1", [](String data) {
    Serial.printf("Speed set: %s\n", data.c_str());
  });

  gui.begin(8081);
}

void loop() {

}
```
