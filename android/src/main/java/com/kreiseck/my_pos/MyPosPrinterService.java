package com.kreiseck.my_pos;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.Toast;

import com.google.gson.Gson;
import com.mypos.smartsdk.MyPOSAPI;
import com.mypos.smartsdk.MyPOSUtil;
import com.mypos.smartsdk.print.PrinterCommand;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public class MyPosPrinterService {

    public static void showToast(Activity context, final String toast) {
        context.runOnUiThread(new Runnable() {
            public void run()
            {
                Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
            }
        });
    }
    protected static void printPaper(Activity context, List<Map<String, Object>> data) {
        List<PrinterCommand> commands = new ArrayList<>();

        for (Map<String, Object> commandData : data) {
            String type = (String) commandData.get("type");

            switch (type) {
                case "text":
                    String textValue = (String) commandData.get("value");
                    int fontSize = (int) commandData.get("fontSize");
                    PrinterCommand.Alignment alignment = PrinterCommand.Alignment.values()[(int) commandData.get("alignment")];
                    commands.add(new PrinterCommand(PrinterCommand.CommandType.TEXT, textValue, fontSize, alignment));
                    break;
                case "doubleText":
                    String leftValue = (String) commandData.get("leftValue");
                    String rightValue = (String) commandData.get("rightValue");
                    commands.add(new PrinterCommand(PrinterCommand.CommandType.TEXT, leftValue, rightValue, PrinterCommand.RECEIPT_SMART_MAX_CHARS_PER_LINE));
                    break;
                case "space":
                    commands.add(new PrinterCommand(PrinterCommand.CommandType.TEXT, "\n"));
                    break;
                case "image":
                    String base64Image = (String) commandData.get("value");
                    byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap bitmapImg = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    commands.add(new PrinterCommand(PrinterCommand.CommandType.IMAGE, bitmapImg));
                    break;
                case "qrCode":
                    Bitmap bitmapQr = QRCodeGenerator.generateQRCode((String) commandData.get("value"));
                    if (bitmapQr != null) {
                        commands.add(new PrinterCommand(PrinterCommand.CommandType.IMAGE, bitmapQr));
                    }
                    break;
            }
        }

        Gson gson = new Gson();
        String jsonCommands = gson.toJson(commands);
        Intent intent = new Intent(MyPOSUtil.PRINT_BROADCAST);
        intent.putExtra("commands", jsonCommands);
        context.sendBroadcast(intent);
        MyPOSAPI.sendExplicitBroadcast(context, intent);
    }


}
