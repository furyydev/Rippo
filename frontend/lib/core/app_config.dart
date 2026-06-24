import 'package:flutter_dotenv/flutter_dotenv.dart';

class AppConfig {
  AppConfig._();

  static String get apiBaseUrl {
    final url = dotenv.env['API_BASE_URL']?.trim();

    if (url == null || url.isEmpty) {
      throw StateError('API_BASE_URL is missing from the .env file');
    }

    return url.endsWith('/') ? url.substring(0, url.length - 1) : url;
  }
}
