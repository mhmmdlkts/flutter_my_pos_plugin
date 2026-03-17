class MyPosPaymentResponse {
  // --- Exact fields per myPOS "In Person Payments → Responses" ---
  final String? reference_number;          // reference_number
  final String? cardholder_name;           // cardholder_name
  final String? date_time;                 // date_time (YYMMDDHHmmss)
  final String? pan;                       // pan (masked)
  final String? pan_hash;                  // pan_hash
  final int? status;                       // status (TransactionProcessingResult)
  final String? status_text;               // status_text
  final String? card_brand;                // card_brand
  final String? card_entry_mode;           // card_entry_mode (ENTRY_MODE_*)
  final String? response_code;             // response_code
  final String? authorization_code;        // authorization_code
  final bool? signature_required;          // signature_required
  final String? TSI;                       // TSI
  final String? TVR;                       // TVR
  final String? AID;                       // AID
  final String? STAN;                      // STAN
  final String? CVM;                       // CVM (P/S/N)
  final String? application_name;          // application_name
  final bool? transaction_approved;        // transaction_approved
  final bool? dcc_available;               // dcc_available
  final double? amount_dcc;                // amount_dcc
  final String? currency_dcc;              // currency_dcc
  final double? exchange_rate;             // exchange_rate
  final String? TID;                       // TID
  final bool? update_pending;              // update_pending
  final String? resp_code;                 // resp_code (Payment Request)
  final String? expire_date;               // expire_date (Payment Request)

  const MyPosPaymentResponse({
    this.reference_number,
    this.cardholder_name,
    this.date_time,
    this.pan,
    this.pan_hash,
    this.status,
    this.status_text,
    this.card_brand,
    this.card_entry_mode,
    this.response_code,
    this.authorization_code,
    this.signature_required,
    this.TSI,
    this.TVR,
    this.AID,
    this.STAN,
    this.CVM,
    this.application_name,
    this.transaction_approved,
    this.dcc_available,
    this.amount_dcc,
    this.currency_dcc,
    this.exchange_rate,
    this.TID,
    this.update_pending,
    this.resp_code,
    this.expire_date,
  });

  factory MyPosPaymentResponse.fromMap(Map<String, dynamic> m) {
    return MyPosPaymentResponse(
      reference_number: m['reference_number']?.toString(),
      cardholder_name: m['cardholder_name']?.toString(),
      date_time: m['date_time']?.toString(),
      pan: m['pan']?.toString(),
      pan_hash: m['pan_hash']?.toString(),
      status: _asInt(m['status']),
      status_text: m['status_text']?.toString(),
      card_brand: m['card_brand']?.toString(),
      card_entry_mode: m['card_entry_mode']?.toString(),
      response_code: m['response_code']?.toString(),
      authorization_code: m['authorization_code']?.toString(),
      signature_required: _asBool(m['signature_required']),
      TSI: m['TSI']?.toString(),
      TVR: m['TVR']?.toString(),
      AID: m['AID']?.toString(),
      STAN: m['STAN']?.toString(),
      CVM: m['CVM']?.toString(),
      application_name: m['application_name']?.toString(),
      transaction_approved: _asBool(m['transaction_approved']),
      dcc_available: _asBool(m['dcc_available']),
      amount_dcc: _asDouble(m['amount_dcc']),
      currency_dcc: m['currency_dcc']?.toString(),
      exchange_rate: _asDouble(m['exchange_rate']),
      TID: m['TID']?.toString(),
      update_pending: _asBool(m['update_pending']),
      resp_code: m['resp_code']?.toString(),
      expire_date: m['expire_date']?.toString(),
    );
  }

  Map<String, dynamic> toMap() => {
        'reference_number': reference_number,
        'cardholder_name': cardholder_name,
        'date_time': date_time,
        'pan': pan,
        'pan_hash': pan_hash,
        'status': status,
        'status_text': status_text,
        'card_brand': card_brand,
        'card_entry_mode': card_entry_mode,
        'response_code': response_code,
        'authorization_code': authorization_code,
        'signature_required': signature_required,
        'TSI': TSI,
        'TVR': TVR,
        'AID': AID,
        'STAN': STAN,
        'CVM': CVM,
        'application_name': application_name,
        'transaction_approved': transaction_approved,
        'dcc_available': dcc_available,
        'amount_dcc': amount_dcc,
        'currency_dcc': currency_dcc,
        'exchange_rate': exchange_rate,
        'TID': TID,
        'update_pending': update_pending,
        'resp_code': resp_code,
        'expire_date': expire_date,
      };

  @override
  String toString() {
    return 'MyPosPaymentResponse(${toMap()})';
  }

  static int? _asInt(dynamic v) {
    if (v == null) return null;
    if (v is int) return v;
    return int.tryParse(v.toString());
  }
  static double? _asDouble(dynamic v) {
    if (v == null) return null;
    if (v is double) return v;
    if (v is int) return v.toDouble();
    return double.tryParse(v.toString());
  }
  static bool? _asBool(dynamic v) {
    if (v == null) return null;
    if (v is bool) return v;
    final s = v.toString().toLowerCase();
    if (s == 'true' || s == '1' || s == 'yes') return true;
    if (s == 'false' || s == '0' || s == 'no') return false;
    return null;
  }
}