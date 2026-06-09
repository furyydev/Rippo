import 'package:flutter/material.dart';

import '../models/repo_model.dart';
import '../services/api_service.dart';
import 'repo_detail_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  late Future<List<RepoModel>> reposFuture;

  @override
  void initState() {
    super.initState();
    reposFuture = ApiService().fetchRepos();
  }

  void openRepoDetail(RepoModel repo) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) {
          return RepoDetailScreen(
            owner: repo.owner,
            repoName: repo.name,
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Rippo Repositories'),
      ),
      body: FutureBuilder<List<RepoModel>>(
        future: reposFuture,
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

          final repos = snapshot.data ?? [];

          if (repos.isEmpty) {
            return const Center(
              child: Text('No repositories found'),
            );
          }

          return ListView.builder(
            padding: const EdgeInsets.all(12),
            itemCount: repos.length,
            itemBuilder: (context, index) {
              final repo = repos[index];

              return Card(
                child: ListTile(
                  title: Text(repo.name),
                  subtitle: Text(
                    repo.description ?? 'No description',
                  ),
                  trailing: Text(
                    repo.isPrivate ? 'Private' : 'Public',
                  ),
                  onTap: () {
                    openRepoDetail(repo);
                  },
                ),
              );
            },
          );
        },
      ),
    );
  }
}
