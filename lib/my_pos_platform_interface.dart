import 'package:my_pos/models/my_pos_payment_response.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'enums/my_pos_currency_enum.dart';
import 'enums/my_pos_payment_response_status.dart';
import 'enums/my_pos_print_response.dart';
import 'enums/sam_slot_enum.dart';
import 'enums/my_pos_receipts.dart';
import 'models/my_pos_info.dart';
import 'models/my_pos_operation_result.dart';
import 'models/my_pos_paper.dart';
import 'models/my_pos_void_reference.dart';
import 'my_pos_method_channel.dart';

abstract class MyPosPlatform extends PlatformInterface {
  /// Constructs a MyPosPlatform.
  MyPosPlatform() : super(token: _token);

  static final Object _token = Object();

  static MyPosPlatform _instance = MethodChannelMyPos();

  /// The default instance of [MyPosPlatform] to use.
  ///
  /// Defaults to [MethodChannelMyPos].
  static MyPosPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [MyPosPlatform] when
  /// they register themselves.
  static set instance(MyPosPlatform instance) {
    PlatformInterface.verify(instance, _token);
    _instance = instance;
  }

  Future<MyPosPaymentResponse> makePayment({
    required double amount,
    required MyPosCurrency currency,
    bool printMerchantReceipt = true,
    bool printCustomerReceipt = true,
    bool fixedPinPad = false,
    bool giftCardTransaction = false,
    String? eReceiptReceiver,
    String? reference,
  }) {
    throw UnimplementedError(
        'makePayment(paymentInfo) has not been implemented.');
  }

  Future<PaymentResponseStatus> makeGlassPayment({
    required double amount,
    required MyPosCurrency currency,
    bool printMerchantReceipt = true,
    bool printCustomerReceipt = true,
    String? eReceiptReceiver,
    String? reference,
  }) {
    throw UnimplementedError(
        'makePayment(paymentInfo) has not been implemented.');
  }

  Future<String?> doSignature(
      String data, SamSlot samSlot, String pin, int timeoutMs) {
    throw UnimplementedError(
        'doSignature(String data, SamSlot samSlot, String pin) has not been implemented.');
  }

  Future<String?> doSignatureWithoutSelection(
      String data, SamSlot samSlot, String pin, int timeoutMs) {
    throw UnimplementedError(
        'doSignatureWithoutSelection(String data, SamSlot samSlot, String pin) has not been implemented.');
  }

  Future<String?> getCertificateSerialHex(SamSlot samSlot, int timeoutMs) {
    throw UnimplementedError(
        'getCertificateSerialHex(SamSlot samSlot) has not been implemented.');
  }

  Future<String?> getCertificateSerialDec(SamSlot samSlot, int timeoutMs) {
    throw UnimplementedError(
        'getCertificateSerialDec(SamSlot samSlot) has not been implemented.');
  }

  Future<String?> getCIN(SamSlot samSlot, int timeoutMs) {
    throw UnimplementedError(
        'getCIN(SamSlot samSlot) has not been implemented.');
  }

  Future<PrintResponse> printPaper(MyPosPaper data) {
    throw UnimplementedError(
        'printPaper(PrinterData data) has not been implemented.');
  }

  /// Voids one card payment. See [MyPos.voidPayment].
  Future<MyPosOperationResult> voidPayment(
    MyPosVoidReference reference, {
    MyPosReceipts receipts = MyPosReceipts.none,
    bool skipConfirmation = false,
  }) {
    throw UnimplementedError('voidPayment() has not been implemented.');
  }

  /// Voids the last payment on this terminal. See [MyPos.voidLastPayment].
  Future<MyPosOperationResult> voidLastPayment({
    MyPosReceipts receipts = MyPosReceipts.none,
    bool skipConfirmation = false,
  }) {
    throw UnimplementedError('voidLastPayment() has not been implemented.');
  }

  /// Refunds an amount to a card. See [MyPos.refund].
  Future<MyPosOperationResult> refund({
    required int amountCents,
    required MyPosCurrency currency,
    MyPosReceipts receipts = MyPosReceipts.none,
    bool skipConfirmation = false,
  }) {
    throw UnimplementedError('refund() has not been implemented.');
  }

  /// Terminal info. See [MyPos.posInfo].
  Future<MyPosInfo?> posInfo() {
    throw UnimplementedError('posInfo() has not been implemented.');
  }
}
