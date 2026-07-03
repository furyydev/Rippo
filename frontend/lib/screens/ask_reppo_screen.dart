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
          ChatBubbleModel(content: response.assistantMessage, isUser: false),
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
    const radius = Radius.circular(18);
    return Align(
      alignment: message.isUser ? Alignment.centerRight : Alignment.centerLeft,
      child: Container(
        constraints: const BoxConstraints(maxWidth: 600),
        margin: const EdgeInsets.only(bottom: 18),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: BoxDecoration(
          color: message.isUser
              ? colors.primary
              : colors.surfaceContainerHighest,
          borderRadius: BorderRadius.only(
            topLeft: radius,
            topRight: radius,
            bottomLeft: message.isUser ? radius : const Radius.circular(4),
            bottomRight: message.isUser ? const Radius.circular(4) : radius,
          ),
          border: message.isUser
              ? null
              : Border.all(color: Theme.of(context).dividerColor),
        ),
        child: message.isUser
            ? Text(
                message.content,
                style: const TextStyle(color: Colors.white, height: 1.4),
              )
            : MarkdownBody(data: message.content, selectable: true),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        title: Row(
          children: [
            Container(
              width: 30,
              height: 30,
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.primary,
                borderRadius: BorderRadius.circular(9),
              ),
              child: const Icon(
                Icons.auto_awesome,
                size: 17,
                color: Colors.white,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('Ask Rippo'),
                  Text(
                    '${widget.repository.owner}/${widget.repository.name}',
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
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
                padding: const EdgeInsets.fromLTRB(16, 20, 16, 20),
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
            Container(
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surface,
                border: Border(
                  top: BorderSide(color: Theme.of(context).dividerColor),
                ),
              ),
              padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: TextField(
                      controller: messageController,
                      enabled: !isSending,
                      textInputAction: TextInputAction.send,
                      minLines: 1,
                      maxLines: 5,
                      onSubmitted: (_) => sendMessage(),
                      decoration: const InputDecoration(
                        hintText: 'Ask about this repository',
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
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
