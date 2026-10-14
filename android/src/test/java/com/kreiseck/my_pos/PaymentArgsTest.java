package com.kreiseck.my_pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import com.mypos.smartsdk.Currency;
import com.mypos.smartsdk.MyPOSUtil;
import com.mypos.smartsdk.print.PrinterStatus;

import org.junit.Test;

public class PaymentArgsTest {

  private static PaymentArgs args(double amount, String currency, boolean printCustomer, String eReceipt) {
    return new PaymentArgs(amount, currency, "", eReceipt, false, false, printCustomer, true);
  }

  @Test
  public void rejectsAmountsThatCannotBeCharged() {
    assertThrows(IllegalArgumentException.class, () -> args(0, "EUR", true, null));
    assertThrows(IllegalArgumentException.class, () -> args(-1, "EUR", true, null));
    assertThrows(IllegalArgumentException.class, () -> args(Double.NaN, "EUR", true, null));
    assertThrows(IllegalArgumentException.class, () -> args(1, " ", true, null));
  }

  @Test
  public void normalisesCurrencyAndEmptyTexts() {
    PaymentArgs a = args(12.5, "eur", true, "");
    assertEquals("EUR", a.currency);
    assertNull(a.reference);
    assertNull(a.eReceiptReceiver);
  }

  @Test
  public void currency_unknownCodeIsAnError_neverEur() {
    assertEquals(Currency.ISK, MyPosPaymentService.currency("ISK"));
    assertEquals(Currency.EUR, MyPosPaymentService.currency("EUR"));
    assertThrows(IllegalArgumentException.class, () -> MyPosPaymentService.currency("XYZ"));
  }

  @Test
  public void customerReceipt_paperThenEReceiptThenOff() {
    assertEquals(MyPOSUtil.RECEIPT_ON, MyPosPaymentService.customerReceipt(args(1, "EUR", true, "a@b.at")));
    assertEquals(MyPOSUtil.RECEIPT_E_RECEIPT, MyPosPaymentService.customerReceipt(args(1, "EUR", false, "a@b.at")));
    assertEquals(MyPOSUtil.RECEIPT_OFF, MyPosPaymentService.customerReceipt(args(1, "EUR", false, null)));
  }

  @Test
  public void printStatus_fromBroadcast() {
    assertEquals(PrintStatus.FAILED, PrintStatus.fromBroadcast(false, PrinterStatus.PRINTER_STATUS_SUCCESS));
    assertEquals(PrintStatus.SUCCESS, PrintStatus.fromBroadcast(true, PrinterStatus.PRINTER_STATUS_SUCCESS));
    assertEquals(PrintStatus.OUT_OF_PAPER, PrintStatus.fromBroadcast(true, PrinterStatus.PRINTER_STATUS_OUT_OF_PAPER));
    assertEquals(PrintStatus.FAILED, PrintStatus.fromBroadcast(true, PrinterStatus.PRINTER_STATUS_UNKNOWN_ERROR));
  }
}
