package com.kreiseck.my_pos;

/**
 * Short ISO 7816-4 command APDU (Lc up to 255 bytes, Le up to 256).
 *
 * The signature cards used here never need extended length, so only the short cases are built:
 * case 1 (header only), case 2 (Le), case 3 (data) and case 4 (data and Le). Le = 256 is
 * encoded as 0x00.
 */
public class CommandAPDU {

  private final byte[] bytes;

  /** A complete APDU, sent as is. */
  public CommandAPDU(byte[] apdu) {
    if (apdu == null || apdu.length < 4) {
      throw new IllegalArgumentException("APDU must have at least 4 bytes");
    }
    this.bytes = apdu.clone();
  }

  /** Case 2: expects up to {@code ne} response bytes. */
  public CommandAPDU(int cla, int ins, int p1, int p2, int ne) {
    this(cla, ins, p1, p2, null, ne);
  }

  /** Case 3: sends {@code data}, expects no response data. */
  public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data) {
    this(cla, ins, p1, p2, data, 0);
  }

  /** Case 4 (or 1 to 3 when data is empty or ne is 0). */
  public CommandAPDU(int cla, int ins, int p1, int p2, byte[] data, int ne) {
    int nc = data == null ? 0 : data.length;
    if (nc > 255) throw new IllegalArgumentException("data longer than 255 bytes");
    if (ne < 0 || ne > 256) throw new IllegalArgumentException("ne must be between 0 and 256");

    int length = 4 + (nc > 0 ? 1 + nc : 0) + (ne > 0 ? 1 : 0);
    byte[] apdu = new byte[length];
    apdu[0] = (byte) cla;
    apdu[1] = (byte) ins;
    apdu[2] = (byte) p1;
    apdu[3] = (byte) p2;
    int pos = 4;
    if (nc > 0) {
      apdu[pos++] = (byte) nc;
      System.arraycopy(data, 0, apdu, pos, nc);
      pos += nc;
    }
    if (ne > 0) {
      apdu[pos] = (byte) (ne == 256 ? 0 : ne);
    }
    this.bytes = apdu;
  }

  /** A copy of the encoded APDU. */
  public byte[] getBytes() {
    return bytes.clone();
  }
}
