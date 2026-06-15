import 'dart:io' show Platform;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Opens a URL in the device's default browser via a native Android intent.
class ExternalBrowser {
  ExternalBrowser._();

  static const _channel = MethodChannel('com.example.reppo/external_browser');

  static const oauthLoginUrl =
      'http://192.168.1.7:8080/oauth2/authorization/github';

  static Future<void> launch(String url) async {
    if (!kIsWeb && Platform.isAndroid) {
      await _channel.invokeMethod<void>('launch', {'url': url});
      return;
    }
    throw UnsupportedError(
      'External browser launch is only supported on Android.',
    );
  }

  static Future<String?> getInitialAuthUri() async {
    if (!kIsWeb && Platform.isAndroid) {
      return _channel.invokeMethod<String>('getInitialAuthUri');
    }

    return null;
  }

  static void setAuthCallback(void Function(String uri)? onAuthCallback) {
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'authCallback') {
        final uri = call.arguments as String;
        onAuthCallback?.call(uri);
      }
    });
  }
}
