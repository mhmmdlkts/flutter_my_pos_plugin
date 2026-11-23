package com.kreiseck.my_pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.mypos.smartsdk.Currency;
import com.mypos.smartsdk.MyPOSRefund;
import com.mypos.smartsdk.MyPOSUtil;
import com.mypos.smartsdk.MyPOSVoid;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;

/** Builds the real SDK requests, so the SDK's own checks apply. Values are invented. */
public class OperationArgsTest {

  private static MethodCall call(String method, Object... keyValues) {
    Map<String, Object> args = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      args.put((String) keyValues[i], keyValues[i + 1]);
    }
    return new MethodCall(method, args);
  }

  @Test
  public void void_carriesStanAuthCodeAndDateTime() {
    MyPOSVoid v = OperationArgs.voidOf(call("voidPayment",
        "stan", "000123", "authCode", " A1B2C3 ", "dateTime", "261123141500"));
    assertEquals(123, v.getSTAN());
    assertEquals("A1B2C3", v.getAuthCode());
    assertEquals("261123141500", v.getDateTime());
    assertFalse(v.getVoidLastTransactionFlag());
    assertEquals(MyPOSUtil.RECEIPT_OFF, v.getPrintMerchantReceipt());
    assertEquals(MyPOSUtil.RECEIPT_OFF, v.getPrintCustomerReceipt());
  }

  @Test
  public void void_stanMayComeAsNumber() {
    assertEquals(7, OperationArgs.voidOf(call("voidPayment", "stan", 7, "authCode", "X", "dateTime", "261123141500")).getSTAN());
  }

  @Test
  public void void_rejectsWhatTheSdkCannotVoid() {
    for (Object stan : new Object[] {null, "", " ", "0", "-3", "abc", 0, -1, 1.5}) {
      assertThrows(String.valueOf(stan), IllegalArgumentException.class,
          () -> OperationArgs.voidOf(call("voidPayment", "stan", stan, "authCode", "X", "dateTime", "261123141500")));
    }
    assertThrows(IllegalArgumentException.class,
        () -> OperationArgs.voidOf(call("voidPayment", "stan", "7", "authCode", " ", "dateTime", "261123141500")));
    assertThrows(IllegalArgumentException.class,
        () -> OperationArgs.voidOf(call("voidPayment", "stan", "7", "authCode", "X")));
  }

  @Test
  public void voidLast_needsNoPaymentData() {
    MyPOSVoid v = OperationArgs.voidLast(call("voidLastPayment"));
    assertTrue(v.getVoidLastTransactionFlag());
  }

  @Test
  public void receipts_onlyWhenAskedFor() {
    MyPOSVoid v = OperationArgs.voidOf(call("voidPayment", "stan", "7", "authCode", "X", "dateTime", "261123141500",
        "printMerchantReceipt", true, "printCustomerReceipt", false));
    assertEquals(MyPOSUtil.RECEIPT_ON, v.getPrintMerchantReceipt());
    assertEquals(MyPOSUtil.RECEIPT_OFF, v.getPrintCustomerReceipt());
  }

  @Test
  public void refund_centsBecomeEuroOnce() {
    MyPOSRefund r = OperationArgs.refundOf(call("refund", "amountCents", 2450, "currency", "eur",
        "printCustomerReceipt", true));
    assertEquals(24.50, r.getRefundAmount(), 0);
    assertEquals(Currency.EUR, r.getCurrency());
    assertEquals(MyPOSUtil.RECEIPT_ON, r.getPrintCustomerReceipt());
    assertEquals(MyPOSUtil.RECEIPT_OFF, r.getPrintMerchantReceipt());
    assertEquals(0.01, OperationArgs.refundOf(call("refund", "amountCents", 1, "currency", "EUR")).getRefundAmount(), 0);
    assertEquals(99999.99, OperationArgs.refundOf(call("refund", "amountCents", 9999999L, "currency", "EUR")).getRefundAmount(), 0);
  }

  @Test
  public void refund_rejectsBadAmountsAndCurrencies() {
    for (Object cents : new Object[] {null, 0, -5, 12.5, "100"}) {
      assertThrows(String.valueOf(cents), IllegalArgumentException.class,
          () -> OperationArgs.refundOf(call("refund", "amountCents", cents, "currency", "EUR")));
    }
    assertThrows(IllegalArgumentException.class,
        () -> OperationArgs.refundOf(call("refund", "amountCents", 100, "currency", "XYZ")));
    assertThrows(IllegalArgumentException.class,
        () -> OperationArgs.refundOf(call("refund", "amountCents", 100)));
  }

  @Test
  public void skipConfirmation_defaultsToShowingIt() {
    assertFalse(OperationArgs.skipConfirmation(call("refund")));
    assertTrue(OperationArgs.skipConfirmation(call("refund", "skipConfirmation", true)));
  }
}
