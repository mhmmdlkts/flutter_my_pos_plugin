package com.kreiseck.my_pos;

import android.app.Activity;

import com.mypos.glasssdk.Currency;
import com.mypos.glasssdk.MyPOSAPI;
import com.mypos.glasssdk.MyPOSPayment;
import com.mypos.glasssdk.MyPOSUtil;
import com.mypos.glasssdk.ReferenceType;

import java.util.UUID;

/** Tap to pay on a phone with the Glass app. */
final class MyPosGlassPaymentService {

  private MyPosGlassPaymentService() {}

  /** Opens the Glass payment screen; throws when it cannot be opened. */
  static void startPayment(Activity activity, PaymentArgs args, int requestCode) throws Exception {
    MyPOSPayment payment = MyPOSPayment.builder()
        .productAmount(args.amount)
        .currency(currency(args.currency))
        .foreignTransactionId(UUID.randomUUID().toString())
        .build();

    if (args.reference != null) {
      payment.setReference(args.reference, ReferenceType.REFERENCE_NUMBER);
    }
    if (args.printCustomerReceipt) {
      payment.setPrintCustomerReceipt(MyPOSUtil.RECEIPT_ON);
    } else if (args.eReceiptReceiver != null) {
      payment.setEReceiptReceiver(args.eReceiptReceiver);
      payment.setPrintCustomerReceipt(MyPOSUtil.RECEIPT_E_RECEIPT);
    } else {
      payment.setPrintCustomerReceipt(MyPOSUtil.RECEIPT_OFF);
    }
    payment.setPrintMerchantReceipt(args.printMerchantReceipt ? MyPOSUtil.RECEIPT_ON : MyPOSUtil.RECEIPT_OFF);

    MyPOSAPI.openPaymentActivity(activity, payment, requestCode);
  }

  static Currency currency(String code) {
    try {
      return Currency.valueOf(code);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Currency not supported by Glass: " + code);
    }
  }
}
