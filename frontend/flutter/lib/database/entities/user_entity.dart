class UserEntity {
  final String id;
  final String email;
  final String firstName;
  final String lastName;
  final DateTime createdAt;

  UserEntity({
    required this.id,
    required this.email,
    required this.firstName,
    required this.lastName,
    required this.createdAt,
  });

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'email': email,
      'first_name': firstName,
      'last_name': lastName,
      'created_at': createdAt.toIso8601String(),
    };
  }

  factory UserEntity.fromMap(Map<String, dynamic> map) {
    return UserEntity(
      id: map['id'],
      email: map['email'],
      firstName: map['first_name'],
      lastName: map['last_name'],
      createdAt: DateTime.parse(map['created_at']),
    );
  }
}
