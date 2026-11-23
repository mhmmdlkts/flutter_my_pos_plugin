/// Identifies a card payment for a void: STAN, authorization code and the
/// terminal's date/time (`YYMMDDHHmmss`), as the payment result reported them.
class MyPosVoidReference {
  const MyPosVoidReference({
    required this.stan,
    required this.authCode,
    required this.dateTime,
  });

  /// Must be greater than 0; the terminal reports it as text, e.g. `000123`.
  final int stan;
  final String authCode;
  final String dateTime;

  /// Reads the values from a map with the keys `stan`, `auth_code` and
  /// `date_time` (strings, as stored after a payment). Returns `null` when one
  /// is missing, empty, or the STAN is not a number greater than 0.
  static MyPosVoidReference? tryParse(Map<String, Object?>? values) {
    String? text(String key) {
      final v = values?[key];
      if (v is! String) return null;
      final t = v.trim();
      return t.isEmpty ? null : t;
    }

    final stanText = text('stan');
    final stan = stanText == null ? null : int.tryParse(stanText);
    final authCode = text('auth_code');
    final dateTime = text('date_time');
    if (stan == null || stan <= 0 || authCode == null || dateTime == null) {
      return null;
    }
    return MyPosVoidReference(
        stan: stan, authCode: authCode, dateTime: dateTime);
  }

  @override
  bool operator ==(Object other) =>
      other is MyPosVoidReference &&
      other.stan == stan &&
      other.authCode == authCode &&
      other.dateTime == dateTime;

  @override
  int get hashCode => Object.hash(stan, authCode, dateTime);

  @override
  String toString() => 'MyPosVoidReference(stan: $stan)';
}
