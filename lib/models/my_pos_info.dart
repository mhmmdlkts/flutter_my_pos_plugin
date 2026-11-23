/// What the terminal reports about itself.
class MyPosInfo {
  const MyPosInfo({
    this.tid,
    this.currencyName,
    this.currencyCode,
    this.merchantId,
    this.merchantName,
  });

  /// Terminal ID.
  final String? tid;
  final String? currencyName;
  final String? currencyCode;
  final String? merchantId;
  final String? merchantName;

  static MyPosInfo? fromMap(Object? map) {
    if (map is! Map) return null;
    String? text(String key) {
      final v = map[key];
      return v is String && v.trim().isNotEmpty ? v.trim() : null;
    }

    return MyPosInfo(
      tid: text('tid'),
      currencyName: text('currencyName'),
      currencyCode: text('currencyCode'),
      merchantId: text('merchantId'),
      merchantName: text('merchantName'),
    );
  }
}
