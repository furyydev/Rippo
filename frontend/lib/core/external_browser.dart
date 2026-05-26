import 'dart:io' show Platform;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Opens a URL in the device's default browser via a native Android intent.
class ExternalBrowser {
  ExternalBrowser._();

  static const _channel = MethodChannel('com.example.reppo/external_browser');

  static const oauthLoginUrl =
      'http://172.33.5.196:8080/oauth2/authorization/github';

  static Future<void> launch(String url) async {
    if (!kIsWeb && Platform.isAndroid) {
      await _channel.invokeMethod<void>('launch', {'url': url});
      return;
    }
    throw UnsupportedError(
      'External browser launch is only supported on Android.',
    );
  }
}
