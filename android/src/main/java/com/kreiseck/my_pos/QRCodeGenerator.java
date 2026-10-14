package com.kreiseck.my_pos;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

final class QRCodeGenerator {

  private QRCodeGenerator() {}

  /** Black and white QR code of {@code size} x {@code size} pixels, or null if it cannot be built. */
  static Bitmap generateQRCode(String value, int size) {
    if (value == null || value.isEmpty() || size <= 0) return null;
    try {
      BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
      int width = matrix.getWidth();
      int height = matrix.getHeight();
      int[] pixels = new int[width * height];
      for (int y = 0; y < height; y++) {
        int row = y * width;
        for (int x = 0; x < width; x++) {
          pixels[row + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
        }
      }
      Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
      bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
      return bitmap;
    } catch (WriterException | IllegalArgumentException e) {
      return null;
    }
  }
}
