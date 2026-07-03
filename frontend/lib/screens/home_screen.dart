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
      appBar: AppBar(title: const Text('Rippo')),
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
            sliver: SliverToBoxAdapter(
              child: FutureBuilder<GitHubUserModel>(
                future: userFuture,
                builder: (context, snapshot) {
                  final username = snapshot.data?.username;
                  return Text(
                    username == null || username.isEmpty
                        ? 'Hi 👋'
                        : 'Hi, $username 👋',
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
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
                return const SliverFillRemaining(
                  child: Center(child: Text('No repositories found')),
                );
              }

              return SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
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
        minimum: const EdgeInsets.fromLTRB(16, 8, 16, 12),
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
