import 'dart:async';

import 'package:flutter/services.dart';

class FlutterVolume {
  static final MethodChannel _channel = const MethodChannel('chavesgu/flutter_volume')
    ..setMethodCallHandler(_handler);

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static final StreamController<double> _streamController = StreamController<double>.broadcast();
  static Stream<double> get _onVolumeChange => _streamController.stream;

  static Future<double> get volume async {
    return await _channel.invokeMethod('getVolume');
  }

  static void listen(VolumeChanged volumeChanged) {
    _onVolumeChange.listen((v) {
      volumeChanged(v);
    });
    _channel.invokeMethod('listen');
  }

  static void dispose() {
    if (!_streamController.isClosed) _streamController.close();
    _channel.invokeMethod('dispose');
  }

  static Future<void> _handler(MethodCall call) async {
    final Map<dynamic, dynamic> args = call.arguments as Map;
    switch (call.method) {
      case 'setVolume':
        _streamController.add(args['value']);
        break;
      default:
        print('unkown method');
    }
  }
}

typedef void VolumeChanged(double volume);
