import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:my_pos/enums/my_pos_currency_enum.dart';
import 'package:my_pos/enums/my_pos_payment_response_status.dart';
import 'package:my_pos/enums/my_pos_print_response.dart';
import 'package:my_pos/enums/sam_slot_enum.dart';
import 'package:my_pos/models/my_pos_paper.dart';
import 'package:my_pos/my_pos.dart';
import 'package:my_pos/my_pos_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('my_pos');
  final calls = <MethodCall>[];
  Object? Function(MethodCall call) answer = (_) => null;

  setUp(() {
    calls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return answer(call);
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  group('makePayment', () {
    test('sends the arguments the native side reads', () async {
      answer = (_) => {'transaction_approved': true};
      await MyPos.makePayment(
        amount: 12.5,
        currency: MyPosCurrency.eur,
        printCustomerReceipt: false,
        printMerchantReceipt: false,
        fixedPinPad: true,
        reference: 'R-1',
      );
      expect(calls.single.method, 'makePayment');
      expect(calls.single.arguments, {
        'amount': 12.5,
        'currency': 'eur',
        'printMerchantReceipt': false,
        'printCustomerReceipt': false,
        'fixedPinpad': true,
        'giftCardTransaction': false,
        'eReceiptReceiver': null,
        'reference': 'R-1',
      });
    });

    test('an approved payment carries the terminal fields', () async {
      answer = (_) => {
            'transaction_approved': true,
            'reference_number': '123456',
            'STAN': '42',
            'amount_dcc': 3,
            'status': '0',
          };
      final res = await MyPos.makePayment(amount: 1, currency: MyPosCurrency.eur);
      expect(res.isApproved, isTrue);
      expect(res.reference_number, '123456');
      expect(res.STAN, '42');
      expect(res.amount_dcc, 3.0);
      expect(res.status, 0);
    });

    test('a declined payment is not approved', () async {
      answer = (_) => {'transaction_approved': false, 'response_code': '05'};
      final res = await MyPos.makePayment(amount: 1, currency: MyPosCurrency.eur);
      expect(res.isApproved, isFalse);
      expect(res.response_code, '05');
    });

    test('cancel comes back as a response, not as an error', () async {
      answer = (_) => 'CANCEL';
      final res = await MyPos.makePayment(amount: 1, currency: MyPosCurrency.eur);
      expect(res.isApproved, isFalse);
      expect(res.isCancelled, isTrue);
    });

    test('a payment that cannot start throws', () async {
      answer = (_) => throw PlatformException(code: 'PAYMENT_NOT_STARTED');
      expect(
        MyPos.makePayment(amount: 1, currency: MyPosCurrency.eur),
        throwsA(isA<PlatformException>().having((e) => e.code, 'code', 'PAYMENT_NOT_STARTED')),
      );
    });
  });

  test('glass payment status', () {
    expect(MethodChannelMyPos.glassStatusFrom({'transaction_approved': true}), PaymentResponseStatus.success);
    expect(MethodChannelMyPos.glassStatusFrom({'response_code': '05'}), PaymentResponseStatus.declined);
    expect(MethodChannelMyPos.glassStatusFrom('CANCEL'), PaymentResponseStatus.cancel);
    expect(MethodChannelMyPos.glassStatusFrom(null), PaymentResponseStatus.unknown);
  });

  group('printing', () {
    test('sends the commands and maps the result', () async {
      answer = (_) => 'OUT_OF_PAPER';
      final paper = MyPosPaper()
        ..addText('Hallo', alignment: PrinterAlignment.center)
        ..addDoubleText('Summe', '12,50')
        ..addQrCode('https://kasseneck.at', size: null)
        ..addSpace(1);
      expect(await paper.print(), PrintResponse.outOfPaper);
      final commands = (calls.single.arguments as Map)['data'] as List;
      expect(commands.map((c) => c['type']), ['text', 'doubleText', 'qrCode', 'space']);
      expect(commands[0]['alignment'], 1);
      expect(commands[2]['size'], 200);
    });

    test('never throws', () async {
      answer = (_) => throw PlatformException(code: 'ALREADY_ACTIVE');
      expect(await MyPos.printPaper(MyPosPaper()..addText('x')), PrintResponse.failed);
    });

    test('preview handles lines longer than the paper', () {
      final paper = MyPosPaper()..addText('x' * 50, alignment: PrinterAlignment.right);
      expect(paper.toString(), contains('x' * 50));
    });
  });

  test('SAM calls pass slot and timeout', () async {
    answer = (_) => 'sig';
    expect(await MyPos.doSignature('data', SamSlot.slot_2, '1234', timeoutMs: 500), 'sig');
    expect(calls.single.arguments, {'samSlot': 2, 'timeoutMs': 500, 'data': 'data', 'pin': '1234'});
  });
}
