import 'package:flutter/material.dart';

import '../models/repo_model.dart';

class AskReppoScreen extends StatefulWidget {
  final RepoModel repository;

  const AskReppoScreen({super.key, required this.repository});

  @override
  State<AskReppoScreen> createState() => _AskReppoScreenState();
}

class _AskReppoScreenState extends State<AskReppoScreen> {
  final messageController = TextEditingController();

  @override
  void dispose() {
    messageController.dispose();
    super.dispose();
  }

  void sendMessage() {
    if (messageController.text.trim().isEmpty) return;
    messageController.clear();
    FocusScope.of(context).unfocus();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('AI integration coming in the next phase.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Ask Reppo'),
            Text(
              '${widget.repository.owner}/${widget.repository.name}',
              style: Theme.of(context).textTheme.bodySmall,
            ),
          ],
        ),
      ),
      body: SafeArea(
        child: Column(
          children: [
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  Align(
                    alignment: Alignment.centerLeft,
                    child: Container(
                      constraints: const BoxConstraints(maxWidth: 520),
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Theme.of(
                          context,
                        ).colorScheme.surfaceContainerHighest,
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: const Text(
                        "👋 Hi! I'm Reppo.\n\n"
                        "I've analyzed this repository and I'm ready to help.\n\n"
                        "You can ask me things like:\n\n"
                        "• Explain the architecture\n\n"
                        "• How authentication works\n\n"
                        "• Summarize recent commits\n\n"
                        "• Find where API calls are made",
                      ),
                    ),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: messageController,
                      textInputAction: TextInputAction.send,
                      onSubmitted: (_) => sendMessage(),
                      decoration: const InputDecoration(
                        hintText: 'Ask about this repository',
                        border: OutlineInputBorder(),
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  IconButton.filled(
                    onPressed: sendMessage,
                    tooltip: 'Send',
                    icon: const Icon(Icons.send),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
