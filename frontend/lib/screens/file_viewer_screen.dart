import 'package:flutter/material.dart';

import '../models/repo_model.dart';
import '../services/api_service.dart';

class FileViewerScreen extends StatefulWidget {
  final String owner;
  final String repoName;
  final String path;

  const FileViewerScreen({
    super.key,
    required this.owner,
    required this.repoName,
    required this.path,
  });

  @override
  State<FileViewerScreen> createState() => _FileViewerScreenState();
}

class _FileViewerScreenState extends State<FileViewerScreen> {
  static const int maxDisplayedCharacters = 200000;

  late Future<FileContentModel> fileFuture;

  @override
  void initState() {
    super.initState();
    fileFuture = ApiService().fetchFileContent(
      widget.owner,
      widget.repoName,
      widget.path,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(widget.path.split('/').last)),
      body: FutureBuilder<FileContentModel>(
        future: fileFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          if (snapshot.hasError) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(
                  'Could not open this file.\n${snapshot.error}',
                  textAlign: TextAlign.center,
                ),
              ),
            );
          }

          final file = snapshot.data!;
          final isTruncated = file.content.length > maxDisplayedCharacters;
          final displayedContent = isTruncated
              ? file.content.substring(0, maxDisplayedCharacters)
              : file.content;

          return Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (isTruncated)
                Container(
                  color: Theme.of(context).colorScheme.secondaryContainer,
                  padding: const EdgeInsets.all(12),
                  child: const Text(
                    'This file is large, so only the first 200,000 '
                    'characters are shown.',
                  ),
                ),
              Expanded(
                child: Scrollbar(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.all(16),
                    scrollDirection: Axis.vertical,
                    child: SingleChildScrollView(
                      scrollDirection: Axis.horizontal,
                      child: SelectableText(
                        displayedContent,
                        style: const TextStyle(
                          fontFamily: 'monospace',
                          fontSize: 13,
                          height: 1.45,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
