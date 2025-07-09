import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'system_status_plugin_method_channel.dart';

abstract class SystemStatusPluginPlatform extends PlatformInterface {
  SystemStatusPluginPlatform() : super(token: _token);

  static final Object _token = Object();

  static SystemStatusPluginPlatform _instance =
      MethodChannelSystemStatusPlugin();

  static SystemStatusPluginPlatform get instance => _instance;

  static set instance(SystemStatusPluginPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<int?> getBatteryLevel() {
    throw UnimplementedError('getBatteryLevel() not implemented.');
  }

  Future<String?> getCpuInfo() {
    throw UnimplementedError('getCpuInfo() not implemented.');
  }
}
