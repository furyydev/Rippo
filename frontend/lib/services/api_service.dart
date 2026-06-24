import 'dart:convert';

import 'package:http/http.dart' as http;

import '../core/app_config.dart';
import '../models/repo_model.dart';

class ApiService {
  static String get baseUrl => AppConfig.apiBaseUrl;
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

    throw Exception(_errorMessage(response, 'Failed to load repositories'));
  }

  Future<List<RepoContentModel>> fetchRepoContents(
    String owner,
    String repoName, {
    String path = '',
  }) async {
    final contentsUrl = _repoUrl(owner, repoName, 'contents');
    final url = path.isEmpty
        ? contentsUrl
        : contentsUrl.replace(queryParameters: {'path': path});
    final response = await http.get(url, headers: _headers());

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);

      return data.map((contentJson) {
        return RepoContentModel.fromJson(contentJson);
      }).toList();
    }

    throw Exception(
      _errorMessage(response, 'Failed to load repository contents'),
    );
  }

  Future<RepoReadmeModel> fetchRepoReadme(String owner, String repoName) async {
    final response = await http.get(
      _repoUrl(owner, repoName, 'readme'),
      headers: _headers(),
    );

    if (response.statusCode == 200) {
      return RepoReadmeModel.fromJson(jsonDecode(response.body));
    }

    throw Exception(_errorMessage(response, 'Failed to load README'));
  }

  Future<List<CommitModel>> fetchRepoCommits(
    String owner,
    String repoName,
  ) async {
    final response = await http.get(
      _repoUrl(owner, repoName, 'commits'),
      headers: _headers(),
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(response.body);
      return data.map((json) => CommitModel.fromJson(json)).toList();
    }

    throw Exception(_errorMessage(response, 'Failed to load commits'));
  }

  Future<FileContentModel> fetchFileContent(
    String owner,
    String repoName,
    String path,
  ) async {
    final url = _repoUrl(
      owner,
      repoName,
      'file',
    ).replace(queryParameters: {'path': path});
    final response = await http.get(url, headers: _headers());

    if (response.statusCode == 200) {
      return FileContentModel.fromJson(jsonDecode(response.body));
    }

    throw Exception(_errorMessage(response, 'Failed to load file'));
  }

  Uri _repoUrl(String owner, String repoName, String endpoint) {
    return Uri.parse(
      '$baseUrl/repo/${Uri.encodeComponent(owner)}/'
      '${Uri.encodeComponent(repoName)}/$endpoint',
    );
  }

  String _errorMessage(http.Response response, String fallback) {
    try {
      final body = jsonDecode(response.body) as Map<String, dynamic>;
      return body['detail'] ?? body['message'] ?? fallback;
    } catch (_) {
      return '$fallback (${response.statusCode})';
    }
  }

  Map<String, String> _headers() {
    if (sessionId == null) {
      return {};
    }

    return {'Cookie': 'JSESSIONID=$sessionId'};
  }
}
