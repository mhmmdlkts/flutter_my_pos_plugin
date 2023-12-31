package com.kreiseck.my_pos;

import android.content.Context;

import java.util.List;

public class SmartCardACOS extends AbstractCashRegisterSmartCard {

	private static final byte[] AID_SIG = new byte[] { (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x18,
			(byte) 0x45, (byte) 0x43 };
	private static final byte[] AID_DEC = new byte[] { (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x18,
			(byte) 0x45, (byte) 0x4E };
	private static final byte[] DF_SIG = new byte[] { (byte) 0xDF, 0x70 };
	private static final byte[] DF_DEC = new byte[] { (byte) 0xDF, 0x71 };
	private static final byte[] EF_C_CH_DS = new byte[] { (byte) 0xc0, (byte) 0x02 };
	private static final byte[] EF_CIN_CSN = new byte[] { (byte) 0xD0, 0x01 };
	private static final byte[] TLV = new byte[] { (byte) 0x84, (byte) 0x01, (byte) 0x88, (byte) 0x80, (byte) 0x01,
			(byte) 0x44 };

	public SmartCardACOS(byte[] atrBytes, Context context, int slotNumber, int timeoutMs) throws Exception {
		this.atrBytes = atrBytes;
		this.context = context;
		this.slotNumber = slotNumber;
		this.timeoutMs = timeoutMs;
		if (applicationsMissing()) {
			throw new Exception("Wrong card");
		}
	}

	@Override
	public byte[] doSignatur(byte[] sha256Hash, String pin) throws Exception {

		try {
			executeSelectWithFileIdAPDU(MASTER_FILE);
			executeSelectWithFileIdAPDU(DF_SIG);
			CommandAPDU command = new CommandAPDU(0x00, 0x22, 0x41, 0xb6, TLV);
			executeCommand(command);
			return doSignaturWithoutSelection(sha256Hash, pin);
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	@Override
	public byte[] doSignaturWithoutSelection(byte[] sha256Hash, String pinS) throws Exception {

		byte[] pin = SmartCardUtil.getFormat1PIN(pinS);
		try {
			CommandAPDU command;
			byte[] data = new byte[] { (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x81, (byte) 0x08, pin[0], pin[1],
					pin[2], pin[3], pin[4], pin[5], pin[6], pin[7] };
			command = new CommandAPDU(data);
			executeCommand(command);
			command = new CommandAPDU(0x00, 0x2A, 0x90, 0x81, sha256Hash);
			executeCommand(command);
			command = new CommandAPDU(0x00, 0x2A, 0x9E, 0x9A, 256);
			return getData(command);
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	@Override
	public String getCertificateSerialDec() throws Exception {

		long serial = getCertificateSerial();
		return Long.toString(serial);
	}

	@Override
	public String getCertificateSerialHex() throws Exception {

		long serial = getCertificateSerial();
		return Long.toHexString(serial);
	}

	@Override
	public String getCIN() throws Exception {

		executeSelectWithFileIdAPDU(DF_DEC);
		executeSelectWithFileIdAPDU(EF_CIN_CSN);
		CommandAPDU command3 = new CommandAPDU(0x00, 0xB0, 0x00, 0x00, 0x08);
		byte[] data = getData(command3);
		String cin = SmartCardUtil.byteArrayToHexString(data);
		return cin;
	}

	private long getCertificateSerial() throws Exception {

		List<byte[]> dataList = getBuffer(true, DF_SIG, EF_C_CH_DS);
		byte[] ba = dataList.get(0);
		int length = SmartCardUtil.byteToUnsignedint(ba[14]);
		long res = 0;
		for (int i = 0; i < length; i++) {
			res = (res << 8) + SmartCardUtil.byteToUnsignedint(ba[15 + i]);
		}
		return res;
	}

	private boolean applicationsMissing() throws Exception {

		boolean applicationsMissing = false;
		ResponseAPDU response1 = selectWithAppliactionId(AID_DEC);
		if (response1.getSW() != 0x9000) {
			applicationsMissing = true;
		}
		ResponseAPDU response2 = selectWithAppliactionId(AID_SIG);
		if (response2.getSW() != 0x9000) {
			applicationsMissing = true;
		}
		return applicationsMissing;
	}
}
