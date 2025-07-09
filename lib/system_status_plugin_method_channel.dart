import 'package:flutter/services.dart';
import 'package:flutter/foundation.dart';

import 'system_status_plugin_platform_interface.dart';

class MethodChannelSystemStatusPlugin extends SystemStatusPluginPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('system_status_plugin');

  @override
  Future<int?> getBatteryLevel() async {
    final level = await methodChannel.invokeMethod<int>('getBatteryLevel');
    return level;
  }

  @override
  Future<String?> getCpuInfo() async {
    final info = await methodChannel.invokeMethod<String>('getCpuInfo');
    return info;
  }
}
