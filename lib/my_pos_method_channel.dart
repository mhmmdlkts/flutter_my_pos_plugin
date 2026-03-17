import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:my_pos/enums/my_pos_print_response.dart';
import 'package:my_pos/models/my_pos_paper.dart';

import 'enums/my_pos_currency_enum.dart';
import 'enums/my_pos_payment_response_status.dart';
import 'enums/sam_slot_enum.dart';
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
    final Map<String, dynamic> args = {
      'amount': amount,
      'currency': currency.name,
      'printMerchantReceipt': printMerchantReceipt,
      'printCustomerReceipt': printCustomerReceipt,
      'fixedPinpad': fixedPinPad,
      'giftCardTransaction': giftCardTransaction,
      'eReceiptReceiver': eReceiptReceiver,
      'reference': reference,
    };
    final res = await methodChannel.invokeMethod('makePayment', args);

    try {
      return MyPosPaymentResponse.fromMap(Map<String, dynamic>.from(res));
    } catch (e) {
      print('Error parsing makePayment response: $e');
      return const MyPosPaymentResponse(status_text: 'UNKNOWN');
    }
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
    final Map<String, dynamic> args = {
      'amount': amount,
      'currency': currency.name,
      'printMerchantReceipt': printMerchantReceipt,
      'printCustomerReceipt': printCustomerReceipt,
      'eReceiptReceiver': eReceiptReceiver,
      'reference': reference,
    };
    final res = await methodChannel.invokeMethod('makeGlassPayment', args);
    if (res is Map) {
      final map = Map<String, dynamic>.from(res as Map);
      final approved = map['transaction_approved'] == true;
      if (approved) return PaymentResponseStatus.success;
      // If we have response_code we can treat as declined; otherwise unknown
      if (map.containsKey('response_code')) return PaymentResponseStatus.declined;
      return PaymentResponseStatus.unknown;
    }
    if (res is String) {
      switch (res) {
        case 'SUCCESS':
          return PaymentResponseStatus.success;
        case 'CANCEL':
          return PaymentResponseStatus.cancel;
        case 'ERROR':
          return PaymentResponseStatus.error;
        case 'TIMEOUT':
          return PaymentResponseStatus.timeout;
        case 'DECLINED':
          return PaymentResponseStatus.declined;
        case 'UNKNOWN':
          return PaymentResponseStatus.unknown;
      }
    }
    return PaymentResponseStatus.unknown;
  }

  @override
  Future<String?> getCertificateSerialHex(SamSlot samSlot, int timeoutMs) async {
    final Map<String, dynamic> args = {
      'samSlot': getSamSlot(samSlot),
      'timeoutMs': timeoutMs,
    };
    final res = await methodChannel.invokeMethod<String>('getCertificateSerialHex', args);
    return res;
  }

  @override
  Future<String?> doSignature(String data, SamSlot samSlot, String pin, int timeoutMs) async {
    final Map<String, dynamic> args = {
      'pin': pin,
      'data': data,
      'samSlot': getSamSlot(samSlot),
      'timeoutMs': timeoutMs,
    };
    final res = await methodChannel.invokeMethod<String>('doSignature', args);
    return res;
  }

  @override
  Future<String?> doSignatureWithoutSelection(String data, SamSlot samSlot, String pin, int timeoutMs) async {
    final Map<String, dynamic> args = {
      'pin': pin,
      'data': data,
      'samSlot': getSamSlot(samSlot),
      'timeoutMs': timeoutMs,
    };
    final res = await methodChannel.invokeMethod<String>('doSignatureWithoutSelection', args);
    return res;
  }

  @override
  Future<String?> getCertificateSerialDec(SamSlot samSlot, int timeoutMs) async {
    final Map<String, dynamic> args = {
      'samSlot': getSamSlot(samSlot),
      'timeoutMs': timeoutMs,
    };
    final res = await methodChannel.invokeMethod<String>('getCertificateSerialDec', args);
    return res;
  }

  @override
  Future<String?> getCIN(SamSlot samSlot, int timeoutMs) async {
    final Map<String, dynamic> args = {
      'samSlot': getSamSlot(samSlot),
      'timeoutMs': timeoutMs,
    };
    final res = await methodChannel.invokeMethod<String>('getCIN', args);
    return res;
  }

  @override
  Future<PrintResponse> printPaper(MyPosPaper data) async {
    final Map<String, dynamic> args = {
      'data': data.commands
    };
    final res = await methodChannel.invokeMethod<String>('printPaper', args);
    switch (res) {
      case 'SUCCESS':
        return PrintResponse.success;
      case 'FAILED':
        return PrintResponse.failed;
      case 'OUT_OF_PAPER':
        return PrintResponse.outOfPaper;
      case 'UNKNOWN':
        return PrintResponse.unknown;
    }
    return PrintResponse.unknown;
  }


  static int getSamSlot(SamSlot samSlot) {
    switch (samSlot) {
      case SamSlot.slot_1:
        return 1;
      case SamSlot.slot_2:
        return 2;
    }
  }
}
