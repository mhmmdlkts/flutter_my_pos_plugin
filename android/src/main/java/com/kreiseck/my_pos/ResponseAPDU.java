package com.kreiseck.my_pos;

public class ResponseAPDU {

    private byte[] data;
    private int sw;

    public ResponseAPDU(byte[] response) {
        if (response.length < 2) {
            throw new IllegalArgumentException("Invalid APDU response");
        }

        if (response.length > 2) {
            data = new byte[response.length - 2];
            System.arraycopy(response, 0, data, 0, response.length - 2);
        }

        sw = ((response[response.length - 2] & 0xFF) << 8) | (response[response.length - 1] & 0xFF);
    }

    public int getSW() {
        return sw;
    }

    public byte[] getData() {
        return data;
    }
}
