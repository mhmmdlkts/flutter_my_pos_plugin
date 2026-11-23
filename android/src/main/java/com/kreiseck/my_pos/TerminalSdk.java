package com.kreiseck.my_pos;

import android.app.Activity;
import android.content.Context;

import com.mypos.smartsdk.MyPOSRefund;
import com.mypos.smartsdk.MyPOSVoid;
import com.mypos.smartsdk.OnPOSInfoListener;

/**
 * The terminal SDK calls used by voids, refunds and the terminal info. Kept as thin as possible so
 * everything in front of it can be tested without a terminal.
 */
interface TerminalSdk {

  void openVoid(Activity activity, MyPOSVoid request, int requestCode, boolean skipConfirmation) throws Exception;

  void openRefund(Activity activity, MyPOSRefund request, int requestCode, boolean skipConfirmation) throws Exception;

  void registerPosInfo(Context context, OnPOSInfoListener listener);
}
