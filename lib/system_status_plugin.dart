import 'dart:async';
import 'package:flutter/services.dart';

class SystemStatusPlugin {
  static const MethodChannel _channel = MethodChannel('system_status_plugin');

  static const EventChannel _batteryChannel =
      EventChannel('system_status/battery_info');

  static const EventChannel _uptimeChannel =
      EventChannel('system_status/uptime');

  /// Get static CPU and GPU info (model, architecture, revision, cores, etc.)
  static Future<Map<String, dynamic>> getCpuInfo() async {
    final Map<dynamic, dynamic> result =
        await _channel.invokeMethod('getCpuInfo');
    return result.cast<String, dynamic>();
  }

  /// Get current CPU clock speeds per core
  static Future<List<String>> getCpuClockSpeeds() async {
    final List<dynamic> result =
        await _channel.invokeMethod('getCpuClockSpeeds');
    return result.cast<String>();
  }

  /// Get current GPU load (percentage)
  static Future<String> getGpuLoad() async {
    final String result = await _channel.invokeMethod('getGpuLoad');
    return result;
  }

  /// Stream CPU clock speeds using a polling interval
  static Stream<List<String>> get cpuClockStream {
    return Stream.periodic(const Duration(seconds: 1))
        .asyncMap((_) => getCpuClockSpeeds());
  }

  /// Convenience method to extract GPU vendor from getCpuInfo()
  static Future<String> getGpuVendor() async {
    final info = await getCpuInfo();
    return info['gpuVendor'] ?? 'Unknown';
  }

  /// Convenience method to extract GPU renderer from getCpuInfo()
  static Future<String> getGpuRenderer() async {
    final info = await getCpuInfo();
    return info['gpuRenderer'] ?? 'Unknown';
  }

  /// Stream GPU vendor
  static Stream<String> get gpuVendorStream {
    return Stream.periodic(const Duration(seconds: 3))
        .asyncMap((_) => getGpuVendor());
  }

  /// Stream GPU renderer
  static Stream<String> get gpuRendererStream {
    return Stream.periodic(const Duration(seconds: 3))
        .asyncMap((_) => getGpuRenderer());
  }

  /// Convenience method to extract CPU scaling governor
  static Future<String> getScalingGovernor() async {
    final info = await getCpuInfo();
    return info['scalingGovernor'] ?? 'Unknown';
  }

  /// Stream CPU scaling governor
  static Stream<String> get scalingGovernorStream {
    return Stream.periodic(const Duration(seconds: 3))
        .asyncMap((_) => getScalingGovernor());
  }

  /// Get device details (model, RAM, storage, screen info, etc.)
  static Future<Map<String, dynamic>> getDeviceDetails() async {
    final Map<dynamic, dynamic> result =
        await _channel.invokeMethod('getDeviceDetails');
    return result.cast<String, dynamic>();
  }

  static Future<String> getAvailableRAM() async {
    return await _channel.invokeMethod('getAvailableRAM');
  }

  static Future<String> getAvailableStorage() async {
    return await _channel.invokeMethod('getAvailableStorage');
  }

  static Stream<String> get availableRamStream =>
      Stream.periodic(const Duration(seconds: 2))
          .asyncMap((_) => getAvailableRAM());

  static Stream<String> get availableStorageStream =>
      Stream.periodic(const Duration(seconds: 2))
          .asyncMap((_) => getAvailableStorage());

  static Future<Map<String, dynamic>> getBatteryInfo() async {
    final Map<dynamic, dynamic> result =
        await _channel.invokeMethod('getBatteryInfo');
    return result.cast<String, dynamic>();
  }

  static Stream<Map<String, dynamic>> get batteryStream => _batteryChannel
      .receiveBroadcastStream()
      .map((event) => Map<String, dynamic>.from(event));

  static Future<Map<String, dynamic>> getSystemInfo() async {
    final Map<dynamic, dynamic> result =
        await _channel.invokeMethod('getSystemInfo');
    return result.cast<String, dynamic>();
  }

  static Stream<String> get uptimeStream =>
      _uptimeChannel.receiveBroadcastStream().map((event) => event.toString());
}
