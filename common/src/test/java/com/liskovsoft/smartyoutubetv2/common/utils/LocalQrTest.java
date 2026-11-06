package com.liskovsoft.smartyoutubetv2.common.utils;

import static org.junit.Assert.*;

import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class LocalQrTest {
    @Test
    public void roundTripsActivationUrlWithoutNetwork() throws Exception {
        String value = "https://youtube.com/qr/activate/TEST-123";
        android.graphics.Bitmap bitmap = LocalQr.render(value);
        assertNotNull(bitmap);
        int[] pixels = new int[512 * 512];
        bitmap.getPixels(pixels, 0, 512, 0, 0, 512, 512);
        String decoded =
                new MultiFormatReader()
                        .decode(
                                new BinaryBitmap(
                                        new HybridBinarizer(
                                                new RGBLuminanceSource(512, 512, pixels))))
                        .getText();
        assertEquals(value, decoded);
    }
}
