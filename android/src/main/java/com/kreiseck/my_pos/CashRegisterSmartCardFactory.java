package com.kreiseck.my_pos;

import android.content.Context;

import com.mypos.smartsdk.SAMCard;

public class CashRegisterSmartCardFactory {

	public static ICashRegisterSmartCard createInstance(Context context, int slotNumber, int timeoutMs) throws Exception {
		byte[] atrBytes = SAMCard.open(context, slotNumber, timeoutMs);
		ICashRegisterSmartCard toReturn;
		String atrHex = SmartCardUtil.byteArrayToHexString(atrBytes);

		if (atrHex.startsWith("3BBF11008131FE45455041")) {
			toReturn = new SmartCardACOS(atrBytes, context, slotNumber, timeoutMs);
		} else if (atrHex.startsWith("3BBF11008131FE454D4341")) {
			toReturn = new SmartCardACOS(atrBytes, context, slotNumber, timeoutMs);
		} else if (atrHex.startsWith("3BDF18008131FE588031B05202046405C903AC73B7B1D422")) {
			toReturn = new SmartCardCardOS(atrBytes, context, slotNumber, timeoutMs);
		} else if (atrHex.startsWith("3BDF18008131FE588031905241016405C903AC73B7B1D444")) {
			toReturn = new SmartCardCardOS(atrBytes, context, slotNumber, timeoutMs);
		} else if (atrHex.startsWith("3BDF96FF910131FE4680319052410264050200AC73D622C017")) {
			toReturn = new SmartCardAcosID(atrBytes, context, slotNumber, timeoutMs);
		} else if (atrHex.startsWith("3BDF18FF910131FE4680319052410264050200AC73D622C099")) {
			toReturn = new SmartCardAcosID(atrBytes, context, slotNumber, timeoutMs);
		} else if (atrHex.startsWith("3BDF97008131FE4680319052410364050201AC73D622C0F8")) {
			toReturn = new SmartCardAcosID(atrBytes, context, slotNumber, timeoutMs);
		} else {
			throw new Exception("Wrong card");
		}
		return toReturn;
	}

}
