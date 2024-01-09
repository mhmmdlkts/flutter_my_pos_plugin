package com.kreiseck.my_pos;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import java.util.Map;

import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import android.content.Intent;
import android.widget.Toast;
import android.app.Activity;


import com.mypos.smartsdk.TransactionProcessingResult;
import com.mypos.smartsdk.Currency;
import com.mypos.smartsdk.MyPOSAPI;
import com.mypos.smartsdk.MyPOSPayment;
import com.mypos.smartsdk.MyPOSUtil;
import com.mypos.smartsdk.ReferenceType;
import com.mypos.smartsdk.TransactionProcessingResult;
import com.mypos.smartsdk.SAMCard;
import com.mypos.smartsdk.print.PrinterStatus;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.Map;
import java.util.List;

/** MyPosPlugin */
public class MyPosPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  private Activity activity;
  private Result pendingResult;


  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "my_pos");
    channel.setMethodCallHandler(this);
  }

  private BroadcastReceiver myReceiver = new BroadcastReceiver() {

    @Override
    public void onReceive(Context context, Intent intent) {
      boolean printing_started = intent.getBooleanExtra("printing_started", false);
      int printer_status = intent.getIntExtra("printer_status", PrinterStatus.PRINTER_STATUS_UNKNOWN_ERROR);

      // If the printing has actually started, handle the status
      if (printing_started) {

        // Handle success and errors
        if (printer_status == PrinterStatus.PRINTER_STATUS_SUCCESS) {
          pendingResult.success("SUCCESS");
        } else if (printer_status == PrinterStatus.PRINTER_STATUS_OUT_OF_PAPER) {
          pendingResult.success("OUT_OF_PAPER");
        } else {
          pendingResult.success("FAILED");
        }
        // etc.
      } else {
        pendingResult.success("FAILED");
      }
    }
  };

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == 1) {

      if (resultCode == Activity.RESULT_OK) {
        if (data == null) {

          if (pendingResult != null) {
            pendingResult.success("CANCEL");
            pendingResult = null;
          }
          return false;
        }
        int transactionResult = data.getIntExtra("status", TransactionProcessingResult.TRANSACTION_FAILED);
        if (transactionResult == TransactionProcessingResult.TRANSACTION_SUCCESS) {

          boolean transaction_approved = data.getBooleanExtra("transaction_approved", false);

          if (transaction_approved) {
            if (pendingResult != null) {
              pendingResult.success("SUCCESS");
              pendingResult = null;
            }
            return true;
          } else {
            if (pendingResult != null) {
              pendingResult.success("DECLINED");
              pendingResult = null;
            }
            return false;
          }
        }
      } else {
        if (pendingResult != null) {
          pendingResult.success("CANCEL");
          pendingResult = null;
        }
        return false;
      }
    }
    if (pendingResult != null) {
      pendingResult.success("CANCEL");
      pendingResult = null;
    }
    return false;
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("makePayment")) {
      pendingResult = result;
      MyPosPaymentService.startPayment(
              activity,
              call.argument("amount"),
              call.argument("currency"),
              call.argument("reference"),
              call.argument("eReceiptReceiver"),
              call.argument("giftCardTransaction"),
              call.argument("fixedPinpad"),
              call.argument("printCustomerReceipt"),
              call.argument("printMerchantReceipt")
      );
      // makePayment(result);
    } else if (call.method.equals("makeGlassPayment")) {
      pendingResult = result;
      MyPosGlassPaymentService.startPayment(
            activity,
            call.argument("amount"),
            call.argument("currency"),
            call.argument("reference"),
            call.argument("eReceiptReceiver"),
            call.argument("printCustomerReceipt"),
            call.argument("printMerchantReceipt")
      );
      // makePayment(result);
    } else if (call.method.equals("getCertificateSerialHex")) {
      int samSlot = call.argument("samSlot");
      int timeoutMs = call.argument("timeoutMs");
      getCertificateSerialHex(samSlot, timeoutMs, result);
    } else if (call.method.equals("getCertificateSerialDec")) {
      int samSlot = call.argument("samSlot");
      int timeoutMs = call.argument("timeoutMs");
      getCertificateSerialDec(samSlot, timeoutMs, result);
    } else if (call.method.equals("getCIN")) {
      int samSlot = call.argument("samSlot");
      int timeoutMs = call.argument("timeoutMs");
      getCIN(samSlot, timeoutMs, result);
    } else if (call.method.equals("doSignature")) {
      String data = call.argument("data");
      String pin = call.argument("pin");
      int samSlot = call.argument("samSlot");
      int timeoutMs = call.argument("timeoutMs");
      doSignature(data, samSlot, pin, timeoutMs, result);
    }  else if (call.method.equals("doSignatureWithoutSelection")) {
      String data = call.argument("data");
      String pin = call.argument("pin");
      int samSlot = call.argument("samSlot");
      int timeoutMs = call.argument("timeoutMs");
      doSignatureWithoutSelection(data, samSlot, pin, timeoutMs, result);
    } else if (call.method.equals("printPaper")) {
      pendingResult = result;
      List<Map<String, Object>> data = call.argument("data");
      MyPosPrinterService.printPaper(activity, data);
    } else {
      result.notImplemented();
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  private void getCertificateSerialHex(int samSlot, int timeoutMs, @NonNull Result result) {
    Thread r = new Thread(new Runnable(){
      @Override
      public void run() {
        try {
          if (!SAMCard.detect(activity, samSlot, timeoutMs)) {
            result.error("NO_SAM", "No SAM card detected in slot " + samSlot, null);
            return;
          }

          ICashRegisterSmartCard card = CashRegisterSmartCardFactory.createInstance(activity, samSlot, timeoutMs);
          String resp = card.getCertificateSerialHex();

          card.close();
          result.success(resp);
        } catch (Exception e) {
          e.printStackTrace();
          result.error("ERROR_SIG", e.getMessage(), null);
          showToast(e.getMessage());
        }
      }
    });
    r.start();
  }

  private void getCertificateSerialDec(int samSlot, int timeoutMs, @NonNull Result result) {
    Thread r = new Thread(new Runnable(){
      @Override
      public void run() {
        try {
          if (!SAMCard.detect(activity, samSlot, timeoutMs)) {
            result.error("NO_SAM", "No SAM card detected in slot " + samSlot, null);
            return;
          }

          ICashRegisterSmartCard card = CashRegisterSmartCardFactory.createInstance(activity, samSlot, timeoutMs);
          String resp = card.getCertificateSerialDec();

          card.close();
          result.success(resp);
        } catch (Exception e) {
          e.printStackTrace();
          result.error("ERROR_SIG", e.getMessage(), null);
          showToast(e.getMessage());
        }
      }
    });
    r.start();
  }

  private void getCIN(int samSlot, int timeoutMs, @NonNull Result result) {
    Thread r = new Thread(new Runnable(){
      @Override
      public void run() {
        try {
          if (!SAMCard.detect(activity, samSlot, timeoutMs)) {
            result.error("NO_SAM", "No SAM card detected in slot " + samSlot, null);
            return;
          }

          ICashRegisterSmartCard card = CashRegisterSmartCardFactory.createInstance(activity, samSlot, timeoutMs);
          String resp = card.getCIN();

          card.close();
          result.success(resp);
        } catch (Exception e) {
          e.printStackTrace();
          result.error("ERROR_SIG", e.getMessage(), null);
          showToast(e.getMessage());
        }
      }
    });
    r.start();
  }

  private void doSignature(String data, int samSlot, String pin, int timeoutMs, @NonNull Result result) {
    Thread r = new Thread(new Runnable(){
      @Override
      public void run() {
        try {
          if (!SAMCard.detect(activity, samSlot, timeoutMs)) {
            result.error("NO_SAM", "No SAM card detected in slot " + samSlot, null);
            return;
          }

          ICashRegisterSmartCard card = CashRegisterSmartCardFactory.createInstance(activity, samSlot, timeoutMs);
          byte[] hash = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));

          byte[] resp = card.doSignatur(hash, pin);
          String signatureBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(resp);

          card.close();
          result.success(signatureBase64Url);
        } catch (Exception e) {
          e.printStackTrace();
          result.error("ERROR_SIG", e.getMessage(), null);
          showToast(e.getMessage());
        }
      }
    });
    r.start();
  }

  private void doSignatureWithoutSelection(String data, int samSlot, String pin, int timeoutMs, @NonNull Result result) {
    Thread r = new Thread(new Runnable(){
      @Override
      public void run() {
        try {
          if (!SAMCard.detect(activity, samSlot, timeoutMs)) {
            result.error("NO_SAM", "No SAM card detected in slot " + samSlot, null);
            return;
          }

          ICashRegisterSmartCard card = CashRegisterSmartCardFactory.createInstance(activity, samSlot, timeoutMs);
          byte[] hash = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));

          byte[] resp = card.doSignaturWithoutSelection(hash, pin);
          String signatureBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(resp);

          card.close();
          result.success(signatureBase64Url);
        } catch (Exception e) {
          e.printStackTrace();
          result.error("ERROR_SIG", e.getMessage(), null);
          showToast(e.getMessage());
        }
      }
    });
    r.start();
  }

  public void showToast(final String toast) {
    activity.runOnUiThread(new Runnable() {
      public void run()
      {
        Toast.makeText(activity, toast, Toast.LENGTH_SHORT).show();
      }
    });
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
    binding.addActivityResultListener(this);
    IntentFilter filter = new IntentFilter("com.mypos.broadcast.PRINTING_DONE");
    this.activity.registerReceiver(myReceiver, filter);
  }


  @Override
  public void onDetachedFromActivityForConfigChanges() {
    activity = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    this.activity.unregisterReceiver(myReceiver);
    activity = null;
  }
}
