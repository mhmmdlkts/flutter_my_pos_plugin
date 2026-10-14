<p align="center">
  <a href="https://kasseneck.at"><img src="doc/kreiseck_logo.png" alt="Kreiseck Software Solutions" width="260"></a>
</p>

<h1 align="center">my_pos</h1>

<p align="center">
  <b>Card payments, receipt printing and the RKSV signature card on Android Smart POS terminals.</b><br>
  The terminal bridge of <a href="https://kasseneck.at">Kasseneck</a>, the cloud cash register system for Austria.
</p>

<p align="center">
  <a href="https://pub.dev/packages/my_pos"><img src="https://img.shields.io/pub/v/my_pos?color=930C0C&label=pub" alt="pub version"></a>
  <a href="https://pub.dev/packages/my_pos/score"><img src="https://img.shields.io/pub/points/my_pos?color=930C0C" alt="pub points"></a>
  <img src="https://img.shields.io/badge/platform-Android-930C0C" alt="Android">
  <img src="https://img.shields.io/badge/license-Apache--2.0-930C0C" alt="Apache-2.0 license">
  <a href="https://kasseneck.at"><img src="https://img.shields.io/badge/by-Kasseneck-111111" alt="by Kasseneck"></a>
</p>

---

`my_pos` connects a Flutter app to an Android Smart POS terminal: take a card payment, print
the receipt on the built-in printer and sign receipts with the RKSV signature card in the
terminal's SAM slot. It runs in production in the [Kasseneck](https://kasseneck.at) apps, for
example the taxi register Karteck.

| | |
|---|---|
| 💳 Card payment | Opens the terminal's payment screen and returns the full result (approval, reference number, STAN, card brand, ...) |
| 🧾 Receipt printing | Text, two-column lines, images and QR codes on the built-in printer |
| 🔏 Signature card | Certificate serial and signature with the RKSV signature card in the SAM slot (ACOS, ACOS-ID, CardOS) |
| 📱 Tap to pay | Payments through the Glass app on a phone |

Android only. The app has to run on a supported terminal (or with the Glass app installed).

## Installation

```yaml
dependencies:
  my_pos: ^0.3.1
```

Nothing else to set up: the plugin adds the package queries for the terminal's payment service
to the merged manifest. Minimum Android version is 6.0 (API 23).

## Card payment

```dart
import 'package:my_pos/enums/my_pos_currency_enum.dart';
import 'package:my_pos/my_pos.dart';

final result = await MyPos.makePayment(
  amount: 12.50,
  currency: MyPosCurrency.eur,
  printCustomerReceipt: false, // the app prints its own receipt
  printMerchantReceipt: false,
  reference: 'ORDER-4711',
);

if (result.isApproved) {
  // Paid. Keep result.reference_number / result.STAN for the books.
} else if (result.isUnknown || result.isCancelled) {
  // No approval came back. Check the terminal's transaction list
  // before charging again: the card may still have been charged.
} else {
  // Declined: result.response_code says why.
}
```

Only `isApproved` means paid. A missing result (`isUnknown`, or `isCancelled` after the payment
screen was closed) does not prove that no money moved.

`makePayment` throws a `PlatformException` only when the payment screen cannot be opened:

| Code | Meaning |
|---|---|
| `NO_ACTIVITY` | The app is not in the foreground. |
| `ALREADY_ACTIVE` | A payment is already running. |
| `INVALID_ARGUMENT` | Amount is not a positive number, or the currency is missing. |
| `PAYMENT_NOT_STARTED` | The payment service is missing or refused the request (unknown currency, unsupported function, ...). |
| `RESULT_LOST` | The app's activity was closed for good while the payment ran. Check the terminal. |

Only one payment runs at a time, and every call is answered exactly once.

## Receipt printing

```dart
import 'package:my_pos/enums/my_pos_print_response.dart';
import 'package:my_pos/models/my_pos_paper.dart';

final paper = MyPosPaper()
  ..addText('Kasseneck', fontSize: 32, alignment: PrinterAlignment.center)
  ..addDoubleText('Taxi ride', 'EUR 12,50')
  ..addDoubleText('Total', 'EUR 12,50')
  ..addQrCode('https://kasseneck.at', size: 240)
  ..addSpace(2)
  ..addCutLine();

final status = await paper.print(); // success, failed, outOfPaper or unknown
```

Printing never throws. On a device without the print service the answer is
`PrintResponse.failed` at once; if the terminal does not report back within 60 seconds it is
`PrintResponse.unknown`. An image or QR code that cannot be built is left out, the rest prints.
`paper.toString()` gives a text preview for logs and tests.

## Signature card (RKSV)

Austrian cash registers sign every receipt (Registrierkassensicherheitsverordnung, RKSV). On a
Smart POS terminal the signature card sits in a SAM slot:

```dart
import 'package:my_pos/enums/sam_slot_enum.dart';

final serial = await MyPos.getCertificateSerialHex(SamSlot.slot_1);
final signature = await MyPos.doSignature(dataToBeSigned, SamSlot.slot_1, pin);
// Base64url without padding, ready for the RKSV signature value.
```

`doSignature` hashes the data with SHA-256 before signing. Errors come as `PlatformException`
with the code `NO_SAM` (no card in the slot) or `ERROR_SIG`.

## Publishing to the terminal app store

Terminal apps are installed from the terminal vendor's app store, not from Google Play. What
helped us get through the review:

* Ship 32-bit ARM (`armeabi-v7a`) next to `arm64-v8a`. Some terminals run a 32-bit system and
  cannot install an app with 64-bit libraries only.
  ```groovy
  defaultConfig {
      ndk { abiFilters "armeabi-v7a", "arm64-v8a" }
  }
  ```
* Upload a release APK signed with APK Signature Scheme v2 and keep the same key for every
  version.
* Ask only for permissions the app needs; the review checks them.

## Example

[`example/`](example) charges EUR 1.00 and prints a test receipt.

## About Kasseneck

**[Kasseneck](https://kasseneck.at)** is a cloud cash register system (Registrierkasse) for
Austria: RKSV-compliant receipts from the browser, the phone or the card terminal, without
extra cash register hardware. This plugin is how Kasseneck talks to card terminals.

- 🌐 Kassensystem: **[kasseneck.at](https://kasseneck.at)**
- 🏢 Built by **[Kreiseck Software Solutions](https://kreiseck.com)**, Austria
- 💙 If this package saves you time, a **like on [pub.dev](https://pub.dev/packages/my_pos)** helps.

## Versioning

Semantic versioning, see the [CHANGELOG](CHANGELOG.md).

## License

Apache-2.0, see [LICENSE](LICENSE). © 2023-2026 Kreiseck Software Solutions.

---

<p align="center">
  <sub>Made with care by <a href="https://kreiseck.com"><b>Kreiseck Software Solutions</b></a> for <a href="https://kasseneck.at"><b>Kasseneck</b></a> · Austria 🇦🇹</sub>
</p>
