## 0.3.1

* myPOS Smart SDK 1.0.8.
* Every call is answered: a payment or print job that cannot start returns an error instead of
  waiting forever, and a second payment or print job while one is running fails with
  `ALREADY_ACTIVE` instead of taking over the first one's answer.
* A payment without result is reported as it is: `CANCEL` when the payment screen was closed,
  `UNKNOWN` when the terminal returned no data, `RESULT_LOST` when the activity went away for
  good. Before 0.3.1 all of these came back as `UNKNOWN`.
* Android 14: the print result receiver is registered as exported, so the plugin no longer
  crashes on start on terminals with Android 14.
* Printing sends one explicit broadcast (as the myPOS SDK documents) instead of two, and answers
  `UNKNOWN` after 60 seconds without a result from the terminal, `FAILED` at once on a device
  without the print service. `printPaper` never throws.
* An unknown currency is an error; before it was silently charged in EUR. ISK is supported.
* Payment results come back on a request code of their own, so other plugins' activity results
  are no longer taken for a myPOS payment.
* SAM card: answers on the main thread, the card is always closed, PINs are checked for digits,
  Base64 works on Android 6 and 7.
* `MyPosPaymentResponse.isApproved`, `isCancelled` and `isUnknown`.
* Own short APDU encoder replaces the previous copy of `javax.smartcardio.CommandAPDU`.
* No toasts and no console output from the plugin.
* Tests (Dart and Java), example app, README.

## 0.3.0

* myPOS Glass payments (`makeGlassPayment`).
* Full payment response (`MyPosPaymentResponse`).

## 0.2.0

* myPOS Glass implemented.

## 0.1.0

* Card payments, receipt printing and SAM signature card on myPOS Smart terminals.

## 0.0.1

* Initial release.
