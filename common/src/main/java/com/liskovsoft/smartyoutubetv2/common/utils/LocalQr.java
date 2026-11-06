package com.liskovsoft.smartyoutubetv2.common.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

/** Local-only rendering. Payloads never leave this process. */
public final class LocalQr {
    private LocalQr() {}

    public static Bitmap render(String text) {
        if (text == null || text.length() > 4096) return null;
        try {
            BitMatrix m = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, 512, 512);
            int[] pixels = new int[512 * 512];
            for (int y = 0; y < 512; y++)
                for (int x = 0; x < 512; x++)
                    pixels[y * 512 + x] = m.get(x, y) ? 0xff000000 : 0xffffffff;
            return Bitmap.createBitmap(pixels, 512, 512, Bitmap.Config.ARGB_8888);
        } catch (Exception failure) {
            return null;
        }
    }

    public static void show(Context context, String text) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        ImageView image = new ImageView(context);
        image.setImageBitmap(render(text));
        content.addView(image, new LinearLayout.LayoutParams(512, 512));
        TextView link = new TextView(context);
        link.setText(text);
        content.addView(link);
        new AlertDialog.Builder(context)
                .setTitle("QR code")
                .setView(content)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
