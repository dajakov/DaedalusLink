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
