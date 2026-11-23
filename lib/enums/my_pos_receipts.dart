/// Which receipts the terminal prints itself for a void or refund.
enum MyPosReceipts {
  none(printMerchant: false, printCustomer: false),
  merchant(printMerchant: true, printCustomer: false),
  customer(printMerchant: false, printCustomer: true),
  both(printMerchant: true, printCustomer: true);

  const MyPosReceipts(
      {required this.printMerchant, required this.printCustomer});

  final bool printMerchant;
  final bool printCustomer;
}
