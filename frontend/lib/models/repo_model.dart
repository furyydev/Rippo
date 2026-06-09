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
  final String type;

  RepoContentModel({
    required this.name,
    required this.type,
  });

  factory RepoContentModel.fromJson(Map<String, dynamic> json) {
    return RepoContentModel(
      name: json['name'] ?? '',
      type: json['type'] ?? '',
    );
  }
}
