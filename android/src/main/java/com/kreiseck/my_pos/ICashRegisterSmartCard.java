package com.kreiseck.my_pos;

public interface ICashRegisterSmartCard {

	byte[] doSignatur(byte[] sha256Hash, String pin) throws Exception;

	byte[] doSignaturWithoutSelection(byte[] sha256Hash, String pin) throws Exception;

	String getCertificateSerialDec() throws Exception;

	String getCertificateSerialHex() throws Exception;

	String getCIN() throws Exception;

	void close() throws Exception;
}
