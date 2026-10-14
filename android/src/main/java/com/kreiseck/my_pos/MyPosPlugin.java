package com.kreiseck.my_pos;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mypos.smartsdk.MyPOSUtil;
import com.mypos.smartsdk.SAMCard;
import com.mypos.smartsdk.print.PrinterStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * Bridge between Flutter and the myPOS Smart / Glass SDK.
 *
 * One payment and one print job can run at a time. Every call is answered exactly once:
 * a call that cannot start returns an error instead of leaving the Dart future open, and
 * late or repeated answers from the terminal are dropped.
 */
public class MyPosPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {

  /** Request code of the payment activity; distinct so other plugins' results are not taken. */
  static final int PAYMENT_REQUEST_CODE = 0x4D50;

  /** A print job the terminal never reports back on is answered with UNKNOWN after this. */
  static final long PRINT_TIMEOUT_MS = 60_000;

  private MethodChannel channel;
  private ActivityPluginBinding activityBinding;
  private Activity activity;
  private boolean receiverRegistered = false;

  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private ExecutorService samExecutor;

  private Result pendingPayment;
  private Result pendingPrint;
  private final Runnable printTimeout = () -> finishPrint(PrintStatus.UNKNOWN);

  private final BroadcastReceiver printingDoneReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      boolean printingStarted = intent.getBooleanExtra("printing_started", false);
      int status = intent.getIntExtra("printer_status", PrinterStatus.PRINTER_STATUS_UNKNOWN_ERROR);
      finishPrint(PrintStatus.fromBroadcast(printingStarted, status));
    }
  };

  // --- FlutterPlugin ---

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    channel = new MethodChannel(binding.getBinaryMessenger(), "my_pos");
    channel.setMethodCallHandler(this);
    samExecutor = Executors.newSingleThreadExecutor();
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
    channel = null;
    samExecutor.shutdown();
  }

  // --- ActivityAware ---

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activityBinding = binding;
    activity = binding.getActivity();
    binding.addActivityResultListener(this);
    registerPrintReceiver();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    detachActivity();
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onAttachedToActivity(binding);
  }

  @Override
  public void onDetachedFromActivity() {
    detachActivity();
  }

  private void detachActivity() {
    unregisterPrintReceiver();
    if (activityBinding != null) {
      activityBinding.removeActivityResultListener(this);
    }
    activityBinding = null;
    activity = null;
  }

  private void registerPrintReceiver() {
    if (activity == null || receiverRegistered) return;
    IntentFilter filter = new IntentFilter(MyPOSUtil.PRINTING_DONE_BROADCAST);
    // The broadcast comes from the myPOS system app, so the receiver has to be exported.
    // Since Android 14 registering without the flag throws.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      activity.registerReceiver(printingDoneReceiver, filter, Context.RECEIVER_EXPORTED);
    } else {
      activity.registerReceiver(printingDoneReceiver, filter);
    }
    receiverRegistered = true;
  }

  private void unregisterPrintReceiver() {
    if (activity == null || !receiverRegistered) return;
    try {
      activity.unregisterReceiver(printingDoneReceiver);
    } catch (IllegalArgumentException ignored) {
      // Already gone.
    }
    receiverRegistered = false;
  }

  // --- MethodCallHandler ---

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "makePayment":
      case "makeGlassPayment":
        startPayment(call, result);
        break;
      case "printPaper":
        startPrint(call, result);
        break;
      case "getCertificateSerialHex":
      case "getCertificateSerialDec":
      case "getCIN":
      case "doSignature":
      case "doSignatureWithoutSelection":
        runSamCommand(call, result);
        break;
      default:
        result.notImplemented();
    }
  }

  // --- Payment ---

  private void startPayment(@NonNull MethodCall call, @NonNull Result result) {
    if (activity == null) {
      result.error("NO_ACTIVITY", "A myPOS payment needs a foreground activity.", null);
      return;
    }
    if (pendingPayment != null) {
      result.error("ALREADY_ACTIVE", "A myPOS payment is already running.", null);
      return;
    }
    PaymentArgs args;
    try {
      args = PaymentArgs.from(call);
    } catch (IllegalArgumentException e) {
      result.error("INVALID_ARGUMENT", e.getMessage(), null);
      return;
    }
    pendingPayment = result;
    try {
      if (call.method.equals("makeGlassPayment")) {
        MyPosGlassPaymentService.startPayment(activity, args, PAYMENT_REQUEST_CODE);
      } else {
        MyPosPaymentService.startPayment(activity, args, PAYMENT_REQUEST_CODE);
      }
    } catch (Exception e) {
      pendingPayment = null;
      result.error("PAYMENT_NOT_STARTED", String.valueOf(e.getMessage()), e.getClass().getSimpleName());
    }
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode != PAYMENT_REQUEST_CODE) return false;
    Result result = pendingPayment;
    pendingPayment = null;
    if (result == null) return true;

    if (resultCode != Activity.RESULT_OK || data == null) {
      result.success("CANCEL");
    } else {
      result.success(bundleToMap(data.getExtras()));
    }
    return true;
  }

  /** Copies the result extras; values the codec cannot carry are passed as text. */
  static Map<String, Object> bundleToMap(@Nullable Bundle extras) {
    HashMap<String, Object> map = new HashMap<>();
    if (extras == null) return map;
    for (String key : extras.keySet()) {
      Object value = extras.get(key);
      if (value == null) continue;
      if (value instanceof String || value instanceof Boolean || value instanceof Integer
          || value instanceof Long || value instanceof Double || value instanceof Float) {
        map.put(key, value);
      } else {
        map.put(key, value.toString());
      }
    }
    return map;
  }

  // --- Printing ---

  private void startPrint(@NonNull MethodCall call, @NonNull Result result) {
    if (activity == null) {
      result.success(PrintStatus.FAILED);
      return;
    }
    if (pendingPrint != null) {
      result.error("ALREADY_ACTIVE", "A myPOS print job is already running.", null);
      return;
    }
    Intent intent;
    try {
      List<Map<String, Object>> data = call.argument("data");
      intent = MyPosPrinterService.buildPrintIntent(data);
    } catch (RuntimeException e) {
      result.error("INVALID_ARGUMENT", String.valueOf(e.getMessage()), null);
      return;
    }
    pendingPrint = result;
    mainHandler.postDelayed(printTimeout, PRINT_TIMEOUT_MS);
    try {
      MyPosPrinterService.send(activity, intent);
    } catch (RuntimeException e) {
      finishPrint(PrintStatus.FAILED);
    }
  }

  private void finishPrint(String status) {
    mainHandler.removeCallbacks(printTimeout);
    Result result = pendingPrint;
    pendingPrint = null;
    if (result != null) result.success(status);
  }

  // --- SAM (signature card of the cash register) ---

  private void runSamCommand(@NonNull MethodCall call, @NonNull Result result) {
    if (activity == null) {
      result.error("NO_ACTIVITY", "SAM access needs an activity.", null);
      return;
    }
    final Context context = activity.getApplicationContext();
    final String method = call.method;
    final Integer slot = call.argument("samSlot");
    final Integer timeout = call.argument("timeoutMs");
    final String data = call.argument("data");
    final String pin = call.argument("pin");
    if (slot == null || timeout == null) {
      result.error("INVALID_ARGUMENT", "samSlot and timeoutMs are required.", null);
      return;
    }
    samExecutor.execute(() -> {
      ICashRegisterSmartCard card = null;
      try {
        if (!SAMCard.detect(context, slot, timeout)) {
          replyError(result, "NO_SAM", "No SAM card detected in slot " + slot);
          return;
        }
        card = CashRegisterSmartCardFactory.createInstance(context, slot, timeout);
        String value;
        switch (method) {
          case "getCertificateSerialHex":
            value = card.getCertificateSerialHex();
            break;
          case "getCertificateSerialDec":
            value = card.getCertificateSerialDec();
            break;
          case "getCIN":
            value = card.getCIN();
            break;
          case "doSignature":
            value = base64Url(card.doSignatur(sha256(data), pin));
            break;
          default:
            value = base64Url(card.doSignaturWithoutSelection(sha256(data), pin));
        }
        final String answer = value;
        mainHandler.post(() -> result.success(answer));
      } catch (Exception e) {
        replyError(result, "ERROR_SIG", String.valueOf(e.getMessage()));
      } finally {
        if (card != null) {
          try {
            card.close();
          } catch (Exception ignored) {
            // The answer is already decided.
          }
        }
      }
    });
  }

  private void replyError(Result result, String code, String message) {
    mainHandler.post(() -> result.error(code, message, null));
  }

  private static byte[] sha256(String data) throws Exception {
    if (data == null) throw new IllegalArgumentException("data is required");
    return MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
  }

  /** Base64url without padding, as RKSV expects for the signature value. */
  static String base64Url(byte[] bytes) {
    return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
  }
}
