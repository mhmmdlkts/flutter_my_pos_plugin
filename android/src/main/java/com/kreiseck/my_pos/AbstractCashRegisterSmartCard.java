package com.kreiseck.my_pos;

import android.content.Context;
import com.mypos.smartsdk.SAMCard;

import java.util.ArrayList;
import java.util.List;

import com.mypos.smartsdk.SAMCard;

public abstract class AbstractCashRegisterSmartCard implements ICashRegisterSmartCard {
	
	protected static final byte[] MASTER_FILE = new byte[] { 0x3F, 0x00 };

	protected byte[] atrBytes;
	protected Context context;
	protected int slotNumber;
	protected int timeoutMs;

	public void close() throws Exception {
		SAMCard.close(context, slotNumber, timeoutMs);
	}

	protected void executeSelectWithFileIdAPDU(byte[] fileID) throws Exception {

		CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x00, 0x0C, fileID);
		ResponseAPDU res = executeCommand(select);
		if(res.getSW() != 0x9000) {
			throw new Exception("Bad status " + res.getSW());
		}

	}

	protected ResponseAPDU selectWithAppliactionId(byte[] appliactionId) throws Exception {
		CommandAPDU select = new CommandAPDU(0x00, 0xA4, 0x04, 0x0C, appliactionId);
		return executeCommand(select);
	}

	protected ResponseAPDU executeCommand(CommandAPDU commandAPDU) throws Exception {

		ResponseAPDU responseAPDU = null;
		try {
			responseAPDU = transmit(commandAPDU);
			if (responseAPDU.getSW() != 0x9000 && responseAPDU.getSW() != 0x6A82) {
				throw new Exception("Response APDU status is " + responseAPDU.getSW());
			}
		} catch (Exception e) {
			throw new Exception(e);
		}
		return responseAPDU;
	}

	protected byte[] getData(CommandAPDU commandAPDU) throws Exception {

		try {
			ResponseAPDU responseAPDU = transmit(commandAPDU);
			if (responseAPDU.getSW() != 0x9000) {
				throw new Exception("Response APDU status is " + responseAPDU.getSW());
			} else {
				return responseAPDU.getData();
			}
		} catch (Exception e) {
			throw new Exception(e);
		}
	}

	protected List<byte[]> getBuffer(boolean onlyFirst, byte[] DF, byte[] EF) throws Exception {

		executeSelectWithFileIdAPDU(MASTER_FILE);
		executeSelectWithFileIdAPDU(DF);
		executeSelectWithFileIdAPDU(EF);
		int offset = 0;
		List<byte[]> dataList = new ArrayList<>(8);
		while (true) {
			ResponseAPDU resp = transmit(new CommandAPDU(0x00, 0xB0, 0x7F & (offset >> 8), offset & 0xFF, 256));
			if (resp.getSW() != 0x9000) {
				break;
			}
			dataList.add(resp.getData());
			if (onlyFirst) {
				break;
			}
			offset += 256;
		}
		return dataList;
	}

	protected ResponseAPDU transmit(CommandAPDU commandAPDU) throws Exception {
		byte[] responseBytes = SAMCard.isoCommand(context, slotNumber, timeoutMs, commandAPDU.getBytes());
		return new ResponseAPDU(responseBytes);
	}
}
