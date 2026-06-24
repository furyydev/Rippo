import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import '../models/repo_model.dart';
import '../services/api_service.dart';
import 'file_viewer_screen.dart';

class RepoDetailScreen extends StatefulWidget {
  final String owner;
  final String repoName;
  final String currentPath;

  const RepoDetailScreen({
    super.key,
    required this.owner,
    required this.repoName,
    this.currentPath = '',
  });

  @override
  State<RepoDetailScreen> createState() => _RepoDetailScreenState();
}

class _RepoDetailScreenState extends State<RepoDetailScreen> {
  late Future<List<RepoContentModel>> contentsFuture;
  Future<RepoReadmeModel>? readmeFuture;
  Future<List<CommitModel>>? commitsFuture;

  bool get isRepositoryRoot => widget.currentPath.isEmpty;

  @override
  void initState() {
    super.initState();
    final apiService = ApiService();

    contentsFuture = apiService.fetchRepoContents(
      widget.owner,
      widget.repoName,
      path: widget.currentPath,
    );

    if (isRepositoryRoot) {
      readmeFuture = apiService.fetchRepoReadme(widget.owner, widget.repoName);
      commitsFuture = apiService.fetchRepoCommits(
        widget.owner,
        widget.repoName,
      );
    }
  }

  void openItem(RepoContentModel item) {
    if (item.type == 'dir') {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => RepoDetailScreen(
            owner: widget.owner,
            repoName: widget.repoName,
            currentPath: item.path,
          ),
        ),
      );
      return;
    }

    if (item.type == 'file') {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => FileViewerScreen(
            owner: widget.owner,
            repoName: widget.repoName,
            path: item.path,
          ),
        ),
      );
    }
  }

  String formatDate(DateTime? date) {
    if (date == null) {
      return 'Unknown date';
    }

    final localDate = date.toLocal();
    final month = localDate.month.toString().padLeft(2, '0');
    final day = localDate.day.toString().padLeft(2, '0');
    return '${localDate.year}-$month-$day';
  }

  Widget sectionTitle(String title, IconData icon) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
      child: Row(
        children: [
          Icon(icon, size: 20),
          const SizedBox(width: 8),
          Text(title, style: Theme.of(context).textTheme.titleLarge),
        ],
      ),
    );
  }

  Widget sectionError(String message, Object? error) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Text(
        '$message\n$error',
        style: TextStyle(color: Theme.of(context).colorScheme.error),
      ),
    );
  }

  Widget buildPathHeader() {
    final path = isRepositoryRoot ? '/' : '/${widget.currentPath}';

    return Container(
      width: double.infinity,
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        children: [
          const Icon(Icons.account_tree_outlined, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              path,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontFamily: 'monospace'),
            ),
          ),
        ],
      ),
    );
  }

  Widget buildContentsSection() {
    return FutureBuilder<List<RepoContentModel>>(
      future: contentsFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: CircularProgressIndicator(),
            ),
          );
        }

        if (snapshot.hasError) {
          return sectionError('Could not load this folder.', snapshot.error);
        }

        final contents = snapshot.data ?? [];
        if (contents.isEmpty) {
          return const Padding(
            padding: EdgeInsets.all(32),
            child: Column(
              children: [
                Icon(Icons.folder_off_outlined, size: 40),
                SizedBox(height: 12),
                Text('This folder is empty'),
              ],
            ),
          );
        }

        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 12),
          child: Column(
            children: contents.map((item) {
              final isDirectory = item.type == 'dir';
              return ListTile(
                leading: Icon(
                  isDirectory ? Icons.folder : Icons.insert_drive_file,
                ),
                title: Text(item.name),
                subtitle: Text(isDirectory ? 'folder' : 'file'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => openItem(item),
              );
            }).toList(),
          ),
        );
      },
    );
  }

  Widget buildReadmeSection() {
    return FutureBuilder<RepoReadmeModel>(
      future: readmeFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: CircularProgressIndicator(),
            ),
          );
        }

        if (snapshot.hasError) {
          return sectionError(
            'README is unavailable for this repository.',
            snapshot.error,
          );
        }

        final readme = snapshot.data!;
        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 12),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: MarkdownBody(data: readme.content, selectable: true),
          ),
        );
      },
    );
  }

  Widget buildCommitsSection() {
    return FutureBuilder<List<CommitModel>>(
      future: commitsFuture,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(
            child: Padding(
              padding: EdgeInsets.all(24),
              child: CircularProgressIndicator(),
            ),
          );
        }

        if (snapshot.hasError) {
          return sectionError('Could not load commit history.', snapshot.error);
        }

        final commits = snapshot.data ?? [];
        if (commits.isEmpty) {
          return const ListTile(title: Text('No commits found'));
        }

        return Card(
          margin: const EdgeInsets.fromLTRB(12, 0, 12, 24),
          child: Column(
            children: commits.map((commit) {
              return ListTile(
                leading: const Icon(Icons.commit),
                title: Text(
                  commit.message,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                subtitle: Text('${commit.author} • ${formatDate(commit.date)}'),
              );
            }).toList(),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final title = isRepositoryRoot
        ? widget.repoName
        : widget.currentPath.split('/').last;

    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: ListView(
        children: [
          buildPathHeader(),
          sectionTitle(
            isRepositoryRoot ? 'Files' : 'Folder contents',
            Icons.folder_open,
          ),
          buildContentsSection(),
          if (isRepositoryRoot) ...[
            sectionTitle('README', Icons.description_outlined),
            buildReadmeSection(),
            sectionTitle('Latest commits', Icons.history),
            buildCommitsSection(),
          ] else
            const SizedBox(height: 24),
        ],
      ),
    );
  }
}
