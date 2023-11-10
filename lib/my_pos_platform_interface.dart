import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'enums/my_pos_currency_enum.dart';
import 'enums/py_pos_payment_response.dart';
import 'enums/py_pos_print_response.dart';
import 'enums/sam_slot_enum.dart';
import 'models/my_pos_paper.dart';
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

  Future<PaymentResponse> makePayment({
    required double amount,
    required MyPosCurrency currency,
    bool printMerchantReceipt = true,
    bool printCustomerReceipt = true,
    bool fixedPinPad = false,
    bool giftCardTransaction = false,
    String? eReceiptReceiverEmail,
    String? reference,
  }) {
    throw UnimplementedError('makePayment(paymentInfo) has not been implemented.');
  }

  Future<String?> doSignature(String data, SamSlot samSlot, String pin, int timeoutMs) {
    throw UnimplementedError('doSignature(String data, SamSlot samSlot, String pin) has not been implemented.');
  }

  Future<String?> doSignatureWithoutSelection(String data, SamSlot samSlot, String pin, int timeoutMs) {
    throw UnimplementedError('doSignatureWithoutSelection(String data, SamSlot samSlot, String pin) has not been implemented.');
  }

  Future<String?> getCertificateSerialHex(SamSlot samSlot, int timeoutMs) {
    throw UnimplementedError('getCertificateSerialHex(SamSlot samSlot) has not been implemented.');
  }

  Future<String?> getCertificateSerialDec(SamSlot samSlot, int timeoutMs) {
    throw UnimplementedError('getCertificateSerialDec(SamSlot samSlot) has not been implemented.');
  }

  Future<String?> getCIN(SamSlot samSlot, int timeoutMs) {
    throw UnimplementedError('getCIN(SamSlot samSlot) has not been implemented.');
  }

  Future<PrintResponse> printPaper(MyPosPaper data) {
    throw UnimplementedError('printPaper(PrinterData data) has not been implemented.');
  }
}
