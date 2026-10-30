package com.kreiseck.my_pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/** The card family by ATR, as in A-Trust's APDU examples (A-Trust/RKSV, CashRegisterSmartCardFactory, 26.03.2025). */
public class CashRegisterSmartCardFactoryTest {

  @Test
  public void acos() {
    assertEquals(CashRegisterSmartCardFactory.CardFamily.ACOS, CashRegisterSmartCardFactory.familyFor("3BBF11008131FE45455041"));
    assertEquals(CashRegisterSmartCardFactory.CardFamily.ACOS, CashRegisterSmartCardFactory.familyFor("3BBF11008131FE454D4341"));
  }

  @Test
  public void cardOs() {
    assertEquals(CashRegisterSmartCardFactory.CardFamily.CARDOS,
        CashRegisterSmartCardFactory.familyFor("3BDF18008131FE588031B05202046405C903AC73B7B1D422"));
    assertEquals(CashRegisterSmartCardFactory.CardFamily.CARDOS,
        CashRegisterSmartCardFactory.familyFor("3BDF18008131FE588031905241016405C903AC73B7B1D444"));
  }

  @Test
  public void acosIdUpTo21() {
    for (String atr : new String[] {"3BDF96FF910131FE4680319052410264050200AC73D622C017",
        "3BDF18FF910131FE4680319052410264050200AC73D622C099", "3BDF97008131FE4680319052410364050201AC73D622C0F8"}) {
      assertEquals(atr, CashRegisterSmartCardFactory.CardFamily.ACOS_ID, CashRegisterSmartCardFactory.familyFor(atr));
    }
  }

  /** the current A-Trust card, added by A-Trust on 25.03.2025; before, the plugin answered "Wrong card" */
  @Test
  public void acosId41() {
    assertEquals(CashRegisterSmartCardFactory.CardFamily.ACOS_ID,
        CashRegisterSmartCardFactory.familyFor("3BDF97008131FE4680319052410464050401AC73D622C0F9"));
  }

  @Test
  public void unknownCard() {
    assertNull(CashRegisterSmartCardFactory.familyFor("3B8F8001804F0CA000000306030001000000006A"));
  }
}
