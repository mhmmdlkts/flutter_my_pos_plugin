package com.kreiseck.my_pos;

import com.mypos.smartsdk.print.PrinterStatus;

/** Print results as the Dart side knows them. */
final class PrintStatus {

  static final String SUCCESS = "SUCCESS";
  static final String FAILED = "FAILED";
  static final String OUT_OF_PAPER = "OUT_OF_PAPER";
  static final String UNKNOWN = "UNKNOWN";

  private PrintStatus() {}

  static String fromBroadcast(boolean printingStarted, int printerStatus) {
    if (!printingStarted) return FAILED;
    if (printerStatus == PrinterStatus.PRINTER_STATUS_SUCCESS) return SUCCESS;
    if (printerStatus == PrinterStatus.PRINTER_STATUS_OUT_OF_PAPER) return OUT_OF_PAPER;
    return FAILED;
  }
}
