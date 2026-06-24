class RepoModel {
  final String owner;
  final String name;
  final String? description;
  final bool isPrivate;

  RepoModel({
    required this.owner,
    required this.name,
    required this.description,
    required this.isPrivate,
  });

  factory RepoModel.fromJson(Map<String, dynamic> json) {
    return RepoModel(
      owner: json['owner']['login'],
      name: json['name'] ?? '',
      description: json['description'],
      isPrivate: json['private'] ?? false,
    );
  }
}

class RepoContentModel {
  final String name;
  final String path;
  final String type;
  final int size;

  RepoContentModel({
    required this.name,
    required this.path,
    required this.type,
    required this.size,
  });

  factory RepoContentModel.fromJson(Map<String, dynamic> json) {
    return RepoContentModel(
      name: json['name'] ?? '',
      path: json['path'] ?? '',
      type: json['type'] ?? '',
      size: json['size'] ?? 0,
    );
  }
}

class RepoReadmeModel {
  final String name;
  final String path;
  final String content;

  RepoReadmeModel({
    required this.name,
    required this.path,
    required this.content,
  });

  factory RepoReadmeModel.fromJson(Map<String, dynamic> json) {
    return RepoReadmeModel(
      name: json['name'] ?? 'README',
      path: json['path'] ?? 'README',
      content: json['content'] ?? '',
    );
  }
}

class CommitModel {
  final String message;
  final String author;
  final DateTime? date;

  CommitModel({
    required this.message,
    required this.author,
    required this.date,
  });

  factory CommitModel.fromJson(Map<String, dynamic> json) {
    final commit = json['commit'] as Map<String, dynamic>? ?? {};
    final commitAuthor = commit['author'] as Map<String, dynamic>? ?? {};
    final githubAuthor = json['author'] as Map<String, dynamic>? ?? {};

    return CommitModel(
      message: commit['message'] ?? 'No commit message',
      author: githubAuthor['login'] ?? commitAuthor['name'] ?? 'Unknown author',
      date: DateTime.tryParse(commitAuthor['date'] ?? ''),
    );
  }
}

class FileContentModel {
  final String name;
  final String path;
  final int size;
  final String content;

  FileContentModel({
    required this.name,
    required this.path,
    required this.size,
    required this.content,
  });

  factory FileContentModel.fromJson(Map<String, dynamic> json) {
    return FileContentModel(
      name: json['name'] ?? '',
      path: json['path'] ?? '',
      size: json['size'] ?? 0,
      content: json['content'] ?? '',
    );
  }
}
