package com.kreiseck.my_pos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.mypos.smartsdk.MyPOSRefund;
import com.mypos.smartsdk.MyPOSVoid;
import com.mypos.smartsdk.OnPOSInfoListener;
import com.mypos.smartsdk.data.MerchantData;
import com.mypos.smartsdk.data.POSInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel.Result;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MyPosOperationsTest {

  /** Records every SDK call; can be told to fail. */
  static final class FakeSdk implements TerminalSdk {
    final List<String> calls = new ArrayList<>();
    MyPOSVoid lastVoid;
    MyPOSRefund lastRefund;
    boolean lastSkip;
    Exception failWith;
    OnPOSInfoListener infoListener;

    @Override
    public void openVoid(Activity activity, MyPOSVoid request, int requestCode, boolean skipConfirmation) throws Exception {
      if (failWith != null) throw failWith;
      calls.add("void:" + requestCode);
      lastVoid = request;
      lastSkip = skipConfirmation;
    }

    @Override
    public void openRefund(Activity activity, MyPOSRefund request, int requestCode, boolean skipConfirmation) throws Exception {
      if (failWith != null) throw failWith;
      calls.add("refund:" + requestCode);
      lastRefund = request;
      lastSkip = skipConfirmation;
    }

    @Override
    public void registerPosInfo(Context context, OnPOSInfoListener listener) {
      calls.add("posInfo");
      infoListener = listener;
    }
  }

  /** Captures the single answer of a call; a second answer fails the test. */
  static final class Answer implements Result {
    int count = 0;
    Object value;
    String errorCode;

    @Override
    public void success(@Nullable Object result) {
      count++;
      value = result;
    }

    @Override
    public void error(String code, @Nullable String message, @Nullable Object details) {
      count++;
      errorCode = code;
    }

    @Override
    public void notImplemented() {
      count++;
      errorCode = "NOT_IMPLEMENTED";
    }
  }

  private FakeSdk sdk;
  private boolean paymentRunning;
  private MyPosOperations ops;
  private Activity activity;

  @Before
  public void setUp() {
    sdk = new FakeSdk();
    paymentRunning = false;
    ops = new MyPosOperations(sdk, () -> paymentRunning);
    activity = Robolectric.buildActivity(Activity.class).setup().get();
    ops.attach(activity);
  }

  private static MethodCall call(String method, Object... keyValues) {
    Map<String, Object> args = new HashMap<>();
    for (int i = 0; i < keyValues.length; i += 2) {
      args.put((String) keyValues[i], keyValues[i + 1]);
    }
    return new MethodCall(method, args);
  }

  private static MethodCall voidCall() {
    return call("voidPayment", "stan", "123", "authCode", "A1", "dateTime", "261123141500");
  }

  private static Intent extras(int status, @Nullable Boolean approved) {
    Intent data = new Intent();
    data.putExtra("status", status);
    if (approved != null) data.putExtra("transaction_approved", approved);
    data.putExtra("STAN", "000124");
    return data;
  }

  @Test
  public void otherMethods_areNotOurs() {
    Answer a = new Answer();
    assertFalse(ops.handle(call("makePayment"), a));
    assertEquals(0, a.count);
  }

  @Test
  public void void_opensTheTerminalAndReturnsTheResultExtras() {
    Answer a = new Answer();
    assertTrue(ops.handle(voidCall(), a));
    assertEquals(List.of("void:" + MyPosOperations.VOID_REQUEST_CODE), sdk.calls);
    assertFalse(sdk.lastSkip);
    assertEquals(123, sdk.lastVoid.getSTAN());
    assertEquals(0, a.count);

    assertTrue(ops.onActivityResult(MyPosOperations.VOID_REQUEST_CODE, Activity.RESULT_OK, extras(0, true)));
    assertEquals(1, a.count);
    @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) a.value;
    assertEquals(0, map.get("status"));
    assertEquals(true, map.get("transaction_approved"));
    assertEquals("000124", map.get("STAN"));
    assertFalse(ops.isRunning());
  }

  @Test
  public void refund_usesItsOwnRequestCode_andSkipIsPassedOn() {
    Answer a = new Answer();
    ops.handle(call("refund", "amountCents", 2450, "currency", "EUR", "skipConfirmation", true), a);
    assertEquals(List.of("refund:" + MyPosOperations.REFUND_REQUEST_CODE), sdk.calls);
    assertTrue(sdk.lastSkip);
    assertEquals(24.50, sdk.lastRefund.getRefundAmount(), 0);
    ops.onActivityResult(MyPosOperations.REFUND_REQUEST_CODE, Activity.RESULT_OK, extras(2, null));
    assertEquals(2, ((Map<?, ?>) a.value).get("status"));
  }

  @Test
  public void closedWithoutResult_isCancel_withoutData_isUnknown() {
    Answer a = new Answer();
    ops.handle(voidCall(), a);
    ops.onActivityResult(MyPosOperations.VOID_REQUEST_CODE, Activity.RESULT_CANCELED, null);
    assertEquals("CANCEL", a.value);

    Answer b = new Answer();
    ops.handle(voidCall(), b);
    ops.onActivityResult(MyPosOperations.VOID_REQUEST_CODE, Activity.RESULT_OK, null);
    assertEquals("UNKNOWN", b.value);
  }

  @Test
  public void foreignLateAndRepeatedResults_areDropped() {
    Answer a = new Answer();
    assertFalse(ops.onActivityResult(0x4D50, Activity.RESULT_OK, extras(0, true))); // payment's code
    assertFalse(ops.onActivityResult(1, Activity.RESULT_OK, extras(0, true)));
    assertTrue(ops.onActivityResult(MyPosOperations.VOID_REQUEST_CODE, Activity.RESULT_OK, extras(0, true))); // nothing pending

    ops.handle(voidCall(), a);
    // A refund result while a void waits does not answer the void.
    ops.onActivityResult(MyPosOperations.REFUND_REQUEST_CODE, Activity.RESULT_OK, extras(0, true));
    assertEquals(0, a.count);
    ops.onActivityResult(MyPosOperations.VOID_REQUEST_CODE, Activity.RESULT_OK, extras(1, null));
    ops.onActivityResult(MyPosOperations.VOID_REQUEST_CODE, Activity.RESULT_OK, extras(0, true));
    assertEquals(1, a.count);
    assertEquals(1, ((Map<?, ?>) a.value).get("status"));
  }

  @Test
  public void oneAtATime_andNeverDuringAPayment() {
    Answer first = new Answer();
    Answer second = new Answer();
    ops.handle(voidCall(), first);
    ops.handle(call("refund", "amountCents", 100, "currency", "EUR"), second);
    assertEquals("ALREADY_ACTIVE", second.errorCode);
    assertEquals(1, sdk.calls.size());
    assertEquals(0, first.count);

    ops.onActivityResult(MyPosOperations.VOID_REQUEST_CODE, Activity.RESULT_CANCELED, null);
    paymentRunning = true;
    Answer third = new Answer();
    ops.handle(voidCall(), third);
    assertEquals("ALREADY_ACTIVE", third.errorCode);
    assertEquals(1, sdk.calls.size());
  }

  @Test
  public void badArguments_neverOpenTheTerminal() {
    Answer a = new Answer();
    ops.handle(call("voidPayment", "stan", "0", "authCode", "A1", "dateTime", "261123141500"), a);
    assertEquals("INVALID_ARGUMENT", a.errorCode);
    Answer b = new Answer();
    ops.handle(call("refund", "amountCents", 0, "currency", "EUR"), b);
    assertEquals("INVALID_ARGUMENT", b.errorCode);
    assertTrue(sdk.calls.isEmpty());
    assertFalse(ops.isRunning());
  }

  @Test
  public void terminalThatCannotOpen_isNotStarted_andFreesTheLock() {
    sdk.failWith = new IllegalStateException("no payment core");
    Answer a = new Answer();
    ops.handle(voidCall(), a);
    assertEquals("NOT_STARTED", a.errorCode);
    assertFalse(ops.isRunning());
  }

  @Test
  public void withoutActivity_nothingStarts() {
    ops.detach();
    Answer a = new Answer();
    ops.handle(voidCall(), a);
    assertEquals("NO_ACTIVITY", a.errorCode);
    Answer info = new Answer();
    ops.handle(call("posInfo"), info);
    assertEquals(1, info.count);
    assertNull(info.value);
    assertTrue(sdk.calls.isEmpty());
  }

  @Test
  public void activityGone_answersTheRunningCallOnce() {
    Answer a = new Answer();
    ops.handle(voidCall(), a);
    ops.activityGone();
    assertEquals("RESULT_LOST", a.errorCode);
    ops.onActivityResult(MyPosOperations.VOID_REQUEST_CODE, Activity.RESULT_OK, extras(0, true));
    assertEquals(1, a.count);
  }

  @Test
  public void configChange_keepsTheRunningCall() {
    Answer a = new Answer();
    ops.handle(voidCall(), a);
    ops.detach();
    ops.attach(activity);
    ops.onActivityResult(MyPosOperations.VOID_REQUEST_CODE, Activity.RESULT_OK, extras(0, true));
    assertEquals(1, a.count);
    assertEquals(0, ((Map<?, ?>) a.value).get("status"));
  }

  @Test
  public void posInfo_answersAllWaitingCallsOnMainThread() {
    Answer a = new Answer();
    Answer b = new Answer();
    ops.handle(call("posInfo"), a);
    ops.handle(call("posInfo"), b);
    assertEquals(List.of("posInfo"), sdk.calls);

    POSInfo info = new POSInfo();
    info.setTID("90000001");
    info.setCurrencyName("EUR");
    info.setCurrencyCode("978");
    MerchantData merchant = new MerchantData();
    merchant.setMerchantID("M-1");
    merchant.setMerchantName("Taxi Beispiel");
    info.setMerchantData(merchant);
    sdk.infoListener.onReceive(info);
    shadowOf(Looper.getMainLooper()).idle();

    assertEquals(1, a.count);
    assertEquals(1, b.count);
    Map<?, ?> map = (Map<?, ?>) a.value;
    assertEquals("90000001", map.get("tid"));
    assertEquals("978", map.get("currencyCode"));
    assertEquals("Taxi Beispiel", map.get("merchantName"));

    // A late second report is not answered again.
    sdk.infoListener.onReceive(info);
    shadowOf(Looper.getMainLooper()).idle();
    assertEquals(1, a.count);
  }

  @Test
  public void posInfo_timesOutWithNull() {
    Answer a = new Answer();
    ops.handle(call("posInfo"), a);
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(MyPosOperations.POS_INFO_TIMEOUT_MS));
    assertEquals(1, a.count);
    assertNull(a.value);
  }
}
