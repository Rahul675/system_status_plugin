import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:system_status_plugin/system_status_plugin_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelSystemStatusPlugin platform = MethodChannelSystemStatusPlugin();
  const MethodChannel channel = MethodChannel('system_status_plugin');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getBatteryLevel(), '42');
  });
}
