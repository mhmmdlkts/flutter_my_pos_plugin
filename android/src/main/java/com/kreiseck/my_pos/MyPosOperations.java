package com.kreiseck.my_pos;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mypos.smartsdk.data.MerchantData;
import com.mypos.smartsdk.data.POSInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * Voids, refunds and the terminal info. Kept apart from payments and printing, which stay as they
 * are.
 *
 * One void or refund runs at a time and none starts while a payment runs. Every call is answered
 * exactly once; late, repeated or foreign activity results are dropped.
 */
final class MyPosOperations implements PluginRegistry.ActivityResultListener {

  /** Request codes of their own, distinct from the payment's. */
  static final int VOID_REQUEST_CODE = 0x4D51;
  static final int REFUND_REQUEST_CODE = 0x4D52;

  /** The terminal info is answered with {@code null} after this. */
  static final long POS_INFO_TIMEOUT_MS = 5_000;

  /** Whether a payment is running (read only; payments are not touched). */
  interface PaymentPending {
    boolean get();
  }

  private final TerminalSdk sdk;
  private final PaymentPending paymentPending;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  @Nullable private Activity activity;
  @Nullable private Result pending;
  private int pendingRequestCode;

  private final List<Result> infoWaiting = new ArrayList<>();
  private final Runnable infoTimeout = () -> finishInfo(null);

  MyPosOperations(TerminalSdk sdk, PaymentPending paymentPending) {
    this.sdk = sdk;
    this.paymentPending = paymentPending;
  }

  void attach(@NonNull Activity activity) {
    this.activity = activity;
  }

  void detach() {
    activity = null;
  }

  /** The activity is gone for good: a running void or refund can no longer report back. */
  void activityGone() {
    detach();
    Result result = pending;
    pending = null;
    if (result != null) {
      result.error("RESULT_LOST", "The app was closed before the result arrived; check the terminal.", null);
    }
    finishInfo(null);
  }

  boolean isRunning() {
    return pending != null;
  }

  /** Handles the call if it is one of ours; returns false otherwise. */
  boolean handle(@NonNull MethodCall call, @NonNull Result result) {
    switch (call.method) {
      case "voidPayment":
      case "voidLastPayment":
      case "refund":
        start(call, result);
        return true;
      case "posInfo":
        posInfo(result);
        return true;
      default:
        return false;
    }
  }

  private void start(MethodCall call, Result result) {
    Activity current = activity;
    if (current == null) {
      result.error("NO_ACTIVITY", "A void or refund needs a foreground activity.", null);
      return;
    }
    if (pending != null || paymentPending.get()) {
      result.error("ALREADY_ACTIVE", "Another payment, void or refund is running.", null);
      return;
    }
    boolean skipConfirmation = OperationArgs.skipConfirmation(call);
    int requestCode = call.method.equals("refund") ? REFUND_REQUEST_CODE : VOID_REQUEST_CODE;
    pending = result;
    pendingRequestCode = requestCode;
    try {
      switch (call.method) {
        case "voidPayment":
          sdk.openVoid(current, OperationArgs.voidOf(call), requestCode, skipConfirmation);
          break;
        case "voidLastPayment":
          sdk.openVoid(current, OperationArgs.voidLast(call), requestCode, skipConfirmation);
          break;
        default:
          sdk.openRefund(current, OperationArgs.refundOf(call), requestCode, skipConfirmation);
      }
    } catch (IllegalArgumentException e) {
      pending = null;
      result.error("INVALID_ARGUMENT", String.valueOf(e.getMessage()), null);
    } catch (Exception e) {
      pending = null;
      result.error("NOT_STARTED", String.valueOf(e.getMessage()), e.getClass().getSimpleName());
    }
  }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode != VOID_REQUEST_CODE && requestCode != REFUND_REQUEST_CODE) return false;
    Result result = pending;
    if (result == null || requestCode != pendingRequestCode) return true;
    pending = null;

    if (resultCode != Activity.RESULT_OK) {
      // Closed without a result: that does not prove that no money moved.
      result.success("CANCEL");
    } else if (data == null) {
      result.success("UNKNOWN");
    } else {
      try {
        result.success(MyPosPlugin.bundleToMap(data.getExtras()));
      } catch (RuntimeException e) {
        result.success("UNKNOWN");
      }
    }
    return true;
  }

  // --- Terminal info ---

  private void posInfo(Result result) {
    if (activity == null) {
      result.success(null);
      return;
    }
    infoWaiting.add(result);
    if (infoWaiting.size() > 1) return; // already asked; all waiting calls get the same answer
    mainHandler.postDelayed(infoTimeout, POS_INFO_TIMEOUT_MS);
    try {
      sdk.registerPosInfo(activity.getApplicationContext(), info -> mainHandler.post(() -> finishInfo(info)));
    } catch (RuntimeException e) {
      finishInfo(null);
    }
  }

  private void finishInfo(@Nullable POSInfo info) {
    mainHandler.removeCallbacks(infoTimeout);
    if (infoWaiting.isEmpty()) return;
    Map<String, Object> answer = info == null ? null : infoToMap(info);
    List<Result> waiting = new ArrayList<>(infoWaiting);
    infoWaiting.clear();
    for (Result r : waiting) {
      r.success(answer);
    }
  }

  static Map<String, Object> infoToMap(POSInfo info) {
    Map<String, Object> map = new HashMap<>();
    map.put("tid", info.getTID());
    map.put("currencyName", info.getCurrencyName());
    map.put("currencyCode", info.getCurrencyCode());
    MerchantData merchant = info.getMerchantData();
    if (merchant != null) {
      map.put("merchantId", merchant.getMerchantID());
      map.put("merchantName", merchant.getMerchantName());
    }
    return map;
  }
}
