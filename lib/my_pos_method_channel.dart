import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'enums/my_pos_currency_enum.dart';
import 'enums/my_pos_payment_response_status.dart';
import 'enums/my_pos_print_response.dart';
import 'enums/sam_slot_enum.dart';
import 'models/my_pos_paper.dart';
import 'models/my_pos_payment_response.dart';
import 'my_pos_platform_interface.dart';

/// An implementation of [MyPosPlatform] that uses method channels.
class MethodChannelMyPos extends MyPosPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('my_pos');

  @override
  Future<MyPosPaymentResponse> makePayment({
    required double amount,
    required MyPosCurrency currency,
    bool printMerchantReceipt = true,
    bool printCustomerReceipt = true,
    bool fixedPinPad = false,
    bool giftCardTransaction = false,
    String? eReceiptReceiver,
    String? reference,
  }) async {
    final res = await methodChannel.invokeMethod<Object?>('makePayment', {
      'amount': amount,
      'currency': currency.name,
      'printMerchantReceipt': printMerchantReceipt,
      'printCustomerReceipt': printCustomerReceipt,
      'fixedPinpad': fixedPinPad,
      'giftCardTransaction': giftCardTransaction,
      'eReceiptReceiver': eReceiptReceiver,
      'reference': reference,
    });
    return paymentResponseFrom(res);
  }

  /// Turns the native answer into a response: the result extras of the
  /// payment, or a status text (`CANCEL`, `UNKNOWN`) when there are none.
  @visibleForTesting
  static MyPosPaymentResponse paymentResponseFrom(Object? res) {
    if (res is Map) {
      return MyPosPaymentResponse.fromMap(Map<String, dynamic>.from(res));
    }
    return MyPosPaymentResponse(
      status_text: res is String ? res : 'UNKNOWN',
      transaction_approved: false,
    );
  }

  @override
  Future<PaymentResponseStatus> makeGlassPayment({
    required double amount,
    required MyPosCurrency currency,
    bool printMerchantReceipt = true,
    bool printCustomerReceipt = true,
    String? eReceiptReceiver,
    String? reference,
  }) async {
    final res = await methodChannel.invokeMethod<Object?>('makeGlassPayment', {
      'amount': amount,
      'currency': currency.name,
      'printMerchantReceipt': printMerchantReceipt,
      'printCustomerReceipt': printCustomerReceipt,
      'eReceiptReceiver': eReceiptReceiver,
      'reference': reference,
    });
    return glassStatusFrom(res);
  }

  @visibleForTesting
  static PaymentResponseStatus glassStatusFrom(Object? res) {
    if (res is Map) {
      if (res['transaction_approved'] == true) return PaymentResponseStatus.success;
      if (res.containsKey('response_code')) return PaymentResponseStatus.declined;
      return PaymentResponseStatus.unknown;
    }
    return switch (res) {
      'SUCCESS' => PaymentResponseStatus.success,
      'CANCEL' => PaymentResponseStatus.cancel,
      'ERROR' => PaymentResponseStatus.error,
      'TIMEOUT' => PaymentResponseStatus.timeout,
      'DECLINED' => PaymentResponseStatus.declined,
      _ => PaymentResponseStatus.unknown,
    };
  }

  @override
  Future<String?> getCertificateSerialHex(SamSlot samSlot, int timeoutMs) {
    return methodChannel.invokeMethod<String>('getCertificateSerialHex', _sam(samSlot, timeoutMs));
  }

  @override
  Future<String?> getCertificateSerialDec(SamSlot samSlot, int timeoutMs) {
    return methodChannel.invokeMethod<String>('getCertificateSerialDec', _sam(samSlot, timeoutMs));
  }

  @override
  Future<String?> getCIN(SamSlot samSlot, int timeoutMs) {
    return methodChannel.invokeMethod<String>('getCIN', _sam(samSlot, timeoutMs));
  }

  @override
  Future<String?> doSignature(String data, SamSlot samSlot, String pin, int timeoutMs) {
    return methodChannel.invokeMethod<String>('doSignature', {
      ..._sam(samSlot, timeoutMs),
      'data': data,
      'pin': pin,
    });
  }

  @override
  Future<String?> doSignatureWithoutSelection(String data, SamSlot samSlot, String pin, int timeoutMs) {
    return methodChannel.invokeMethod<String>('doSignatureWithoutSelection', {
      ..._sam(samSlot, timeoutMs),
      'data': data,
      'pin': pin,
    });
  }

  Map<String, Object> _sam(SamSlot samSlot, int timeoutMs) => {
        'samSlot': getSamSlot(samSlot),
        'timeoutMs': timeoutMs,
      };

  /// Printing never throws: a job that cannot start is reported as
  /// [PrintResponse.failed].
  @override
  Future<PrintResponse> printPaper(MyPosPaper data) async {
    try {
      final res = await methodChannel.invokeMethod<String>('printPaper', {'data': data.commands});
      return printResponseFrom(res);
    } on PlatformException {
      return PrintResponse.failed;
    } on MissingPluginException {
      return PrintResponse.failed;
    }
  }

  @visibleForTesting
  static PrintResponse printResponseFrom(String? res) => switch (res) {
        'SUCCESS' => PrintResponse.success,
        'FAILED' => PrintResponse.failed,
        'OUT_OF_PAPER' => PrintResponse.outOfPaper,
        _ => PrintResponse.unknown,
      };

  static int getSamSlot(SamSlot samSlot) => switch (samSlot) {
        SamSlot.slot_1 => 1,
        SamSlot.slot_2 => 2,
      };
}
