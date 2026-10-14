import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:my_pos/enums/my_pos_currency_enum.dart';
import 'package:my_pos/models/my_pos_paper.dart';
import 'package:my_pos/my_pos.dart';

void main() => runApp(const ExampleApp());

class ExampleApp extends StatelessWidget {
  const ExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'my_pos example',
      theme: ThemeData(colorSchemeSeed: const Color(0xFF2E3133)),
      home: const ExamplePage(),
    );
  }
}

class ExamplePage extends StatefulWidget {
  const ExamplePage({super.key});

  @override
  State<ExamplePage> createState() => _ExamplePageState();
}

class _ExamplePageState extends State<ExamplePage> {
  String _log = 'Run this app on a myPOS Smart terminal.';
  bool _busy = false;

  Future<void> _run(Future<String> Function() action) async {
    setState(() => _busy = true);
    String text;
    try {
      text = await action();
    } on PlatformException catch (e) {
      text = 'Error ${e.code}: ${e.message}';
    }
    if (!mounted) return;
    setState(() {
      _busy = false;
      _log = text;
    });
  }

  Future<String> _pay() async {
    final res = await MyPos.makePayment(
      amount: 1.00,
      currency: MyPosCurrency.eur,
      printCustomerReceipt: false,
      printMerchantReceipt: false,
      reference: 'EXAMPLE-1',
    );
    if (res.isCancelled) return 'Payment cancelled.';
    if (!res.isApproved) return 'Declined (response code ${res.response_code}).';
    return 'Approved, reference ${res.reference_number}, STAN ${res.STAN}.';
  }

  Future<String> _print() async {
    final paper = MyPosPaper()
      ..addText('my_pos example', fontSize: 32, alignment: PrinterAlignment.center)
      ..addSpace(1)
      ..addDoubleText('Taxi ride', 'EUR 1,00')
      ..addDoubleText('Total', 'EUR 1,00')
      ..addSpace(1)
      ..addQrCode('https://kasseneck.at')
      ..addText('Cash register by Kasseneck', alignment: PrinterAlignment.center)
      ..addCutLine();
    final result = await paper.print();
    return 'Print: ${result.name}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('my_pos example')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          FilledButton(
            onPressed: _busy ? null : () => _run(_pay),
            child: const Text('Charge EUR 1.00'),
          ),
          const SizedBox(height: 12),
          OutlinedButton(
            onPressed: _busy ? null : () => _run(_print),
            child: const Text('Print test receipt'),
          ),
          const SizedBox(height: 24),
          Text(_log),
        ],
      ),
    );
  }
}
