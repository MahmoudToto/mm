package mhsal.mndop.com;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * كلاس مشترك يحوّل صفحة HTML (وصل/كمبيالة) إلى Bitmap عبر WebView مخفي.
 * يُستخدم في PrintPreviewActivity (للمعاينة والطباعة عبر PrintDocument)
 * وفي PrintActivity (للطباعة المباشرة عبر USB)، لتجنّب تكرار نفس المنطق
 * الحسّاس (انتظار جاهزية الصفحة، measure/layout، إلخ) في مكانين.
 *
 * ملاحظة مهمة: كل دوال هذا الكلاس يجب استدعاؤها من UI Thread فقط،
 * لأن WebView لا يمكن استخدامه من Thread آخر.
 */
public class WebViewReceiptRenderer {

    public interface SnapshotCallback {
        /** bmp تكون null لو فشل التحميل أو التصوير لأي سبب */
        void onSnapshot(Bitmap bmp);
    }

    private final Activity activity;
    private final WebView  webView;
    private final Handler  mainHandler = new Handler(Looper.getMainLooper());

    private final int renderW;
    private final int renderH;

    // أقصى وقت ننتظره قبل ما نعتبر إن إشارة الجاهزية من JS لن تصل (احتياطي فقط)
    private static final long READY_TIMEOUT_MS = 3500;
    // فاصل أمان صغير بعد إشارة "جاهز" قبل التصوير (لضمان اكتمال آخر رسم)
    private static final long SNAPSHOT_GRACE_MS = 150;

    // رقم تسلسلي لكل طلب رندر — يمنع تنفيذ callback متأخر من طلب سابق
    private int renderToken = 0;
    private SnapshotCallback pendingCallback;

