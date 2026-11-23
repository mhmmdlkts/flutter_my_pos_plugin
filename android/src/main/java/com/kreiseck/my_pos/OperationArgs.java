package com.kreiseck.my_pos;

import androidx.annotation.Nullable;

import com.mypos.smartsdk.MyPOSRefund;
import com.mypos.smartsdk.MyPOSUtil;
import com.mypos.smartsdk.MyPOSVoid;

import java.util.UUID;

import io.flutter.plugin.common.MethodCall;

/**
 * Checks the arguments of a void or refund and builds the SDK request. Every problem is an
 * {@link IllegalArgumentException}, thrown before the terminal is opened.
 */
final class OperationArgs {

  private OperationArgs() {}

  /** Void of one payment, identified by STAN, authorization code and date/time of the payment. */
  static MyPOSVoid voidOf(MethodCall call) {
    int stan = stan(call.argument("stan"));
    String authCode = required(call.argument("authCode"), "authCode");
    String dateTime = required(call.argument("dateTime"), "dateTime");
    try {
      MyPOSVoid request = MyPOSVoid.builder()
          .STAN(stan)
          .authCode(authCode)
          .dateTime(dateTime)
          .foreignTransactionId(UUID.randomUUID().toString())
          .build();
      return withReceipts(request, call);
    } catch (Exception e) {
      throw new IllegalArgumentException(String.valueOf(e.getMessage()));
    }
  }

  /** Void of the last payment on this terminal; needs no payment data. */
  static MyPOSVoid voidLast(MethodCall call) {
    try {
      MyPOSVoid request = MyPOSVoid.builder()
          .voidLastTransactionFlag(true)
          .foreignTransactionId(UUID.randomUUID().toString())
          .build();
      return withReceipts(request, call);
    } catch (Exception e) {
      throw new IllegalArgumentException(String.valueOf(e.getMessage()));
    }
  }

  /** Refund of an amount in cents to a card that is presented again. */
  static MyPOSRefund refundOf(MethodCall call) {
    Object cents = call.argument("amountCents");
    if (!(cents instanceof Integer) && !(cents instanceof Long)) {
      throw new IllegalArgumentException("amountCents is required");
    }
    long amountCents = ((Number) cents).longValue();
    if (amountCents <= 0) {
      throw new IllegalArgumentException("amountCents must be positive");
    }
    String currency = call.argument("currency");
    MyPOSRefund request;
    try {
      request = MyPOSRefund.builder()
          .refundAmount(amountCents / 100.0)
          .currency(MyPosPaymentService.currency(currency == null ? "" : currency.trim().toUpperCase(java.util.Locale.ROOT)))
          .foreignTransactionId(UUID.randomUUID().toString())
          .build();
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException(String.valueOf(e.getMessage()));
    }
    request.setPrintMerchantReceipt(receipt(call, "printMerchantReceipt"));
    request.setPrintCustomerReceipt(receipt(call, "printCustomerReceipt"));
    return request;
  }

  /** Whether the terminal skips its own confirmation screen; defaults to showing it. */
  static boolean skipConfirmation(MethodCall call) {
    Boolean skip = call.argument("skipConfirmation");
    return skip != null && skip;
  }

  private static MyPOSVoid withReceipts(MyPOSVoid request, MethodCall call) {
    request.setPrintMerchantReceipt(receipt(call, "printMerchantReceipt"));
    request.setPrintCustomerReceipt(receipt(call, "printCustomerReceipt"));
    return request;
  }

  /** Receipts are off unless asked for. */
  private static int receipt(MethodCall call, String key) {
    Boolean on = call.argument(key);
    return on != null && on ? MyPOSUtil.RECEIPT_ON : MyPOSUtil.RECEIPT_OFF;
  }

  /** The SDK needs the STAN as a number other than 0; the terminal reports it as text. */
  static int stan(@Nullable Object value) {
    if (value instanceof Integer) {
      int stan = (Integer) value;
      if (stan > 0) return stan;
    } else if (value instanceof String) {
      try {
        int stan = Integer.parseInt(((String) value).trim());
        if (stan > 0) return stan;
      } catch (NumberFormatException ignored) {
        // Not a number: reported below.
      }
    }
    throw new IllegalArgumentException("stan must be a positive number");
  }

  private static String required(@Nullable Object value, String name) {
    if (value instanceof String && !((String) value).trim().isEmpty()) {
      return ((String) value).trim();
    }
    throw new IllegalArgumentException(name + " is required");
  }
}
