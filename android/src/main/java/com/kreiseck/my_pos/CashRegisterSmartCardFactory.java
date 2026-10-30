package com.kreiseck.my_pos;

import android.content.Context;

import com.mypos.smartsdk.SAMCard;

public class CashRegisterSmartCardFactory {

	/** The A-Trust card families this plugin speaks to. */
	enum CardFamily { ACOS, CARDOS, ACOS_ID }

	// ATRs as in A-Trust's APDU examples (github.com/A-Trust/RKSV, CashRegisterSmartCardFactory, 26.03.2025).
	private static final String ACOS_EPA = "3BBF11008131FE45455041";
	private static final String ACOS_MCA = "3BBF11008131FE454D4341";
	private static final String CARDOS_53_A = "3BDF18008131FE588031B05202046405C903AC73B7B1D422";
	private static final String CARDOS_53_B = "3BDF18008131FE588031905241016405C903AC73B7B1D444";
	// ACOS-ID v2.0 (cold, warm) and v2.1: no longer issued by A-Trust
	private static final String ACOSID_ATR_V2_COLD = "3BDF96FF910131FE4680319052410264050200AC73D622C017";
	private static final String ACOSID_ATR_V2_WARM = "3BDF18FF910131FE4680319052410264050200AC73D622C099";
	private static final String ACOSID_ATR_V2_1 = "3BDF97008131FE4680319052410364050201AC73D622C0F8";
	// ACOS-ID v4.1, the current card
	private static final String ACOSID_ATR_V4_1 = "3BDF97008131FE4680319052410464050401AC73D622C0F9";

	/** The card family of an ATR (hex, upper case), null for a card this plugin does not know. */
	static CardFamily familyFor(String atrHex) {
		if (atrHex.startsWith(ACOS_EPA) || atrHex.startsWith(ACOS_MCA)) {
			return CardFamily.ACOS;
		}
		if (atrHex.startsWith(CARDOS_53_A) || atrHex.startsWith(CARDOS_53_B)) {
			return CardFamily.CARDOS;
		}
		if (atrHex.startsWith(ACOSID_ATR_V2_COLD) || atrHex.startsWith(ACOSID_ATR_V2_WARM)
				|| atrHex.startsWith(ACOSID_ATR_V2_1) || atrHex.startsWith(ACOSID_ATR_V4_1)) {
			return CardFamily.ACOS_ID;
		}
		return null;
	}

	public static ICashRegisterSmartCard createInstance(Context context, int slotNumber, int timeoutMs) throws Exception {
		byte[] atrBytes = SAMCard.open(context, slotNumber, timeoutMs);
		CardFamily family = familyFor(SmartCardUtil.byteArrayToHexString(atrBytes));
		if (family == null) {
			throw new Exception("Wrong card");
		}
		switch (family) {
			case ACOS:
				return new SmartCardACOS(atrBytes, context, slotNumber, timeoutMs);
			case CARDOS:
				return new SmartCardCardOS(atrBytes, context, slotNumber, timeoutMs);
			default:
				return new SmartCardAcosID(atrBytes, context, slotNumber, timeoutMs);
		}
	}

}
