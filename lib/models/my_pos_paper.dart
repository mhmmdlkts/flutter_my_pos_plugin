import 'dart:convert';


enum PrinterAlignment {
  left,
  center,
  right,
}

class MyPosPaper {
  final List<Map<String, dynamic>> commands = [];

  void addText(String text, {int fontSize = 24, PrinterAlignment alignment = PrinterAlignment.left}) {
    commands.add({
      'type': 'text',
      'value': text,
      'fontSize': fontSize,
      'alignment': alignment.index,
    });
  }

  void addDoubleText(String leftText, String rightText) {
    commands.add({
      'type': 'doubleText',
      'leftValue': leftText,
      'rightValue': rightText,
    });
  }

  void addSpace(int lines) {
    for (int i = 0; i < lines; i++) {
      commands.add({'type': 'space'});
    }
  }

  void addImage(List<int> byteData) {
    addImageBase64(base64Encode(byteData));
  }

  void addImageBase64(String base64Image) {
    commands.add({
      'type': 'image',
      'value': base64Image,
    });
  }

  Map<String, dynamic> toJson() => {
    'commands': commands,
  };

  void addQrCode(String qrCodeText) async {
    commands.add({
      'type': 'qrCode',
      'value': qrCodeText,
    });
  }


}
