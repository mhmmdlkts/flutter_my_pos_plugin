import 'package:my_pos/enums/my_pos_currency_enum.dart';

import 'enums/py_pos_payment_response.dart';
import 'enums/py_pos_print_response.dart';
import 'enums/sam_slot_enum.dart';
import 'models/my_pos_paper.dart';
import 'my_pos_platform_interface.dart';

class MyPos {

  static Future<PaymentResponse> makePayment({
    required double amount,
    required MyPosCurrency currency,
    bool printMerchantReceipt = true,
    bool printCustomerReceipt = true,
    bool fixedPinPad = false,
    bool giftCardTransaction = false,
    String? eReceiptReceiverEmail,
    String? reference
  }) async {
    return MyPosPlatform.instance.makePayment(
      amount: amount,
      currency: currency,
      printMerchantReceipt: printMerchantReceipt,
      printCustomerReceipt: printCustomerReceipt,
      fixedPinPad: fixedPinPad,
      giftCardTransaction: giftCardTransaction,
      eReceiptReceiverEmail: eReceiptReceiverEmail,
      reference: reference
    );
  }

  static Future<String?> getCertificateSerialHex(SamSlot samSlot, {int timeoutMs = 1000}) async {
    return MyPosPlatform.instance.getCertificateSerialHex(samSlot, timeoutMs);
  }

  static Future<String?> getCertificateSerialDec(SamSlot samSlot, {int timeoutMs = 1000}) async {
    return MyPosPlatform.instance.getCertificateSerialDec(samSlot, timeoutMs);
  }

  static Future<String?> getCIN(SamSlot samSlot, {int timeoutMs = 1000}) async {
    return MyPosPlatform.instance.getCIN(samSlot, timeoutMs);
  }

  static Future<String?> doSignature(String data, SamSlot samSlot, String pin, {int timeoutMs = 1000}) async {
    return MyPosPlatform.instance.doSignature(data, samSlot, pin, timeoutMs);
  }

  static Future<String?> doSignatureWithoutSelection(String data, SamSlot samSlot, String pin, {int timeoutMs = 1000}) async {
    return MyPosPlatform.instance.doSignatureWithoutSelection(data, samSlot, pin, timeoutMs);
  }

  static Future<PrintResponse> printPaper(MyPosPaper data) async {
    return MyPosPlatform.instance.printPaper(data);
  }
}