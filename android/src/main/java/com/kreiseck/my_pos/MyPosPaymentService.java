package com.kreiseck.my_pos;

import android.app.Activity;

import com.mypos.smartsdk.Currency;
import com.mypos.smartsdk.MyPOSAPI;
import com.mypos.smartsdk.MyPOSPayment;
import com.mypos.smartsdk.MyPOSUtil;
import com.mypos.smartsdk.ReferenceType;

import java.util.UUID;

/** Card payment on Android Smart POS terminals. */
final class MyPosPaymentService {

  private MyPosPaymentService() {}

  /** Opens the terminal's payment screen; throws when it cannot be opened. */
  static void startPayment(Activity activity, PaymentArgs args, int requestCode) throws Exception {
    MyPOSPayment payment = MyPOSPayment.builder()
        .productAmount(args.amount)
        .currency(currency(args.currency))
        .foreignTransactionId(UUID.randomUUID().toString())
        .fixedPinpad(args.fixedPinpad)
        .giftCardTransaction(args.giftCardTransaction)
        .build();

    if (args.reference != null) {
      payment.setReference(args.reference, ReferenceType.REFERENCE_NUMBER);
    }
    payment.setPrintCustomerReceipt(customerReceipt(args));
    if (args.eReceiptReceiver != null && !args.printCustomerReceipt) {
      payment.setEReceiptReceiver(args.eReceiptReceiver);
    }
    payment.setPrintMerchantReceipt(args.printMerchantReceipt ? MyPOSUtil.RECEIPT_ON : MyPOSUtil.RECEIPT_OFF);

    MyPOSAPI.openPaymentActivity(activity, payment, requestCode);
  }

  static int customerReceipt(PaymentArgs args) {
    if (args.printCustomerReceipt) return MyPOSUtil.RECEIPT_ON;
    if (args.eReceiptReceiver != null) return MyPOSUtil.RECEIPT_E_RECEIPT;
    return MyPOSUtil.RECEIPT_OFF;
  }

  /** Maps the ISO code; an unknown code is an error, never a silent switch to EUR. */
  static Currency currency(String code) {
    try {
      return Currency.valueOf(code);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Currency not supported by the terminal: " + code);
    }
  }
}
