import 'package:flutter/material.dart';
import 'package:flutter_markdown/flutter_markdown.dart';

import '../models/chat_model.dart';
import '../models/repo_model.dart';
import '../services/api_service.dart';

class AskReppoScreen extends StatefulWidget {
  final RepoModel repository;

  const AskReppoScreen({super.key, required this.repository});

  @override
  State<AskReppoScreen> createState() => _AskReppoScreenState();
}

class _AskReppoScreenState extends State<AskReppoScreen> {
  final messageController = TextEditingController();
  final scrollController = ScrollController();
  final messages = <ChatBubbleModel>[];
  final apiService = ApiService();

  int? chatSessionId;
  bool isSending = false;

  @override
  void dispose() {
    messageController.dispose();
    scrollController.dispose();
    super.dispose();
  }

  Future<void> sendMessage() async {
    final message = messageController.text.trim();
    if (message.isEmpty || isSending) return;

    setState(() {
      isSending = true;
      messages.add(ChatBubbleModel(content: message, isUser: true));
    });
    messageController.clear();
    FocusScope.of(context).unfocus();
    scrollToLatest();

    try {
      chatSessionId ??= (await apiService.createChatSession(
        widget.repository.owner,
        widget.repository.name,
      )).id;

      final response = await apiService.sendChatMessage(
        owner: widget.repository.owner,
        repoName: widget.repository.name,
        chatSessionId: chatSessionId!,
        message: message,
      );

      if (!mounted) return;
      setState(() {
        messages.add(
          ChatBubbleModel(
            content: response.assistantMessage,
            isUser: false,
          ),
        );
      });
    } catch (error) {
      if (!mounted) return;
      setState(() {
        messages.add(
          ChatBubbleModel(
            content: 'Could not get a response: $error',
            isUser: false,
          ),
        );
      });
    } finally {
      if (mounted) {
        setState(() => isSending = false);
        scrollToLatest();
      }
    }
  }

  void scrollToLatest() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!scrollController.hasClients) return;
      scrollController.animateTo(
        scrollController.position.maxScrollExtent,
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    });
  }

  Widget messageBubble(ChatBubbleModel message) {
    final colors = Theme.of(context).colorScheme;
    return Align(
      alignment: message.isUser
          ? Alignment.centerRight
          : Alignment.centerLeft,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 600),
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: message.isUser
              ? colors.primaryContainer
              : colors.surfaceContainerHighest,
          borderRadius: BorderRadius.circular(16),
        ),
        child: message.isUser
            ? Text(message.content)
            : MarkdownBody(data: message.content, selectable: true),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Ask Rippo'),
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
                controller: scrollController,
                padding: const EdgeInsets.all(16),
                children: [
                  messageBubble(
                    const ChatBubbleModel(
                      content:
                          "Hi! I'm Rippo. Ask me anything supported by this "
                          "repository's README and our recent conversation.",
                      isUser: false,
                    ),
                  ),
                  ...messages.map(messageBubble),
                  if (isSending)
                    const Align(
                      alignment: Alignment.centerLeft,
                      child: Padding(
                        padding: EdgeInsets.all(12),
                        child: CircularProgressIndicator(),
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
                      enabled: !isSending,
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
                    onPressed: isSending ? null : sendMessage,
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
