import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import '../models/repo_model.dart';
import '../services/api_service.dart';
import 'ask_reppo_screen.dart';
import 'file_viewer_screen.dart';

class RepoDetailScreen extends StatefulWidget {
  final RepoModel repository;
  final String currentPath;

  const RepoDetailScreen({
    super.key,
    required this.repository,
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
      widget.repository.owner,
      widget.repository.name,
      path: widget.currentPath,
    );

    if (isRepositoryRoot) {
      readmeFuture = apiService.fetchRepoReadme(
        widget.repository.owner,
        widget.repository.name,
      );
      commitsFuture = apiService.fetchRepoCommits(
        widget.repository.owner,
        widget.repository.name,
      );
    }
  }

  void openItem(RepoContentModel item) {
    if (item.type == 'dir') {
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (context) => RepoDetailScreen(
            repository: widget.repository,
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
            owner: widget.repository.owner,
            repoName: widget.repository.name,
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
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 28, 16, 10),
      child: Row(
        children: [
          Icon(icon, size: 18, color: theme.colorScheme.primary),
          const SizedBox(width: 10),
          Text(
            title.toUpperCase(),
            style: theme.textTheme.labelLarge?.copyWith(
              fontWeight: FontWeight.w700,
              letterSpacing: 0.8,
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  MarkdownStyleSheet readmeStyleSheet() {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return MarkdownStyleSheet.fromTheme(theme).copyWith(
      p: theme.textTheme.bodyMedium?.copyWith(
        height: 1.6,
        color: colors.onSurface,
      ),
      h1: theme.textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.bold),
      h2: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.bold),
      h3: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
      code: const TextStyle(
        fontFamily: 'monospace',
        fontSize: 13,
        backgroundColor: Color(0xFF0D1117),
      ),
      codeblockPadding: const EdgeInsets.all(14),
      codeblockDecoration: BoxDecoration(
        color: const Color(0xFF0D1117),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: theme.dividerColor),
      ),
      blockquoteDecoration: BoxDecoration(
        color: colors.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(8),
        border: Border(left: BorderSide(color: colors.primary, width: 3)),
      ),
      blockquotePadding: const EdgeInsets.all(12),
      horizontalRuleDecoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: theme.dividerColor)),
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

    final theme = Theme.of(context);
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: theme.colorScheme.surface,
        border: Border(bottom: BorderSide(color: theme.dividerColor)),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      child: Row(
        children: [
          Icon(
            Icons.account_tree_outlined,
            size: 18,
            color: theme.colorScheme.primary,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              path,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontFamily: 'monospace',
                fontSize: 13,
                color: theme.colorScheme.onSurfaceVariant,
              ),
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

        final theme = Theme.of(context);
        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 16),
          child: Column(
            children: contents.map((item) {
              final isDirectory = item.type == 'dir';
              return ListTile(
                leading: Icon(
                  isDirectory
                      ? Icons.folder_outlined
                      : Icons.insert_drive_file_outlined,
                  color: isDirectory
                      ? theme.colorScheme.primary
                      : theme.colorScheme.onSurfaceVariant,
                ),
                title: Text(item.name),
                subtitle: Text(isDirectory ? 'folder' : 'file'),
                trailing: const Icon(Icons.chevron_right, size: 20),
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
          margin: const EdgeInsets.symmetric(horizontal: 16),
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: MarkdownBody(
              data: readme.content,
              selectable: true,
              styleSheet: readmeStyleSheet(),
            ),
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
          margin: const EdgeInsets.fromLTRB(16, 0, 16, 24),
          child: Column(
            children: commits.map((commit) {
              return ListTile(
                leading: const Icon(Icons.commit, size: 20),
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
        ? widget.repository.name
        : widget.currentPath.split('/').last;

    return Scaffold(
      appBar: AppBar(title: Text(title)),
      body: ListView(
        padding: EdgeInsets.only(bottom: isRepositoryRoot ? 88 : 0),
        children: [
          buildPathHeader(),
          if (isRepositoryRoot) ...[
            sectionTitle('README', Icons.description_outlined),
            buildReadmeSection(),
          ],
          sectionTitle(
            isRepositoryRoot ? 'Repository Files' : 'Folder contents',
            Icons.folder_open,
          ),
          buildContentsSection(),
          if (isRepositoryRoot) ...[
            sectionTitle('Recent Commits', Icons.history),
            buildCommitsSection(),
          ] else
            const SizedBox(height: 24),
        ],
      ),
      bottomNavigationBar: isRepositoryRoot
          ? SafeArea(
              minimum: const EdgeInsets.fromLTRB(16, 8, 16, 14),
              child: SizedBox(
                width: double.infinity,
                child: FilledButton.icon(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) =>
                            AskReppoScreen(repository: widget.repository),
                      ),
                    );
                  },
                  icon: const Icon(Icons.auto_awesome, size: 20),
                  label: const Text('Ask Rippo'),
                ),
              ),
            )
          : null,
    );
  }
}
