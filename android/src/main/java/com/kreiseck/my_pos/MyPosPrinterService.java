package com.kreiseck.my_pos;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.gson.Gson;
import com.mypos.smartsdk.MyPOSAPI;
import com.mypos.smartsdk.MyPOSUtil;
import com.mypos.smartsdk.print.PrinterCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Prints on the built-in printer of myPOS Smart terminals. */
final class MyPosPrinterService {

  private static final int DEFAULT_FONT_SIZE = 24;
  private static final int DEFAULT_QR_SIZE = 200;

  private MyPosPrinterService() {}

  /**
   * Turns the commands from Dart into the print broadcast. An image or QR code that cannot be
   * built is left out: a receipt without it is better than none.
   */
  static Intent buildPrintIntent(List<Map<String, Object>> data) {
    if (data == null) throw new IllegalArgumentException("data is required");
    List<PrinterCommand> commands = new ArrayList<>();
    for (Map<String, Object> command : data) {
      PrinterCommand printerCommand = toPrinterCommand(command);
      if (printerCommand != null) commands.add(printerCommand);
    }
    Intent intent = new Intent(MyPOSUtil.PRINT_BROADCAST);
    intent.putExtra("commands", new Gson().toJson(commands));
    return intent;
  }

  /**
   * Sends the job to the myPOS system app as explicit broadcast, as the SDK documents.
   * Returns false when no app on the device takes print jobs.
   */
  static boolean send(Context context, Intent intent) {
    List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(intent, 0);
    if (receivers == null || receivers.isEmpty()) return false;
    MyPOSAPI.sendExplicitBroadcast(context, intent);
    return true;
  }

  private static PrinterCommand toPrinterCommand(Map<String, Object> command) {
    String type = (String) command.get("type");
    if (type == null) return null;
    switch (type) {
      case "text":
        return new PrinterCommand(
            PrinterCommand.CommandType.TEXT,
            string(command.get("value")),
            integer(command.get("fontSize"), DEFAULT_FONT_SIZE),
            alignment(command.get("alignment")));
      case "doubleText":
        return new PrinterCommand(
            PrinterCommand.CommandType.TEXT,
            string(command.get("leftValue")),
            string(command.get("rightValue")),
            PrinterCommand.RECEIPT_SMART_MAX_CHARS_PER_LINE);
      case "space":
        return new PrinterCommand(PrinterCommand.CommandType.TEXT, "\n");
      case "image": {
        byte[] bytes;
        try {
          bytes = Base64.decode(string(command.get("value")), Base64.DEFAULT);
        } catch (IllegalArgumentException e) {
          return null;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap == null ? null : new PrinterCommand(PrinterCommand.CommandType.IMAGE, bitmap);
      }
      case "qrCode": {
        Bitmap qr = QRCodeGenerator.generateQRCode(
            string(command.get("value")), integer(command.get("size"), DEFAULT_QR_SIZE));
        return qr == null ? null : new PrinterCommand(PrinterCommand.CommandType.IMAGE, qr);
      }
      default:
        return null;
    }
  }

  private static String string(Object value) {
    return value == null ? "" : value.toString();
  }

  static int integer(Object value, int fallback) {
    return value instanceof Number ? ((Number) value).intValue() : fallback;
  }

  static PrinterCommand.Alignment alignment(Object value) {
    PrinterCommand.Alignment[] all = PrinterCommand.Alignment.values();
    int index = integer(value, 0);
    return index >= 0 && index < all.length ? all[index] : all[0];
  }
}