    /**
     * @param activity الـ Activity المالكة (لـ getFilesDir() وتشغيل الكود على UI Thread)
     * @param webView  WebView مخفي مُعدّ مسبقاً (مرفق فعلياً لشجرة العرض، visibility=INVISIBLE)
     * @param renderW  عرض الالتقاط بالبكسل
     * @param renderH  ارتفاع الالتقاط بالبكسل
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public WebViewReceiptRenderer(Activity activity, WebView webView, int renderW, int renderH) {
        this.activity = activity;
        this.webView  = webView;
        this.renderW  = renderW;
        this.renderH  = renderH;

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDefaultTextEncodingName("UTF-8");
        s.setUseWideViewPort(false);
        s.setLoadWithOverviewMode(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);

        // جسر JS↔Android: الصفحة تنادي هذا بعد اكتمال window.onload
        // (وبالتبعية fitText() الموجودة في ReceiptBuilder)، فلا نعتمد
        // على تايمر تخميني لمعرفة جاهزية المحتوى
        webView.addJavascriptInterface(new RenderReadyBridge(), "AndroidReady");
    }

    private class RenderReadyBridge {
        @JavascriptInterface
        public void onReady(int token) {
            mainHandler.post(() -> onPageReady(token));
        }
    }

    /**
     * يحمّل HTML في الـ WebView وينده الـ callback بالـ Bitmap الناتج،
     * بعد ما الصفحة تخلص تحميل وضبط نفسها فعلياً (لا أكتر ولا أقل).
     *
     * @param html      محتوى الصفحة (ناتج ReceiptBuilder.buildHtml مثلاً)
     * @param fileName  اسم الملف المؤقت الذي سيُكتب في getFilesDir()
     * @param callback  يُستدعى على UI Thread دائماً
     */
    public void render(String html, String fileName, SnapshotCallback callback) {
        final int token = ++renderToken;
        pendingCallback = callback;

        String htmlWithSignal = injectReadySignal(html, token);
        File f = writeFile(htmlWithSignal, fileName);
        if (f == null) {
            finish(token, null);
            return;
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                finish(token, null);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // احتياطي فقط: لو JS لم ينادِ onReady لأي سبب (استثناء
                // غير متوقع، أو تعطيل JS)، نُكمل بعد أقصى انتظار بدل
                // ما العملية تتعلق للأبد
                mainHandler.postDelayed(() -> {
                    if (renderToken == token && pendingCallback != null) {
                        finish(token, snap(view));
                    }
                }, READY_TIMEOUT_MS);
            }
        });

        webView.loadUrl("file://" + f.getAbsolutePath());
    }

    /** بيتنفذ لما JS ينادي AndroidReady.onReady(token) */
    private void onPageReady(int token) {
        if (token != renderToken || pendingCallback == null) return;
        // فاصل صغير لضمان اكتمال آخر رسم (repaint) داخل WebView قبل التصوير
        mainHandler.postDelayed(() -> finish(token, snap(webView)), SNAPSHOT_GRACE_MS);
    }

    /** ينفّذ الـ callback مرة واحدة بس، ويتجاهل أي نداء متأخر من token قديم */
    private void finish(int token, Bitmap bmp) {
        if (token != renderToken) return; // طلب قديم تم تجاوزه، تجاهله
        SnapshotCallback cb = pendingCallback;
        pendingCallback = null;
        if (cb != null) cb.onSnapshot(bmp);
    }

    /**
     * يحقن سكريبت صغير في نهاية &lt;body&gt; ينادي AndroidReady.onReady(token)
     * بعد اكتمال حدث "load" — أي بعد ما window.onload الأصلي (المُعرّف
     * داخل ReceiptBuilder ويشمل fitText()) يكون خلص شغله، لأن المتصفح
     * بينفذ مستمعي حدث load بترتيب تسجيلهم. لا ينادي fitText() بنفسه
     * لتجنّب تنفيذها مرتين بلا داعي.
     */
    private String injectReadySignal(String html, int token) {
        String signal = "<script>"
                + "(function(){"
                + "  function notifyReady(){"
                + "    if (window.AndroidReady && window.AndroidReady.onReady) {"
                + "      window.AndroidReady.onReady(" + token + ");"
                + "    }"
                + "  }"
                + "  if (document.readyState === 'complete') {"
                + "    notifyReady();"
                + "  } else {"
                + "    window.addEventListener('load', notifyReady);"
                + "  }"
                + "})();"
                + "</script></body>";
        return html.replace("</body>", signal);
    }

    /**
     * يلتقط Bitmap من الـ WebView. يعيد measure/layout قبل كل تصوير
     * عمداً — كل loadUrl() يغيّر المحتوى الداخلي، وبدون إعادة هذه
     * الخطوة هنا سيُرسم الـ View بحالة layout قديمة فيظهر جزء بسيط
     * فقط من بداية المحتوى بدل الوصل كامل.
     */
    private Bitmap snap(WebView view) {
        try {
            // تأكد من أن الخلفية بيضاء صريحة قبل الرسم
            view.setBackgroundColor(Color.WHITE);

            view.measure(
                    View.MeasureSpec.makeMeasureSpec(renderW, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(renderH, View.MeasureSpec.EXACTLY)
            );
            view.layout(0, 0, renderW, renderH);

            Bitmap bmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            // مسح الخلفية باللون الأبيض لضمان عدم وجود أجزاء شفافة
            c.drawColor(Color.WHITE);
            view.draw(c);
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private File writeFile(String html, String name) {
        try {
            File f = new File(activity.getFilesDir(), name);
            OutputStreamWriter w = new OutputStreamWriter(
                    new FileOutputStream(f), StandardCharsets.UTF_8);
            w.write(html);
            w.close();
            return f;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * يجب استدعاؤها من onDestroy() الخاصة بالـ Activity المالكة،
     * لإلغاء أي عملية رندر معلّقة ومنع أي callback متأخر من العمل
     * بعد تدمير الـ Activity، وتدمير الـ WebView نفسه.
     */
    public void destroy() {
        renderToken++; // يُسقط أي طلب جارٍ
        pendingCallback = null;
        mainHandler.removeCallbacksAndMessages(null);
        webView.destroy();
    }
}