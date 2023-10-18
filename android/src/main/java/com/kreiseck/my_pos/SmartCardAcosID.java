package com.kreiseck.my_pos;

import android.content.Context;

import java.math.BigInteger;
import java.util.List;

public class SmartCardAcosID extends AbstractCashRegisterSmartCard {

	private static final byte[] DF_SIG = new byte[] { (byte) 0xDF, 0x01 };

	private static final byte[] EF_CIN_CSN = new byte[] { (byte) 0xD0, 0x01 };
	private static final byte[] EF_C_CH_DS = new byte[] { (byte) 0xc0, 0x00 };

	private static final byte[] AID_SIG = new byte[] { (byte) 0xD0, 0x40, 0x00, 0x00, 0x22, 0x00, 0x01 };

	public SmartCardAcosID(byte[] atrBytes, Context context, int slotNumber, int timeoutMs) throws Exception {
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
			return doSignaturWithoutSelection(sha256Hash, pin);
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	@Override
	public byte[] doSignaturWithoutSelection(byte[] sha256Hash, String pin) throws Exception {

		try {
			byte[] ba = SmartCardUtil.getFormat2PIN(pin);
			// Response response3 = SendCase3APDU(isoReader, 0x00, 0x20, 0x00, 0x81, formatedPIN);
			byte[] data = new byte[] { (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x8A, (byte) 0x08, ba[0], ba[1],
					ba[2], ba[3], ba[4], ba[5], ba[6], ba[7] };
			CommandAPDU command = new CommandAPDU(data);
			executeCommand(command);
			command = new CommandAPDU(0x00, 0x2A, 0x9E, 0x9A, sha256Hash, 64);
			return getData(command);
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	@Override
	public String getCertificateSerialDec() throws Exception {

		BigInteger serial = getCertificateSerial();
		return serial.toString();
	}

	@Override
	public String getCertificateSerialHex() throws Exception {

		BigInteger serial = getCertificateSerial();
		return serial.toString(16);
	}

	@Override
	public String getCIN() throws Exception {
		executeSelectWithFileIdAPDU(MASTER_FILE);
		executeSelectWithFileIdAPDU(EF_CIN_CSN);
		CommandAPDU command3 = new CommandAPDU(0x00, 0xB0, 0x00, 0x00, 0x08);
		byte[] data = getData(command3);
		String cin = SmartCardUtil.byteArrayToHexString(data);
		return cin;
	}

	private BigInteger getCertificateSerial() throws Exception {

		List<byte[]> dataList = getBuffer(true, DF_SIG, EF_C_CH_DS);
		byte[] ba = dataList.get(0);
		int length = SmartCardUtil.byteToUnsignedint(ba[14]);
		BigInteger bi = BigInteger.valueOf(0);
		for (int i = 0; i < length; i++) {
			bi = bi.shiftLeft(8).add(BigInteger.valueOf(SmartCardUtil.byteToUnsignedint(ba[15 + i])));
		}
		return bi;
	}

	private boolean applicationsMissing() throws Exception {

		boolean applicationsMissing = false;
		ResponseAPDU response1 = selectWithAppliactionId(AID_SIG);
		if (response1.getSW() != 0x9000) {
			applicationsMissing = true;
		}
		return applicationsMissing;
	}
}
