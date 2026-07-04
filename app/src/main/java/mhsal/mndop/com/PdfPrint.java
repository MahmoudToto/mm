package mhsal.mndop.com;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;

public class PdfPrint {

    public interface CallbackPrint {
        void success(byte[] pclData);
        void onFailure(String error);
    }

    private static final int DPI = 203;
    private static final int TARGET_WIDTH = 750;

    public static void convertToPcl(final WebView webView, final CallbackPrint callback) {
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                // قياس المحتوى بالكامل
                int contentWidth = TARGET_WIDTH;
                int contentHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

                webView.measure(
                        View.MeasureSpec.makeMeasureSpec(contentWidth, View.MeasureSpec.EXACTLY),
                        contentHeight
                );

                int measuredHeight = webView.getMeasuredHeight();
                if (measuredHeight <= 0) measuredHeight = 1200;

                webView.layout(0, 0, contentWidth, measuredHeight);

                // رسم الصورة
                Bitmap bmp = Bitmap.createBitmap(contentWidth, measuredHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);

                canvas.drawColor(Color.WHITE);

                canvas.translate(-10, 0);

                webView.draw(canvas);
                // تحويل للـ PCL
                byte[] pcl = buildPcl(bmp, contentWidth, measuredHeight);
                bmp.recycle();

                callback.success(pcl);

            } catch (Exception e) {
                callback.onFailure(e.getMessage() != null ? e.getMessage() : e.toString());
            }
        });
    }

    private static byte[] buildPcl(Bitmap bmp, int w, int h) throws Exception {
        // تكبير الصورة لتناسب عرض الورقة (220mm)
        int targetWidth = (int) (220.0 / 25.4 * DPI); // ≈ 1757px
        float scale = (float) targetWidth / w;
        int targetHeight = (int) (h * scale);

        Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true);
        bmp.recycle();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // ============================================================
        // أوامر PCL الأساسية فقط - من غير أي أوامر حجم ورق
        // ============================================================

        // 1. إعادة ضبط الطابعة
        write(out, "\u001BE");

        // 2. اتجاه Landscape
        write(out, "\u001B&l1O");

        // 3. هامش علوي وأيسر = 0
        write(out, "\u001B&l0E");
        write(out, "\u001B&l0U");

        // 4. ضبط DPI
        write(out, "\u001B*t" + DPI + "R");

        // 5. بدء الراستر
        write(out, "\u001B*r0F");
        write(out, "\u001B*r" + targetWidth + "S");
        write(out, "\u001B*r" + targetHeight + "T");
        write(out, "\u001B*b0M");
        write(out, "\u001B*r1A");

        // 6. تحويل البكسل لـ PCL
        int rowBytes = (targetWidth + 7) / 8;
        for (int y = 0; y < targetHeight; y++) {
            byte[] row = new byte[rowBytes];
            for (int x = 0; x < targetWidth; x++) {
                int pixel = scaledBmp.getPixel(x, y);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);
                int gray = (r * 299 + g * 587 + b * 114) / 1000;
                if (gray < 200) {
                    row[x / 8] |= (byte) (0x80 >> (x % 8));
                }
            }
            write(out, "\u001B*b" + rowBytes + "W");
            out.write(row);
        }

        // 7. إنهاء الراستر
        write(out, "\u001B*rB");

        // 8. طرد الورقة
        out.write(0x0C);

        // 9. إعادة ضبط الطابعة
        write(out, "\u001BE");

        scaledBmp.recycle();

        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String s) throws Exception {
        out.write(s.getBytes("ISO-8859-1"));
    }
}