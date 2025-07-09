# system_status_plugin

[![Pub Version](https://img.shields.io/pub/v/system_status_plugin)](https://pub.dev/packages/system_status_plugin)
[![Platform](https://img.shields.io/badge/platform-android-blue)](https://flutter.dev/platforms)
[![License](https://img.shields.io/github/license/Rahul675/system_status_plugin)](https://github.com/Rahul675/system_status_plugin/blob/main/LICENSE)
[![GitHub Repo stars](https://img.shields.io/github/stars/Rahul675/system_status_plugin?style=social)](https://github.com/Rahul675/system_status_plugin)

A Flutter plugin to access real-time **system-level information** such as CPU, GPU, Battery, RAM, Storage, and more on **Android** devices.

---

## 🔧 Features

- ✅ **CPU Info** (Architecture, cores, clock speeds)
- ✅ **GPU Info** (Vendor, renderer, load %)
- ✅ **Battery Info** (Level, status, health)
- ✅ **Device Info** (Model, RAM, Storage, Screen)
- ✅ **System Info** (Board, Hardware, Uptime)
- ✅ **Live Streams** for dynamic values like battery, uptime, clock speeds, etc.

---

## 🚀 Installation

Add this to your `pubspec.yaml` file:

```yaml
dependencies:
  system_status_plugin: ^1.0.0
```

than run : 
```
flutter pub get
```
---

# 🧪 Usage
## Import the package
```
import 'package:system_status_plugin/system_status_plugin.dart';
```

## Get Static Info

```
final cpuInfo = await SystemStatusPlugin.getCpuInfo();
final gpuVendor = await SystemStatusPlugin.getGpuVendor();
final batteryInfo = await SystemStatusPlugin.getBatteryInfo();
final deviceDetails = await SystemStatusPlugin.getDeviceDetails();
final systemInfo = await SystemStatusPlugin.getSystemInfo();
```

## Get Dynamic Info
```
final clockSpeeds = await SystemStatusPlugin.getCpuClockSpeeds();
final gpuLoad = await SystemStatusPlugin.getGpuLoad();
final availableRAM = await SystemStatusPlugin.getAvailableRAM();
final availableStorage = await SystemStatusPlugin.getAvailableStorage();
final scalingGovernor = await SystemStatusPlugin.getScalingGovernor();

## Stream Real-Time Updates
SystemStatusPlugin.cpuClockStream.listen((speeds) {
  print("CPU Clock Speeds: $speeds");
});

SystemStatusPlugin.gpuRendererStream.listen((renderer) {
  print("GPU Renderer: $renderer");
});

SystemStatusPlugin.availableRamStream.listen((ram) {
  print("Available RAM: $ram");
});

SystemStatusPlugin.batteryStream.listen((battery) {
  print("Battery Status: ${battery['level']}%");
});

SystemStatusPlugin.uptimeStream.listen((uptime) {
  print("Uptime: $uptime");
});
```
## 📜 License
This project is licensed under the MIT License.


## 🙌 Contributions
PRs are welcome. Please fork the repo and open a pull request. For major changes, open an issue first to discuss what you'd like to change.

## 🔗 Links

[Pub.dev Page](https://pub.dev/packages/system_status_plugin)

[GitHub Repository](https://github.com/Rahul675/system_status_plugin)
