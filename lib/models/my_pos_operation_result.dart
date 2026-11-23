import 'package:flutter/services.dart';

/// Why a void or refund never started. Nothing was sent to the card in any
/// of these cases.
enum MyPosNotStartedReason {
  /// No foreground activity to open the terminal from.
  noActivity,

  /// The values cannot be voided or refunded (e.g. STAN 0, amount ≤ 0).
  invalidArgument,

  /// A payment, void or refund is still running.
  alreadyActive,

  /// The terminal could not open its screen (no payment core on the device).
  notOpened,

  /// The terminal does not know the payment to void (e.g. after settlement).
  notFound,

  /// The terminal refused before any card step: not activated, currency or
  /// amount not allowed.
  refusedByTerminal,

  /// The plugin is not available on this platform.
  notSupported,
}

/// Outcome of a void or refund.
///
/// Only [MyPosDone] and [MyPosUnclear] may have moved money. An unclear
/// result must never be retried blindly: check the terminal's transactions
/// first.
sealed class MyPosOperationResult {
  const MyPosOperationResult({
    this.status,
    this.responseCode,
    this.statusText,
    this.extras = const {},
  });

  /// `TransactionProcessingResult` code of the terminal, if it sent one.
  final int? status;
  final String? responseCode;
  final String? statusText;

  /// Everything the terminal reported (keys as the terminal sends them).
  final Map<String, Object?> extras;

  /// Turns the native answer of a void or refund into a result.
  ///
  /// The terminal reports `status` (0 success, 1 cancelled, 2 declined, …);
  /// whether it also sends `transaction_approved` is not documented for these
  /// operations. Done is: approved, or status 0 without an explicit "not
  /// approved". Anything that may have moved money but is not clearly done is
  /// unclear.
  static MyPosOperationResult fromNative(Object? answer) {
    if (answer is! Map) {
      // `CANCEL` (screen closed without result), `UNKNOWN` (no data) or
      // nothing at all: the outcome is not known.
      return MyPosUnclear(statusText: answer is String ? answer : null);
    }
    final extras = <String, Object?>{
      for (final e in answer.entries) e.key.toString(): e.value,
    };
    final status = _int(extras['status']);
    final approved = _bool(extras['transaction_approved']);
    final responseCode = extras['response_code']?.toString();
    final statusText = extras['status_text']?.toString();

    MyPosOperationResult unclear() => MyPosUnclear(
        status: status,
        responseCode: responseCode,
        statusText: statusText,
        extras: extras);
    MyPosOperationResult notStarted(MyPosNotStartedReason reason) =>
        MyPosNotStarted(reason,
            status: status,
            responseCode: responseCode,
            statusText: statusText,
            extras: extras);

    if (approved == true) {
      return MyPosDone(
          status: status,
          responseCode: responseCode,
          statusText: statusText,
          extras: extras);
    }
    switch (status) {
      case 0:
        return approved == false
            ? unclear()
            : MyPosDone(
                status: status,
                responseCode: responseCode,
                statusText: statusText,
                extras: extras);
      case 1:
        return MyPosCancelled(
            status: status,
            responseCode: responseCode,
            statusText: statusText,
            extras: extras);
      case 2:
        return MyPosDeclined(
            status: status,
            responseCode: responseCode,
            statusText: statusText,
            extras: extras);
      case 4: // device not activated
      case 6: // invalid currency
      case 7: // invalid amount
      case 9: // invalid e-receipt credential
        return notStarted(MyPosNotStartedReason.refusedByTerminal);
      case 5: // no data found: the payment to void is unknown
        return notStarted(MyPosNotStartedReason.notFound);
      default:
        // 3 failed, 8 communication error, 10 completion pending, unknown.
        return unclear();
    }
  }

  /// Turns a platform error into a result. Errors raised before the terminal
  /// opened mean nothing happened; anything else is unclear.
  static MyPosOperationResult fromError(Object error) {
    if (error is MissingPluginException) {
      return const MyPosNotStarted(MyPosNotStartedReason.notSupported);
    }
    if (error is PlatformException) {
      final reason = switch (error.code) {
        'NO_ACTIVITY' => MyPosNotStartedReason.noActivity,
        'INVALID_ARGUMENT' => MyPosNotStartedReason.invalidArgument,
        'ALREADY_ACTIVE' => MyPosNotStartedReason.alreadyActive,
        'NOT_STARTED' => MyPosNotStartedReason.notOpened,
        _ => null,
      };
      if (reason != null) {
        return MyPosNotStarted(reason, statusText: error.message);
      }
      return MyPosUnclear(statusText: error.code);
    }
    return MyPosUnclear(statusText: error.toString());
  }

  static int? _int(Object? v) =>
      v is int ? v : (v is String ? int.tryParse(v.trim()) : null);

  static bool? _bool(Object? v) {
    if (v is bool) return v;
    if (v is String) {
      final t = v.trim().toLowerCase();
      if (t == 'true') return true;
      if (t == 'false') return false;
    }
    return null;
  }
}

/// The money moved: voided or refunded.
final class MyPosDone extends MyPosOperationResult {
  const MyPosDone(
      {super.status, super.responseCode, super.statusText, super.extras});
}

/// The card's bank declined; nothing moved.
final class MyPosDeclined extends MyPosOperationResult {
  const MyPosDeclined(
      {super.status, super.responseCode, super.statusText, super.extras});
}

/// Cancelled on the terminal; nothing moved.
final class MyPosCancelled extends MyPosOperationResult {
  const MyPosCancelled(
      {super.status, super.responseCode, super.statusText, super.extras});
}

/// Never started; nothing moved.
final class MyPosNotStarted extends MyPosOperationResult {
  const MyPosNotStarted(this.reason,
      {super.status, super.responseCode, super.statusText, super.extras});

  final MyPosNotStartedReason reason;
}

/// Not known whether money moved. Check the terminal before doing anything.
final class MyPosUnclear extends MyPosOperationResult {
  const MyPosUnclear(
      {super.status, super.responseCode, super.statusText, super.extras});
}
