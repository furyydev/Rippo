import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/repo_model.dart';

class ApiService {
  static const String baseUrl = 'http://192.168.1.7:8080';
  static String? sessionId;

  Future<List<RepoModel>> fetchRepos() async {
    final url = Uri.parse('$baseUrl/repos');
    final response = await http.get(url, headers: _headers());

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);

      return data.map((repoJson) {
        return RepoModel.fromJson(repoJson);
      }).toList();
    }

    throw Exception('Failed to load repositories');
  }

  Future<List<RepoContentModel>> fetchRepoContents(
    String owner,
    String repoName,
  ) async {
    final url = Uri.parse('$baseUrl/repo/$owner/$repoName/contents');
    final response = await http.get(url, headers: _headers());

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);

      return data.map((contentJson) {
        return RepoContentModel.fromJson(contentJson);
      }).toList();
    }

    throw Exception('Failed to load repository contents');
  }

  Map<String, String> _headers() {
    if (sessionId == null) {
      return {};
    }

    return {
      'Cookie': 'JSESSIONID=$sessionId',
    };
  }
}
