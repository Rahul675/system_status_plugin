import 'package:flutter_test/flutter_test.dart';
import 'package:system_status_plugin/system_status_plugin.dart';
import 'package:system_status_plugin/system_status_plugin_platform_interface.dart';
import 'package:system_status_plugin/system_status_plugin_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockSystemStatusPluginPlatform
    with MockPlatformInterfaceMixin
    implements SystemStatusPluginPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<int?> getBatteryLevel() {
    // TODO: implement getBatteryLevel
    throw UnimplementedError();
  }

  @override
  Future<String?> getCpuInfo() {
    // TODO: implement getCpuInfo
    throw UnimplementedError();
  }
}

void main() {
  final SystemStatusPluginPlatform initialPlatform =
      SystemStatusPluginPlatform.instance;

  test('$MethodChannelSystemStatusPlugin is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelSystemStatusPlugin>());
  });

  test('getPlatformVersion', () async {
    SystemStatusPlugin systemStatusPlugin = SystemStatusPlugin();
    MockSystemStatusPluginPlatform fakePlatform =
        MockSystemStatusPluginPlatform();
    SystemStatusPluginPlatform.instance = fakePlatform;

    // expect(await systemStatusPlugin.ge(), '42');
  });
}
