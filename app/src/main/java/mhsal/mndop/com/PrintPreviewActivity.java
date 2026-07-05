package mhsal.mndop.com;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileReader;
import java.util.Map;

public class PrintPreviewActivity extends AppCompatActivity {

    private WebView wvPreviewReal;
    private MaterialButton btnPrint;
    private TextView tvReceiptCount;
    private String htmlFilePath;

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection connection;
    private UsbInterface usbInterface;
    private UsbEndpoint endpointOut;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print_preview);

        htmlFilePath = getIntent().getStringExtra("html_file_path");
        int count = getIntent().getIntExtra("receipt_count", 0);

        wvPreviewReal = findViewById(R.id.wv_preview_real);
        btnPrint = findViewById(R.id.btn_print_system);
        tvReceiptCount = findViewById(R.id.tv_receipt_count);

        MaterialButton btnPrev = findViewById(R.id.btn_prev);
        MaterialButton btnNext = findViewById(R.id.btn_next);
        if (btnPrev != null) btnPrev.setVisibility(View.GONE);
        if (btnNext != null) btnNext.setVisibility(View.GONE);

        tvReceiptCount.setText(count + " وصل جاهز للطباعة");

        wvPreviewReal.getSettings().setJavaScriptEnabled(true);
        wvPreviewReal.getSettings().setSupportZoom(true);
        wvPreviewReal.getSettings().setBuiltInZoomControls(true);
        wvPreviewReal.getSettings().setDisplayZoomControls(false);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        displayHtmlContent();

        if (usbManager != null && !usbManager.getDeviceList().isEmpty()) {
            usbDevice = usbManager.getDeviceList().values().iterator().next();
        }

        btnPrint.setText("🖨️ طباعة فورية عبر USB");
        btnPrint.setOnClickListener(v -> startDirectUsbPrint());
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void displayHtmlContent() {
        String html = readFile();
        if (html == null || html.isEmpty()) {
            tvReceiptCount.setText("لا توجد بيانات لعرضها");
            return;
        }

        String customStyle =
                "<style>"
                        + "html,body{"
                        + "margin:0;"
                        + "padding:8px;"
                        + "background:#fff;"
                        + "font-family:Tahoma,Arial,sans-serif;"
                        + "}"
                        + "table{"
                        + "width:100%;"
                        + "border-collapse:collapse;"
                        + "table-layout:auto;"
                        + "}"
                        + "th,td{"
                        + "border:1px solid #000;"
                        + "padding:4px;"
                        + "font-size:13px;"
                        + "text-align:center;"
                        + "vertical-align:middle;"
                        + "white-space:normal;"
                        + "word-break:break-word;"
                        + "}"
                        + "img{"
                        + "max-width:100%;"
                        + "height:auto;"
                        + "}"
                        + "</style>";

        String styledHtml = html.replace("<head>", "<head>" + customStyle);

        WebSettings settings = wvPreviewReal.getSettings();
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        wvPreviewReal.setInitialScale(100);
        wvPreviewReal.setPadding(0, 0, 0, 0);
        wvPreviewReal.setWebViewClient(new WebViewClient());

        wvPreviewReal.loadDataWithBaseURL(null, styledHtml, "text/html", "UTF-8", null);
    }

    private void startDirectUsbPrint() {
        String html = readFile();
        if (html == null || html.isEmpty()) {
            toast("❌ لا توجد بيانات لإرسالها!");
            return;
        }

        if (usbDevice == null) {
            if (usbManager != null && !usbManager.getDeviceList().isEmpty()) {
                usbDevice = usbManager.getDeviceList().values().iterator().next();
            }
            if (usbDevice == null) {
                toast("❌ الطابعة غير متصلة!");
                return;
            }
        }

        toast("جاري تحضير الطباعة...");

        runOnUiThread(() -> {
            try {
                WebView printView = new WebView(PrintPreviewActivity.this);
                printView.getSettings().setJavaScriptEnabled(true);
                // نفس إعدادات WebViewReceiptRenderer: بدون wide viewport حتى
                // تتطابق عرض الصفحة (CSS) مع عرض الالتقاط في PdfPrint (750px)،
                // وإلا يتمدد المحتوى لـ 980px ويُقص الطرف الأيمن من الوصل
                printView.getSettings().setUseWideViewPort(false);
                printView.getSettings().setLoadWithOverviewMode(false);

                String printStyle = "<style>"
                        + "html, body { margin: 0 !important; padding: 10px 30px 10px 10px !important; background: #fff; direction: rtl !important; }"
                        + "table { width: 100% !important; border-collapse: collapse; table-layout: auto; }"
                        + "th, td { border: 1px solid #000 !important; padding: 4px !important; font-size: 13px !important; text-align: center; vertical-align: middle; }"
                        + "</style>";

                String finalPrintHtml = html.replace("<head>", "<head>" + printStyle);

                printView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        int targetWidth = 750;
                        view.measure(
                                View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        );

                        int totalHeight = view.getMeasuredHeight();
                        if (totalHeight <= 0) totalHeight = 500;

                        view.layout(0, 0, targetWidth, totalHeight);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            try {
                                PdfPrint.convertToPcl(view, new PdfPrint.CallbackPrint() {
                                    @Override
                                    public void success(byte[] data) {
                                        sendBytesToUsbPrinter(data);
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        toast("❌ فشل: " + errorMessage);
                                    }
                                });
                            } catch (Exception e) {
                                toast("❌ خطأ: " + e.getMessage());
                            }
                        }, 400);
                    }
                });

                printView.loadDataWithBaseURL(null, finalPrintHtml, "text/html", "UTF-8", null);

            } catch (Exception e) {
                toast("❌ خطأ: " + e.getMessage());
            }
        });
    }

    private void sendBytesToUsbPrinter(byte[] data) {
        new Thread(() -> {
            try {
                if (!connectPrinter()) {
                    runOnUiThread(() -> toast("❌ فشل الاتصال بالطابعة"));
                    return;
                }

                boolean ok = sendBytes(data);
                closePrinter();

                runOnUiThread(() -> toast(ok ? "✅ تمت الطباعة بنجاح!" : "❌ فشل الطباعة"));

            } catch (Exception e) {
                runOnUiThread(() -> toast("❌ خطأ: " + e.getMessage()));
            }
        }).start();
    }

    private boolean connectPrinter() {
        if (usbDevice == null) return false;
        try {
            connection = usbManager.openDevice(usbDevice);
            if (connection == null) return false;
            endpointOut = null;
            usbInterface = null;
            for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
                UsbInterface intf = usbDevice.getInterface(i);
                for (int e = 0; e < intf.getEndpointCount(); e++) {
                    UsbEndpoint ep = intf.getEndpoint(e);
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK && ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                        endpointOut = ep;
                        usbInterface = intf;
                        break;
                    }
                }
                if (endpointOut != null) break;
            }
            if (endpointOut == null || usbInterface == null) return false;
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
                    connection.releaseInterface(usbInterface);
            } catch (Exception ignored) {}
            return connection.claimInterface(usbInterface, true);
        } catch (Exception e) {
            return false;
        }
    }

    private void closePrinter() {
        try {
            if (connection != null && usbInterface != null) connection.releaseInterface(usbInterface);
        } catch (Exception ignored) {}
        try {
            if (connection != null) connection.close();
        } catch (Exception ignored) {}
        connection = null;
        usbInterface = null;
        endpointOut = null;
    }

    private boolean sendBytes(byte[] data) {
        if (connection == null || endpointOut == null) return false;
        int chunkSize = Math.max(endpointOut.getMaxPacketSize(), 512);
        int offset = 0;
        while (offset < data.length) {
            int len = Math.min(chunkSize, data.length - offset);
            byte[] chunk = new byte[len];
            System.arraycopy(data, offset, chunk, 0, len);
            int result = connection.bulkTransfer(endpointOut, chunk, len, 5000);
            if (result < 0) return false;
            offset += len;
        }
        return true;
    }

    private String readFile() {
        if (htmlFilePath == null) return null;
        try {
            File f = new File(htmlFilePath);
            FileReader fr = new FileReader(f);
            char[] buf = new char[(int) f.length()];
            fr.read(buf);
            fr.close();
            return new String(buf);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closePrinter();
        if (htmlFilePath != null) {
            try {
                new File(htmlFilePath).delete();
            } catch (Exception ignored) {}
        }
    }
}