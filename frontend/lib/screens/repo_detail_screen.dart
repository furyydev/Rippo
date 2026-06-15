import 'package:flutter/material.dart';

import '../models/repo_model.dart';
import '../services/api_service.dart';

class RepoDetailScreen extends StatefulWidget {
  final String owner;
  final String repoName;

  const RepoDetailScreen({
    super.key,
    required this.owner,
    required this.repoName,
  });

  @override
  State<RepoDetailScreen> createState() => _RepoDetailScreenState();
}

class _RepoDetailScreenState extends State<RepoDetailScreen> {
  late Future<List<RepoContentModel>> contentsFuture;

  @override
  void initState() {
    super.initState();
    contentsFuture = ApiService().fetchRepoContents(
      widget.owner,
      widget.repoName,
    );
  }

  IconData getIconForType(String type) {
    if (type == 'dir') {
      return Icons.folder;
    }

    return Icons.insert_drive_file;
  }

  String getDisplayType(String type) {
    if (type == 'dir') {
      return 'folder';
    }

    return 'file';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.repoName),
      ),
      body: FutureBuilder<List<RepoContentModel>>(
        future: contentsFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(),
            );
          }

          if (snapshot.hasError) {
            return Center(
              child: Text('Error: ${snapshot.error}'),
            );
          }

          final contents = snapshot.data ?? [];

          if (contents.isEmpty) {
            return const Center(
              child: Text('No files or folders found'),
            );
          }

          return ListView.builder(
            itemCount: contents.length,
            itemBuilder: (context, index) {
              final item = contents[index];

              return ListTile(
                leading: Icon(getIconForType(item.type)),
                title: Text(item.name),
                subtitle: Text(getDisplayType(item.type)),
              );
            },
          );
        },
      ),
    );
  }
}
