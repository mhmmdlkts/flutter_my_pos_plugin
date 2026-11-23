package com.kreiseck.my_pos;

import android.app.Activity;
import android.content.Context;

import com.mypos.smartsdk.MyPOSAPI;
import com.mypos.smartsdk.MyPOSRefund;
import com.mypos.smartsdk.MyPOSVoid;
import com.mypos.smartsdk.OnPOSInfoListener;

/** {@link TerminalSdk} on the real terminal: hands every call to the SDK unchanged. */
final class MyPosSdk implements TerminalSdk {

  @Override
  public void openVoid(Activity activity, MyPOSVoid request, int requestCode, boolean skipConfirmation) throws Exception {
    MyPOSAPI.openVoidActivity(activity, request, requestCode, skipConfirmation);
  }

  @Override
  public void openRefund(Activity activity, MyPOSRefund request, int requestCode, boolean skipConfirmation) throws Exception {
    MyPOSAPI.openRefundActivity(activity, request, requestCode, skipConfirmation);
  }

  @Override
  public void registerPosInfo(Context context, OnPOSInfoListener listener) {
    MyPOSAPI.registerPOSInfo(context, listener);
  }
}
