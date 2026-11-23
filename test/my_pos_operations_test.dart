// Voids, refunds and the terminal info. Values are invented.
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:my_pos/enums/my_pos_currency_enum.dart';
import 'package:my_pos/enums/my_pos_receipts.dart';
import 'package:my_pos/models/my_pos_operation_result.dart';
import 'package:my_pos/models/my_pos_void_reference.dart';
import 'package:my_pos/my_pos.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const channel = MethodChannel('my_pos');
  final calls = <MethodCall>[];
  Object? Function(MethodCall call) answer = (_) => null;

  setUp(() {
    calls.clear();
    answer = (_) => null;
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return answer(call);
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  const ref = MyPosVoidReference(
      stan: 123, authCode: 'A1B2C3', dateTime: '261123141500');

  group('voidPayment', () {
    test('sends reference and options; terminal confirmation by default',
        () async {
      answer = (_) => {'status': 0, 'transaction_approved': true};
      final r = await MyPos.voidPayment(ref);
      expect(r, isA<MyPosDone>());
      expect(calls.single.method, 'voidPayment');
      expect(calls.single.arguments, {
        'stan': 123,
        'authCode': 'A1B2C3',
        'dateTime': '261123141500',
        'printMerchantReceipt': false,
        'printCustomerReceipt': false,
        'skipConfirmation': false,
      });
    });

    test('receipts and skipping the confirmation are passed on', () async {
      answer = (_) => {'status': 0};
      await MyPos.voidPayment(ref,
          receipts: MyPosReceipts.customer, skipConfirmation: true);
      expect(
          calls.single.arguments, containsPair('printCustomerReceipt', true));
      expect(
          calls.single.arguments, containsPair('printMerchantReceipt', false));
      expect(calls.single.arguments, containsPair('skipConfirmation', true));
    });

    test('voidLastPayment sends only options', () async {
      answer = (_) => {'status': 1};
      expect(await MyPos.voidLastPayment(), isA<MyPosCancelled>());
      expect(calls.single.method, 'voidLastPayment');
      expect(calls.single.arguments, {
        'printMerchantReceipt': false,
        'printCustomerReceipt': false,
        'skipConfirmation': false
      });
    });
  });

  group('refund', () {
    test('sends cents and currency', () async {
      answer = (_) => {'status': 0, 'transaction_approved': true};
      expect(await MyPos.refund(amountCents: 2450), isA<MyPosDone>());
      expect(calls.single.method, 'refund');
      expect(calls.single.arguments, {
        'amountCents': 2450,
        'currency': 'eur',
        'printMerchantReceipt': false,
        'printCustomerReceipt': false,
        'skipConfirmation': false,
      });
    });

    test('0 or less never reaches the terminal', () async {
      for (final cents in [0, -1]) {
        final r =
            await MyPos.refund(amountCents: cents, currency: MyPosCurrency.eur);
        expect(r, isA<MyPosNotStarted>());
        expect((r as MyPosNotStarted).reason,
            MyPosNotStartedReason.invalidArgument);
      }
      expect(calls, isEmpty);
    });
  });

  group('result from the terminal', () {
    MyPosOperationResult of(Object? a) => MyPosOperationResult.fromNative(a);

    test('done: approved, or status 0 without an explicit "not approved"', () {
      expect(of({'transaction_approved': true}), isA<MyPosDone>());
      expect(of({'status': 0}), isA<MyPosDone>());
      expect(of({'status': '0', 'transaction_approved': 'true'}),
          isA<MyPosDone>());
      expect(of({'status': 0, 'transaction_approved': false}),
          isA<MyPosUnclear>());
    });

    test('nothing moved: cancelled, declined, refused, not found', () {
      expect(of({'status': 1}), isA<MyPosCancelled>());
      expect(of({'status': 2, 'response_code': '05'}), isA<MyPosDeclined>());
      for (final s in [4, 6, 7, 9]) {
        final r = of({'status': s});
        expect(r, isA<MyPosNotStarted>(), reason: 'status $s');
        expect((r as MyPosNotStarted).reason,
            MyPosNotStartedReason.refusedByTerminal);
      }
      final notFound = of({'status': 5});
      expect(
          (notFound as MyPosNotStarted).reason, MyPosNotStartedReason.notFound);
    });

    test('may have moved money: unclear', () {
      for (final s in [3, 8, 10, 11, 99, null]) {
        expect(of({'status': s}), isA<MyPosUnclear>(), reason: 'status $s');
      }
      expect(of('CANCEL'), isA<MyPosUnclear>());
      expect(of('UNKNOWN'), isA<MyPosUnclear>());
      expect(of(null), isA<MyPosUnclear>());
      expect(of({}), isA<MyPosUnclear>());
    });

    test('carries status, response code and all extras', () {
      final r = of({
        'status': 2,
        'response_code': '05',
        'status_text': 'Declined',
        'STAN': '000124'
      });
      expect(r.status, 2);
      expect(r.responseCode, '05');
      expect(r.statusText, 'Declined');
      expect(r.extras['STAN'], '000124');
    });

    test('approved wins over an odd status', () {
      expect(of({'status': 3, 'transaction_approved': true}), isA<MyPosDone>());
    });
  });

  group('errors become results', () {
    Future<MyPosOperationResult> failing(String code) {
      answer = (_) => throw PlatformException(code: code, message: 'x');
      return MyPos.voidPayment(ref);
    }

    test('before the terminal opened: not started', () async {
      expect(((await failing('NO_ACTIVITY')) as MyPosNotStarted).reason,
          MyPosNotStartedReason.noActivity);
      expect(((await failing('INVALID_ARGUMENT')) as MyPosNotStarted).reason,
          MyPosNotStartedReason.invalidArgument);
      expect(((await failing('ALREADY_ACTIVE')) as MyPosNotStarted).reason,
          MyPosNotStartedReason.alreadyActive);
      expect(((await failing('NOT_STARTED')) as MyPosNotStarted).reason,
          MyPosNotStartedReason.notOpened);
    });

    test('app closed during the void or unknown error: unclear', () async {
      expect(await failing('RESULT_LOST'), isA<MyPosUnclear>());
      expect(await failing('SOMETHING_NEW'), isA<MyPosUnclear>());
    });

    test('no plugin: not supported', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      final r = await MyPos.refund(amountCents: 100);
      expect((r as MyPosNotStarted).reason, MyPosNotStartedReason.notSupported);
    });
  });

  group('MyPosVoidReference.tryParse', () {
    test('reads the stored strings', () {
      expect(
        MyPosVoidReference.tryParse({
          'stan': ' 000123 ',
          'auth_code': 'A1B2C3',
          'date_time': '261123141500',
          'tid': '90000001'
        }),
        ref,
      );
    });

    test('null when something is missing or the STAN is not a number above 0',
        () {
      const ok = {'stan': '7', 'auth_code': 'X', 'date_time': '261123141500'};
      expect(MyPosVoidReference.tryParse(null), isNull);
      expect(MyPosVoidReference.tryParse({}), isNull);
      for (final stan in ['', ' ', '0', '-1', 'abc', '1.5']) {
        expect(MyPosVoidReference.tryParse({...ok, 'stan': stan}), isNull,
            reason: stan);
      }
      expect(MyPosVoidReference.tryParse({...ok, 'stan': 7}), isNull);
      expect(MyPosVoidReference.tryParse({...ok, 'auth_code': ' '}), isNull);
      expect(MyPosVoidReference.tryParse({...ok}..remove('date_time')), isNull);
    });
  });

  group('posInfo', () {
    test('reads what the terminal reports', () async {
      answer = (_) => {
            'tid': '90000001',
            'currencyCode': '978',
            'merchantName': 'Taxi Beispiel'
          };
      final info = await MyPos.posInfo();
      expect(calls.single.method, 'posInfo');
      expect(info?.tid, '90000001');
      expect(info?.currencyCode, '978');
      expect(info?.merchantName, 'Taxi Beispiel');
      expect(info?.merchantId, isNull);
    });

    test('no answer, error or no plugin: null', () async {
      answer = (_) => null;
      expect(await MyPos.posInfo(), isNull);
      answer = (_) => throw PlatformException(code: 'X');
      expect(await MyPos.posInfo(), isNull);
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(channel, null);
      expect(await MyPos.posInfo(), isNull);
    });
  });
}
