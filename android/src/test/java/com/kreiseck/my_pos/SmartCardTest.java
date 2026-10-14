package com.kreiseck.my_pos;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class SmartCardTest {

  @Test
  public void format2Pin_fourDigits() throws Exception {
    assertArrayEquals(new byte[] {0x24, 0x12, 0x34, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF},
        SmartCardUtil.getFormat2PIN("1234"));
  }

  @Test
  public void format2Pin_sixDigits() throws Exception {
    assertArrayEquals(new byte[] {0x26, 0x12, 0x34, 0x56, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF},
        SmartCardUtil.getFormat2PIN("123456"));
  }

  @Test
  public void format1Pin_isAsciiPaddedWithZeros() throws Exception {
    assertArrayEquals(new byte[] {'1', '2', '3', '4', 0, 0, 0, 0}, SmartCardUtil.getFormat1PIN("1234"));
  }

  @Test
  public void pin_mustBeFourOrSixDigits() {
    assertThrows(Exception.class, () -> SmartCardUtil.getFormat2PIN("12345"));
    assertThrows(Exception.class, () -> SmartCardUtil.getFormat2PIN("12a4"));
    assertThrows(Exception.class, () -> SmartCardUtil.getFormat1PIN(null));
  }

  @Test
  public void responseApdu_splitsDataAndStatusWord() {
    ResponseAPDU response = new ResponseAPDU(new byte[] {0x01, 0x02, (byte) 0x90, 0x00});
    assertEquals(0x9000, response.getSW());
    assertArrayEquals(new byte[] {0x01, 0x02}, response.getData());
  }

  @Test
  public void responseApdu_statusOnly() {
    ResponseAPDU response = new ResponseAPDU(new byte[] {0x6A, (byte) 0x82});
    assertEquals(0x6A82, response.getSW());
    assertNull(response.getData());
  }

  @Test
  public void hex_isUpperCase() {
    assertEquals("00AB7F", SmartCardUtil.byteArrayToHexString(new byte[] {0x00, (byte) 0xAB, 0x7F}));
  }
}
