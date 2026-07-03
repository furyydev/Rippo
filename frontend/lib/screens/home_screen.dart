import 'package:flutter/material.dart';

import '../core/external_browser.dart';
import '../models/repo_model.dart';
import '../services/api_service.dart';
import '../widgets/repository_card.dart';
import 'repo_detail_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late Future<GitHubUserModel> userFuture;
  late Future<List<RepoModel>> reposFuture;

  @override
  void initState() {
    super.initState();
    final apiService = ApiService();
    userFuture = apiService.fetchUser();
    reposFuture = apiService.fetchRepos();
  }

  void openRepository(RepoModel repository) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => RepoDetailScreen(repository: repository),
      ),
    );
  }

  Future<void> viewGitHub(String profileUrl) async {
    try {
      await ExternalBrowser.launch(profileUrl);
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text('Could not open GitHub: $error')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 16,
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 28,
              height: 28,
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primary,
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(
                Icons.hub_outlined,
                size: 16,
                color: Colors.white,
              ),
            ),
            const SizedBox(width: 10),
            const Text('Rippo'),
          ],
        ),
      ),
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(20, 24, 20, 16),
            sliver: SliverToBoxAdapter(
              child: FutureBuilder<GitHubUserModel>(
                future: userFuture,
                builder: (context, snapshot) {
                  final username = snapshot.data?.username;
                  final theme = Theme.of(context);
                  return Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        username == null || username.isEmpty
                            ? 'Hi 👋'
                            : 'Hi, $username 👋',
                        style: theme.textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                          letterSpacing: 0.2,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        'Browse and explore your repositories',
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
          ),
          FutureBuilder<List<RepoModel>>(
            future: reposFuture,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const SliverFillRemaining(
                  child: Center(child: CircularProgressIndicator()),
                );
              }
              if (snapshot.hasError) {
                return SliverFillRemaining(
                  child: Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Text(
                        'Could not load repositories.\n${snapshot.error}',
                        textAlign: TextAlign.center,
                      ),
                    ),
                  ),
                );
              }

              final repositories = snapshot.data ?? [];
              if (repositories.isEmpty) {
                final theme = Theme.of(context);
                return SliverFillRemaining(
                  hasScrollBody: false,
                  child: Center(
                    child: Padding(
                      padding: const EdgeInsets.all(32),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Container(
                            width: 64,
                            height: 64,
                            decoration: BoxDecoration(
                              color: theme.colorScheme.primary.withValues(
                                alpha: 0.12,
                              ),
                              borderRadius: BorderRadius.circular(18),
                            ),
                            child: Icon(
                              Icons.folder_open_outlined,
                              size: 32,
                              color: theme.colorScheme.primary,
                            ),
                          ),
                          const SizedBox(height: 18),
                          Text(
                            'No repositories found',
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          const SizedBox(height: 6),
                          Text(
                            'Repositories you have access to will appear here.',
                            textAlign: TextAlign.center,
                            style: theme.textTheme.bodyMedium?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }

              return SliverPadding(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 100),
                sliver: SliverList.builder(
                  itemCount: repositories.length,
                  itemBuilder: (context, index) {
                    final repository = repositories[index];
                    return RepositoryCard(
                      repository: repository,
                      onOpen: () => openRepository(repository),
                    );
                  },
                ),
              );
            },
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        minimum: const EdgeInsets.fromLTRB(20, 8, 20, 14),
        child: FutureBuilder<GitHubUserModel>(
          future: userFuture,
          builder: (context, snapshot) {
            return OutlinedButton.icon(
              onPressed: snapshot.hasData
                  ? () => viewGitHub(snapshot.data!.profileUrl)
                  : null,
              icon: const Icon(Icons.open_in_new),
              label: const Text('View GitHub'),
            );
          },
        ),
      ),
    );
  }
}
