import 'dart:convert';

import 'package:my_pos/my_pos.dart';

import '../enums/py_pos_print_response.dart';


enum PrinterAlignment {
  left,
  center,
  right,
}

class MyPosPaper {
  static String cutLineImage = "iVBORw0KGgoAAAANSUhEUgAAAoAAAABICAYAAABm6UTTAAAACXBIWXMAAA7EAAAOxAGVKw4bAAAHiWlUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4gPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iQWRvYmUgWE1QIENvcmUgOS4wLWMwMDEgNzkuYzAyMDRiMmRlZiwgMjAyMy8wMi8wMi0xMjoxNDoyNCAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6QXR0cmliPSJodHRwOi8vbnMuYXR0cmlidXRpb24uY29tL2Fkcy8xLjAvIiB4bWxuczpkYz0iaHR0cDovL3B1cmwub3JnL2RjL2VsZW1lbnRzLzEuMS8iIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIgeG1sbnM6cGhvdG9zaG9wPSJodHRwOi8vbnMuYWRvYmUuY29tL3Bob3Rvc2hvcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIgZGM6Zm9ybWF0PSJpbWFnZS9wbmciIHhtcDpDcmVhdG9yVG9vbD0iQ2FudmEiIHhtcDpDcmVhdGVEYXRlPSIyMDIzLTExLTA5VDA0OjA4OjA4KzAxOjAwIiB4bXA6TW9kaWZ5RGF0ZT0iMjAyMy0xMS0wOVQwNDowOTo1MCswMTowMCIgeG1wOk1ldGFkYXRhRGF0ZT0iMjAyMy0xMS0wOVQwNDowOTo1MCswMTowMCIgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDplZjBhZmM2Mi1jZTU5LTQ0OGMtODBlMC04NTY3ZDlkMWMxNGUiIHhtcE1NOkRvY3VtZW50SUQ9ImFkb2JlOmRvY2lkOnBob3Rvc2hvcDpjMWZiNTA5Ny0zZDlkLTAzNDAtOTUxYi01YzRiZjUwYjdjYjUiIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDpiODUzMWMyMC0yOTJlLTQxMDMtYTZlOS0zMTZlYjhlMzhlMWEiPiA8QXR0cmliOkFkcz4gPHJkZjpTZXE+IDxyZGY6bGkgQXR0cmliOkNyZWF0ZWQ9IjIwMjMtMTEtMDkiIEF0dHJpYjpFeHRJZD0iOGY4NGEzOWEtZGM0MS00NGE1LTlhOGMtZTU2NjBkYzM4Y2YzIiBBdHRyaWI6RmJJZD0iNTI1MjY1OTE0MTc5NTgwIiBBdHRyaWI6VG91Y2hUeXBlPSIyIi8+IDwvcmRmOlNlcT4gPC9BdHRyaWI6QWRzPiA8ZGM6dGl0bGU+IDxyZGY6QWx0PiA8cmRmOmxpIHhtbDpsYW5nPSJ4LWRlZmF1bHQiPlNvY2lhbCBNZWRpYSBDb250ZW50IERlc2lnbiB1bmQgTWFuYWdlbWVudCBPZmZlciAtIDk8L3JkZjpsaT4gPC9yZGY6QWx0PiA8L2RjOnRpdGxlPiA8ZGM6Y3JlYXRvcj4gPHJkZjpTZXE+IDxyZGY6bGk+RUNFIEFSU0xBTjwvcmRmOmxpPiA8L3JkZjpTZXE+IDwvZGM6Y3JlYXRvcj4gPHhtcE1NOkhpc3Rvcnk+IDxyZGY6U2VxPiA8cmRmOmxpIHN0RXZ0OmFjdGlvbj0ic2F2ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6Yjg1MzFjMjAtMjkyZS00MTAzLWE2ZTktMzE2ZWI4ZTM4ZTFhIiBzdEV2dDp3aGVuPSIyMDIzLTExLTA5VDA0OjA5OjUwKzAxOjAwIiBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZG9iZSBQaG90b3Nob3AgMjQuNSAoTWFjaW50b3NoKSIgc3RFdnQ6Y2hhbmdlZD0iLyIvPiA8cmRmOmxpIHN0RXZ0OmFjdGlvbj0ic2F2ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6ZWYwYWZjNjItY2U1OS00NDhjLTgwZTAtODU2N2Q5ZDFjMTRlIiBzdEV2dDp3aGVuPSIy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";
  final List<Map<String, dynamic>> commands = [];

  void addCutLine() {
    addImageBase64(cutLineImage);
  }

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

  void addQrCode(String qrCodeText, {int? size = 200}) async {
    commands.add({
      'type': 'qrCode',
      'size': size,
      'value': qrCodeText,
    });
  }

  Future<PrintResponse> print() async {
    return MyPos.printPaper(this);
  }

  @override
  String toString() {
    String val = '';

    int paperSize = 32;
    val += '\n' * 3;
    val += '${'#' * (paperSize + 4)}\n';
    for (var element in commands) {
      String line = '';
      switch (element['type']) {
        case 'text':
          int padLeft = 0;
          if (element['alignment'] == PrinterAlignment.center.index) {
            padLeft = (paperSize - element['value'].toString().length)~/2;
          } else if (element['alignment'] == PrinterAlignment.left.index) {
            padLeft = 0;
          } else if (element['alignment'] == PrinterAlignment.right.index) {
            padLeft = paperSize - element['value'].toString().length;
          }
          line += (' '*padLeft) + element['value'];
          break;
        case 'doubleText':
          line += '${element['leftValue']} ${element['rightValue'].toString().padLeft((paperSize-1)-element['leftValue'].toString().length)}';
          break;
        case 'image':
          if (element['value'] == cutLineImage) {
            line += '${'-' * (paperSize-3)} >8';
            break;
          }
          line += '${(' '*((paperSize - 7)~/2))}(Image)';
          break;
        case 'qrCode':
          line += '${' '*((paperSize - 4)~/2)}(QR)';
          break;
        case 'space':
          line += ' ' * paperSize;
          break;
      }
      line = '| $line${' ' * ((paperSize) - line.length)} |';
      val += '$line\n';
    }
    val += '${'#' * (paperSize + 4)}\n';
    val += '\n' * 3;
    return val;
  }
}
