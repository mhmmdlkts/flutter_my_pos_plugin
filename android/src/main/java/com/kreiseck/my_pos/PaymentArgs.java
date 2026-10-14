package com.kreiseck.my_pos;

import androidx.annotation.Nullable;

import io.flutter.plugin.common.MethodCall;

/** Arguments of a payment call, checked before the myPOS activity is opened. */
final class PaymentArgs {

  final double amount;
  final String currency;
  @Nullable final String reference;
  @Nullable final String eReceiptReceiver;
  final boolean giftCardTransaction;
  final boolean fixedPinpad;
  final boolean printCustomerReceipt;
  final boolean printMerchantReceipt;

  PaymentArgs(double amount, String currency, @Nullable String reference, @Nullable String eReceiptReceiver,
              boolean giftCardTransaction, boolean fixedPinpad,
              boolean printCustomerReceipt, boolean printMerchantReceipt) {
    if (Double.isNaN(amount) || Double.isInfinite(amount) || amount <= 0) {
      throw new IllegalArgumentException("amount must be a positive number");
    }
    if (currency == null || currency.trim().isEmpty()) {
      throw new IllegalArgumentException("currency is required");
    }
    this.amount = amount;
    this.currency = currency.trim().toUpperCase(java.util.Locale.ROOT);
    this.reference = emptyToNull(reference);
    this.eReceiptReceiver = emptyToNull(eReceiptReceiver);
    this.giftCardTransaction = giftCardTransaction;
    this.fixedPinpad = fixedPinpad;
    this.printCustomerReceipt = printCustomerReceipt;
    this.printMerchantReceipt = printMerchantReceipt;
  }

  static PaymentArgs from(MethodCall call) {
    Object amount = call.argument("amount");
    if (!(amount instanceof Number)) {
      throw new IllegalArgumentException("amount is required");
    }
    return new PaymentArgs(
        ((Number) amount).doubleValue(),
        call.argument("currency"),
        call.argument("reference"),
        call.argument("eReceiptReceiver"),
        flag(call, "giftCardTransaction", false),
        flag(call, "fixedPinpad", false),
        flag(call, "printCustomerReceipt", true),
        flag(call, "printMerchantReceipt", true));
  }

  private static boolean flag(MethodCall call, String key, boolean fallback) {
    Boolean value = call.argument(key);
    return value != null ? value : fallback;
  }

  @Nullable
  private static String emptyToNull(@Nullable String value) {
    return value == null || value.isEmpty() ? null : value;
  }
}
