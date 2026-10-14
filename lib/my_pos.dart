import 'enums/my_pos_currency_enum.dart';
import 'enums/my_pos_payment_response_status.dart';
import 'enums/my_pos_print_response.dart';
import 'enums/sam_slot_enum.dart';
import 'models/my_pos_paper.dart';
import 'models/my_pos_payment_response.dart';
import 'my_pos_platform_interface.dart';

/// Card payments, receipt printing and the RKSV signature card on Android
/// Smart POS terminals.
///
/// All calls need the app to run on a supported terminal (or with the Glass
/// app for [makeGlassPayment]). Only one payment and one print job can run at
/// a time; a second call while one is running fails with `ALREADY_ACTIVE`.
class MyPos {
  /// Starts a card payment on the terminal and waits for the result.
  ///
  /// Check [MyPosPaymentResponse.isApproved] before treating the sale as paid.
  /// Without a result from the terminal the response has `status_text`
  /// `CANCEL` (screen closed) or `UNKNOWN` (result lost); in both cases check
  /// the terminal before charging again. Throws a
  /// `PlatformException` when the payment screen cannot be opened (no
  /// activity, payment service missing, invalid amount or currency).
  static Future<MyPosPaymentResponse> makePayment({
    required double amount,
    required MyPosCurrency currency,
    bool printMerchantReceipt = true,
    bool printCustomerReceipt = true,
    bool fixedPinPad = false,
    bool giftCardTransaction = false,
    String? eReceiptReceiver,
    String? reference,
  }) {
    return MyPosPlatform.instance.makePayment(
      amount: amount,
      currency: currency,
      printMerchantReceipt: printMerchantReceipt,
      printCustomerReceipt: printCustomerReceipt,
      fixedPinPad: fixedPinPad,
      giftCardTransaction: giftCardTransaction,
      eReceiptReceiver: eReceiptReceiver,
      reference: reference,
    );
  }

  /// Starts a tap-to-pay payment through the Glass app.
  static Future<PaymentResponseStatus> makeGlassPayment({
    required double amount,
    required MyPosCurrency currency,
    bool printMerchantReceipt = true,
    bool printCustomerReceipt = true,
    String? eReceiptReceiver,
    String? reference,
  }) {
    return MyPosPlatform.instance.makeGlassPayment(
      amount: amount,
      currency: currency,
      printMerchantReceipt: printMerchantReceipt,
      printCustomerReceipt: printCustomerReceipt,
      eReceiptReceiver: eReceiptReceiver,
      reference: reference,
    );
  }

  /// Serial number (hex) of the signature certificate on the SAM card.
  static Future<String?> getCertificateSerialHex(SamSlot samSlot,
      {int timeoutMs = 1000}) {
    return MyPosPlatform.instance.getCertificateSerialHex(samSlot, timeoutMs);
  }

  /// Serial number (decimal) of the signature certificate on the SAM card.
  static Future<String?> getCertificateSerialDec(SamSlot samSlot,
      {int timeoutMs = 1000}) {
    return MyPosPlatform.instance.getCertificateSerialDec(samSlot, timeoutMs);
  }

  /// Card identification number of the SAM card.
  static Future<String?> getCIN(SamSlot samSlot, {int timeoutMs = 1000}) {
    return MyPosPlatform.instance.getCIN(samSlot, timeoutMs);
  }

  /// Signs the SHA-256 hash of [data] with the SAM card; returns the
  /// signature as Base64url without padding.
  static Future<String?> doSignature(String data, SamSlot samSlot, String pin,
      {int timeoutMs = 1000}) {
    return MyPosPlatform.instance.doSignature(data, samSlot, pin, timeoutMs);
  }

  /// Like [doSignature], but skips selecting the signature application.
  static Future<String?> doSignatureWithoutSelection(
      String data, SamSlot samSlot, String pin,
      {int timeoutMs = 1000}) {
    return MyPosPlatform.instance
        .doSignatureWithoutSelection(data, samSlot, pin, timeoutMs);
  }

  /// Prints [data] on the built-in printer. Never throws; an image or QR code
  /// that cannot be built is left out.
  static Future<PrintResponse> printPaper(MyPosPaper data) {
    return MyPosPlatform.instance.printPaper(data);
  }
}
