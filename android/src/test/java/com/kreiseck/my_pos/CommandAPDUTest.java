package com.kreiseck.my_pos;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/** The encodings the signature cards receive must stay byte for byte the same. */
public class CommandAPDUTest {

  private static byte[] bytes(int... values) {
    byte[] out = new byte[values.length];
    for (int i = 0; i < values.length; i++) out[i] = (byte) values[i];
    return out;
  }

  @Test
  public void case2_readBinaryWithLe() {
    assertArrayEquals(bytes(0x00, 0xB0, 0x00, 0x00, 0x08),
        new CommandAPDU(0x00, 0xB0, 0x00, 0x00, 0x08).getBytes());
  }

  @Test
  public void case2_le256IsEncodedAsZero() {
    assertArrayEquals(bytes(0x00, 0x2A, 0x9E, 0x9A, 0x00),
        new CommandAPDU(0x00, 0x2A, 0x9E, 0x9A, 256).getBytes());
  }

  @Test
  public void case3_selectFile() {
    assertArrayEquals(bytes(0x00, 0xA4, 0x00, 0x0C, 0x02, 0x3F, 0x00),
        new CommandAPDU(0x00, 0xA4, 0x00, 0x0C, bytes(0x3F, 0x00)).getBytes());
  }

  @Test
  public void case4_signHashExpecting64Bytes() {
    byte[] hash = new byte[32];
    for (int i = 0; i < hash.length; i++) hash[i] = (byte) i;
    byte[] apdu = new CommandAPDU(0x00, 0x2A, 0x9E, 0x9A, hash, 64).getBytes();

    byte[] expected = new byte[4 + 1 + 32 + 1];
    System.arraycopy(bytes(0x00, 0x2A, 0x9E, 0x9A, 0x20), 0, expected, 0, 5);
    System.arraycopy(hash, 0, expected, 5, 32);
    expected[37] = 0x40;
    assertArrayEquals(expected, apdu);
  }

  @Test
  public void emptyDataAndNoLe_isHeaderOnly() {
    assertArrayEquals(bytes(0x00, 0xA4, 0x04, 0x0C),
        new CommandAPDU(0x00, 0xA4, 0x04, 0x0C, new byte[0]).getBytes());
  }

  @Test
  public void rawApdu_isSentUnchanged() {
    byte[] raw = bytes(0x00, 0x20, 0x00, 0x81, 0x08, 1, 2, 3, 4, 5, 6, 7, 8);
    assertArrayEquals(raw, new CommandAPDU(raw).getBytes());
  }

  @Test
  public void rejectsWhatShortApdusCannotCarry() {
    assertThrows(IllegalArgumentException.class, () -> new CommandAPDU(new byte[3]));
    assertThrows(IllegalArgumentException.class, () -> new CommandAPDU(0, 0, 0, 0, new byte[256]));
    assertThrows(IllegalArgumentException.class, () -> new CommandAPDU(0, 0, 0, 0, 257));
  }
}
