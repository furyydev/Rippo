class ChatSessionModel {
  final int id;

  const ChatSessionModel({required this.id});

  factory ChatSessionModel.fromJson(Map<String, dynamic> json) {
    return ChatSessionModel(id: json['chatSessionId'] as int);
  }
}

class ChatResponseModel {
  final String assistantMessage;
  final int chatSessionId;
  final DateTime timestamp;

  const ChatResponseModel({
    required this.assistantMessage,
    required this.chatSessionId,
    required this.timestamp,
  });

  factory ChatResponseModel.fromJson(Map<String, dynamic> json) {
    return ChatResponseModel(
      assistantMessage: json['assistantMessage'] as String? ?? '',
      chatSessionId: json['chatSessionId'] as int,
      timestamp: DateTime.parse(json['timestamp'] as String),
    );
  }
}

class ChatBubbleModel {
  final String content;
  final bool isUser;

  const ChatBubbleModel({required this.content, required this.isUser});
}
