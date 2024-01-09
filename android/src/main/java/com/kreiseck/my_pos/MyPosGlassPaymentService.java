package com.kreiseck.my_pos;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.mypos.glasssdk.Currency;
import com.mypos.glasssdk.MyPOSAPI;
import com.mypos.glasssdk.MyPOSPayment;
import com.mypos.glasssdk.MyPOSUtil;
import com.mypos.glasssdk.ReferenceType;

import java.util.UUID;

public class MyPosGlassPaymentService {


    public static void showToast(Activity context, final String toast) {
        context.runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void startPayment(
            @NonNull Activity context,
            @NonNull double amount,
            @NonNull String currency,
            String reference,
            String eReceiptReceiver,
            boolean printCustomerReceipt,
            boolean printMerchantReceipt
    ) {
        try {
            MyPOSPayment payment = MyPOSPayment.builder()
                    .productAmount(amount)
                    .currency(getCurrency(currency))
                    .foreignTransactionId(UUID.randomUUID().toString())
                    .build();

            if (reference != null && !reference.isEmpty()) {
                payment.setReference(reference, ReferenceType.REFERENCE_NUMBER);
            }

            if (printCustomerReceipt) {
                payment.setPrintCustomerReceipt(MyPOSUtil.RECEIPT_ON);
            } else {
                if (eReceiptReceiver != null && !eReceiptReceiver.isEmpty()) {
                    payment.setEReceiptReceiver(eReceiptReceiver);
                    payment.setPrintCustomerReceipt(MyPOSUtil.RECEIPT_E_RECEIPT);
                } else {
                    payment.setPrintCustomerReceipt(MyPOSUtil.RECEIPT_OFF);
                }
            }

            if (printMerchantReceipt) {
                payment.setPrintMerchantReceipt(MyPOSUtil.RECEIPT_ON);
            } else {
                payment.setPrintMerchantReceipt(MyPOSUtil.RECEIPT_OFF);
            }


            MyPOSAPI.openPaymentActivity(context, payment, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Currency getCurrency(String curCode) {
        switch (curCode.toUpperCase()) {
            case "EUR":
                return Currency.EUR;
            case "USD":
                return Currency.USD;
            case "GBP":
                return Currency.GBP;
            case "CHF":
                return Currency.CHF;
            case "BGN":
                return Currency.BGN;
            case "RON":
                return Currency.RON;
            case "HRK":
                return Currency.HRK;
            case "CZK":
                return Currency.CZK;
            case "DKK":
                return Currency.DKK;
            case "HUF":
                return Currency.HUF;
            case "NOK":
                return Currency.NOK;
            case "PLN":
                return Currency.PLN;
            case "SEK":
                return Currency.SEK;
        }
        return Currency.EUR;
    }
}
