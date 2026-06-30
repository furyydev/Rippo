import 'package:flutter/material.dart';

import '../models/repo_model.dart';

class RepositoryCard extends StatelessWidget {
  final RepoModel repository;
  final VoidCallback onOpen;

  const RepositoryCard({
    super.key,
    required this.repository,
    required this.onOpen,
  });

  @override
  Widget build(BuildContext context) {
    final description = repository.description?.trim();

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              repository.name,
              style: Theme.of(
                context,
              ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600),
            ),
            if (description != null && description.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(
                description,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ],
            const SizedBox(height: 16),
            Align(
              alignment: Alignment.centerRight,
              child: FilledButton.tonal(
                onPressed: onOpen,
                child: const Text('Open'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
