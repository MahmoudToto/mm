package mhsal.mndop.com;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class PrintActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION_OUTER = "mhsal.mndop.com.USB_PRINTER_PERM_OUTER";

    private Spinner        spinnerUser, spinnerYear, spinnerMonth;
    private MaterialButton btnLoadSales, btnPrint, btnDetectPrinter;
    private RecyclerView   rvInvoices;
    private ProgressBar    progressBar;
    private TextView       tvStatus, tvPrinterStatus;
    private LinearLayout   layoutFilters;
    private RadioGroup     rgInstallmentMode;
    private RadioButton    rbAllInstallments, rbByNumber, rbByDate;
    private ChipGroup      cgInstallmentNumbers;
    private Spinner        spinnerInstallmentMonth, spinnerInstallmentYear;
    private LinearLayout   layoutByNumber, layoutByDate;
    private android.webkit.WebView wvOffscreen;

    private final List<String>     userList      = new ArrayList<>();
    private final List<String>     yearList      = new ArrayList<>();
    private final List<String>     monthList     = new ArrayList<>();
    private final List<SaleRecord> allSales      = new ArrayList<>();
    private final List<SaleRecord> selectedSales = new ArrayList<>();

    private String selectedUser  = "";
    private String selectedYear  = "";
    private int    selectedMonth = 0;

    private CompanyInfo companyInfo = new CompanyInfo();

    private UsbManager          usbManager;
    private UsbDevice           usbDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_print);
        initViews();
        loadCompanyInfo();
        loadUsers();
        setupInstallmentModeListener();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    private void initViews() {
        spinnerUser             = findViewById(R.id.spinner_user);
        spinnerYear             = findViewById(R.id.spinner_year);
        spinnerMonth            = findViewById(R.id.spinner_month);
        btnLoadSales            = findViewById(R.id.btn_load_sales);
        btnPrint                = findViewById(R.id.btn_print);
        btnDetectPrinter        = findViewById(R.id.btn_detect_printer);
        tvPrinterStatus         = findViewById(R.id.tv_printer_status);
        rvInvoices              = findViewById(R.id.rv_invoices);
        progressBar             = findViewById(R.id.progress_print);
        tvStatus                = findViewById(R.id.tv_status);
        layoutFilters           = findViewById(R.id.layout_filters);
        rgInstallmentMode       = findViewById(R.id.rg_installment_mode);
        rbAllInstallments       = findViewById(R.id.rb_all_installments);
        rbByNumber              = findViewById(R.id.rb_by_number);
        rbByDate                = findViewById(R.id.rb_by_date);
        cgInstallmentNumbers    = findViewById(R.id.cg_installment_numbers);
        spinnerInstallmentMonth = findViewById(R.id.spinner_installment_month);
        spinnerInstallmentYear  = findViewById(R.id.spinner_installment_year);
        layoutByNumber          = findViewById(R.id.layout_by_number);
        layoutByDate            = findViewById(R.id.layout_by_date);
        wvOffscreen             = findViewById(R.id.wv_offscreen);

        rvInvoices.setLayoutManager(new LinearLayoutManager(this));

        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int y = 2024; y <= thisYear + 1; y++) yearList.add(String.valueOf(y));
        spinnerYear.setAdapter(simpleAdapter(yearList));
        spinnerYear.setSelection(yearList.indexOf(String.valueOf(thisYear)));

        String[] months = {"1","2","3","4","5","6","7","8","9","10","11","12"};
        for (String m : months) monthList.add(m);
        spinnerMonth.setAdapter(simpleAdapter(monthList));
        spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH));

        spinnerInstallmentYear.setAdapter(simpleAdapter(yearList));
        spinnerInstallmentYear.setSelection(yearList.indexOf(String.valueOf(thisYear)));
        spinnerInstallmentMonth.setAdapter(simpleAdapter(monthList));
        spinnerInstallmentMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH));

        btnLoadSales.setOnClickListener(v -> loadSales());
        btnPrint.setOnClickListener(v -> startPrinting());
        btnDetectPrinter.setOnClickListener(v -> manualDetectPrinter());

        layoutFilters.setVisibility(View.GONE);
        btnPrint.setVisibility(View.GONE);
    }

    private void setupInstallmentModeListener() {
        rgInstallmentMode.setOnCheckedChangeListener((group, checkedId) -> {
            layoutByNumber.setVisibility(checkedId == R.id.rb_by_number ? View.VISIBLE : View.GONE);
            layoutByDate.setVisibility(checkedId == R.id.rb_by_date     ? View.VISIBLE : View.GONE);
        });
    }

    private void loadCompanyInfo() {
        FirebaseDatabase.getInstance().getReference("info")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        companyInfo.name   = getString(snap, "name",   "شركة");
                        companyInfo.text   = getString(snap, "text",   "");
                        companyInfo.branch = getString(snap, "branch", "");
                        companyInfo.phone  = getString(snap, "phone",  "");
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    private void loadUsers() {
        setLoading(true);
        FirebaseDatabase.getInstance().getReference("user")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        userList.clear();
                        for (DataSnapshot child : snap.getChildren()) {
                            String name = child.child("name").getValue(String.class);
                            if (name != null && !name.isEmpty())
                                userList.add(child.getKey());
                        }
                        if (userList.isEmpty()) {
                            toast("لا يوجد مستخدمون");
                            setLoading(false);
                            return;
                        }
                        spinnerUser.setAdapter(simpleAdapter(userList));
                        selectedUser = userList.get(0);
                        spinnerUser.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                                selectedUser = userList.get(pos);
                            }
                            @Override public void onNothingSelected(AdapterView<?> p) {}
                        });
                        setLoading(false);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        toast("فشل تحميل المستخدمين");
                        setLoading(false);
                    }
                });
    }

    private void loadSales() {
        selectedYear  = yearList.get(spinnerYear.getSelectedItemPosition());
        selectedMonth = Integer.parseInt(monthList.get(spinnerMonth.getSelectedItemPosition()));

        if (selectedUser.isEmpty()) { toast("اختر مستخدماً أولاً"); return; }

        setLoading(true);
        tvStatus.setText("جاري تحميل الفواتير...");
        allSales.clear();
        layoutFilters.setVisibility(View.GONE);
        btnPrint.setVisibility(View.GONE);

        String path = "sales/" + selectedUser + "/" + selectedYear + "/" + selectedMonth;
        FirebaseDatabase.getInstance().getReference(path)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        for (DataSnapshot child : snap.getChildren()) {
                            SaleRecord sr = parseSale(child);
                            if (sr != null) allSales.add(sr);
                        }
                        setLoading(false);
                        if (allSales.isEmpty()) {
                            tvStatus.setText("لا توجد فواتير في هذا الشهر");
                            return;
                        }
                        tvStatus.setText("تم تحميل " + allSales.size() + " فاتورة");
                        showInvoiceList();
                        buildInstallmentChips();
                        layoutFilters.setVisibility(View.VISIBLE);
                        btnPrint.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        setLoading(false);
                        toast("فشل تحميل الفواتير: " + e.getMessage());
                    }
                });
    }

    private SaleRecord parseSale(DataSnapshot snap) {
        try {
            SaleRecord sr    = new SaleRecord();
            sr.firebaseKey   = snap.getKey();
            sr.id            = getLong(snap, "id");
            sr.customerName  = getString(snap, "customerName", "");
            sr.area          = getString(snap, "area", "");
            sr.address       = getString(snap, "address", "");
            sr.phone         = getString(snap, "phone", "");
            sr.paymentType   = getString(snap, "paymentType", "cash");
            sr.installment   = getString(snap, "installment", "");
            sr.totalAmount   = getDouble(snap, "totalAmount");
            sr.downPayment   = getDouble(snap, "downPayment");
            sr.remaining     = getDouble(snap, "remaining");
            sr.timestamp     = getLong(snap, "timestamp");
            sr.salesperson   = getString(snap, "salespersonName", selectedUser);

            sr.products = new ArrayList<>();
            for (DataSnapshot p : snap.child("products").getChildren()) {
                ProductLine pl = new ProductLine();
                pl.name       = getString(p, "productName", "منتج");
                pl.unitPrice  = getDouble(p, "unitPrice");
                pl.quantity   = (int) getLong(p, "quantity");
                pl.totalPrice = getDouble(p, "totalPrice");
                sr.products.add(pl);
            }

            sr.installmentValues = parseInstallmentPlan(sr.installment);
            return sr;
        } catch (Exception e) {
            return null;
        }
    }

    private List<Long> parseInstallmentPlan(String plan) {
        List<Long> result = new ArrayList<>();
        if (plan == null || plan.isEmpty()) return result;
        try {
            String norm = arabicToWestern(plan.trim())
                    .replace("×", "*").replace("x", "*").replace("X", "*")
                    .replaceAll("\\s+", "");
            for (String term : norm.split("\\+")) {
                if (term.contains("*")) {
                    String[] parts = term.split("\\*");
                    int  count = Integer.parseInt(parts[0]);
                    long value = Long.parseLong(parts[1]);
                    for (int i = 0; i < count; i++) result.add(value);
                } else {
                    result.add(Long.parseLong(term));
                }
            }
        } catch (Exception ignored) {}
        return result;
    }

    private String arabicToWestern(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray())
            sb.append(c >= '٠' && c <= '٩' ? (char)('0' + (c - '٠')) : c);
        return sb.toString();
    }

    private void showInvoiceList() {
        PrintInvoiceAdapter adapter = new PrintInvoiceAdapter(
                this, allSales, selectedSales, () -> {
            int n = selectedSales.size();
            btnPrint.setText(n == 0 ? "🖨️ طباعة الكل" : "🖨️ طباعة " + n + " فاتورة");
        });
        rvInvoices.setAdapter(adapter);
    }

    private void buildInstallmentChips() {
        cgInstallmentNumbers.removeAllViews();
        int max = 0;
        for (SaleRecord sr : allSales)
            if (sr.installmentValues.size() > max) max = sr.installmentValues.size();
        for (int i = 1; i <= max; i++) {
            Chip chip = new Chip(this);
            chip.setText("قسط " + i);
            chip.setCheckable(true);
            cgInstallmentNumbers.addView(chip);
        }
    }

    // ══════════════════════ تعديل الميثود لطلب الإذن الخارجي أولاً ══════════════════════
    private void startPrinting() {
        List<SaleRecord> toPrint = selectedSales.isEmpty()
                ? new ArrayList<>(allSales) : new ArrayList<>(selectedSales);

        if (toPrint.isEmpty()) { toast("لا توجد فواتير للطباعة"); return; }

        List<Integer>  indices  = getSelectedInstallmentIndices(toPrint);
        List<ReceiptData> receipts = buildReceipts(toPrint, indices);

        if (receipts.isEmpty()) { toast("لا توجد أقساط مطابقة للفلتر"); return; }

        new AlertDialog.Builder(this)
                .setTitle("تأكيد الطباعة")
                .setMessage("هل تريد الانتقال إلى معاينة " + receipts.size() + " وصل وقراءتها قبل الطباعة؟")
                .setPositiveButton("معاينة الأوصال", (d, w) -> {
                    try {
                        StringBuilder allHtml = new StringBuilder();
                        allHtml.append("<!DOCTYPE html><html dir='rtl'><head>")
                                .append("<meta charset='UTF-8'/>")
                                .append("<style>")
                                .append("@media print{.pb{page-break-after:always;}}")
                                .append("body{margin:0; padding:20px; font-family:sans-serif;}")
                                .append("table{width:100%; border-collapse:collapse;}")
                                .append("th, td{border:1px solid #000; padding:8px; text-align:center;}")
                                .append("</style></head><body>");

                        for (int i = 0; i < receipts.size(); i++) {
                            String html = ReceiptBuilder.buildHtml(receipts.get(i));
                            int start = html.indexOf("<body>") + 6;
                            int end   = html.indexOf("</body>");
                            if (start > 5 && end > 0)
                                allHtml.append(html, start, end);
                            else
                                allHtml.append(html);
                            if (i < receipts.size() - 1)
                                allHtml.append("<div class='pb'></div>");
                        }
                        allHtml.append("</body></html>");

                        File cacheFile = new File(getCacheDir(), "temp_print_receipts.html");
                        FileWriter writer = new FileWriter(cacheFile);
                        writer.write(allHtml.toString());
                        writer.close();

                        // 💡 نقوم بفحص وطلب الإذن هنا في الصفحة الخارجية قبل فتح شاشة المعاينة
                        checkUsbPermissionAndProceed(cacheFile.getAbsolutePath(), receipts.size());

                    } catch (Exception e) {
                        toast("❌ خطأ في كتابة ملف المعاينة: " + e.getMessage());
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void checkUsbPermissionAndProceed(String filePath, int count) {
        if (usbManager == null) {
            openPreviewActivity(filePath, count);
            return;
        }

        Map<String, UsbDevice> devices = usbManager.getDeviceList();
        if (devices == null || devices.isEmpty()) {
            toast("⚠️ لم يتم العثور على طابعة USB متصلة، سيتم فتح المعاينة فقط");
            openPreviewActivity(filePath, count);
            return;
        }

        UsbDevice targetDevice = null;
        for (UsbDevice dev : devices.values()) {
            for (int i = 0; i < dev.getInterfaceCount(); i++) {
                android.hardware.usb.UsbInterface intf = dev.getInterface(i);
                for (int e = 0; e < intf.getEndpointCount(); e++) {
                    android.hardware.usb.UsbEndpoint ep = intf.getEndpoint(e);
                    if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK &&
                            ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                        targetDevice = dev;
                        break;
                    }
                }
                if (targetDevice != null) break;
            }
            if (targetDevice != null) break;
        }

        if (targetDevice == null) targetDevice = devices.values().iterator().next();

        if (usbManager.hasPermission(targetDevice)) {
            openPreviewActivity(filePath, count);
        } else {
            // 💡 حقن الـ BroadcastReceiver ليعمل فوراً ويطلب الصلاحية ويقعد برة
            IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION_OUTER);
            Context appContext = getApplicationContext();

            BroadcastReceiver dynamicReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (ACTION_USB_PERMISSION_OUTER.equals(intent.getAction())) {
                        // الانتقال لصفحة المعاينة فور الانتهاء من حوار الصلاحية الخارجي
                        openPreviewActivity(filePath, count);
                    }
                    try { appContext.unregisterReceiver(this); } catch (Exception ignored) {}
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(dynamicReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                appContext.registerReceiver(dynamicReceiver, filter);
            }

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }

            Intent usbIntent = new Intent(ACTION_USB_PERMISSION_OUTER);
            usbIntent.setPackage(appContext.getPackageName());
            PendingIntent pi = PendingIntent.getBroadcast(appContext, 789, usbIntent, flags);

            usbManager.requestPermission(targetDevice, pi);
        }
    }

    private void openPreviewActivity(String filePath, int count) {
        Intent intent = new Intent(PrintActivity.this, PrintPreviewActivity.class);
        intent.putExtra("html_file_path", filePath);
        intent.putExtra("receipt_count", count);
        startActivity(intent);
    }

    private List<Integer> getSelectedInstallmentIndices(List<SaleRecord> sales) {
        List<Integer> indices = new ArrayList<>();
        int checkedId = rgInstallmentMode.getCheckedRadioButtonId();

        if (checkedId == R.id.rb_all_installments) return indices;

        if (checkedId == R.id.rb_by_number) {
            for (int i = 0; i < cgInstallmentNumbers.getChildCount(); i++) {
                Chip chip = (Chip) cgInstallmentNumbers.getChildAt(i);
                if (chip.isChecked()) indices.add(i);
            }
            return indices;
        }

        if (checkedId == R.id.rb_by_date) {
            int filterYear  = Integer.parseInt(yearList.get(spinnerInstallmentYear.getSelectedItemPosition()));
            int filterMonth = Integer.parseInt(monthList.get(spinnerInstallmentMonth.getSelectedItemPosition()));
            for (SaleRecord sr : sales) {
                Calendar start = Calendar.getInstance();
                start.setTimeInMillis(sr.timestamp);
                for (int i = 0; i < sr.installmentValues.size(); i++) {
                    Calendar due = (Calendar) start.clone();
                    due.add(Calendar.MONTH, i);
                    if (due.get(Calendar.YEAR) == filterYear
                            && (due.get(Calendar.MONTH) + 1) == filterMonth
                            && !indices.contains(i)) {
                        indices.add(i);
                    }
                }
            }
        }
        return indices;
    }

    private List<ReceiptData> buildReceipts(List<SaleRecord> sales, List<Integer> indices) {
        List<ReceiptData> list = new ArrayList<>();
        for (SaleRecord sr : sales) {
            if (sr.installmentValues.isEmpty()) continue;
            int total = sr.installmentValues.size();
            if (indices.isEmpty()) {
                for (int i = 0; i < total; i++) list.add(makeReceipt(sr, i, total));
            } else {
                for (int idx : indices)
                    if (idx < total) list.add(makeReceipt(sr, idx, total));
            }
        }
        return list;
    }

    private ReceiptData makeReceipt(SaleRecord sr, int index, int total) {
        ReceiptData r   = new ReceiptData();
        r.sale              = sr;
        r.company           = companyInfo;
        r.installmentIndex  = index;
        r.totalInstallments = total;
        r.installmentAmount = sr.installmentValues.get(index);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(sr.timestamp);
        cal.add(Calendar.MONTH, index);
        r.dueDay   = cal.get(Calendar.DAY_OF_MONTH);
        r.dueMonth = cal.get(Calendar.MONTH) + 1;
        r.dueYear  = cal.get(Calendar.YEAR);

        double paid = sr.downPayment;
        for (int i = 0; i <= index; i++) paid += sr.installmentValues.get(i);
        r.remainingAfter = Math.max(0, sr.totalAmount - paid);
        return r;
    }

    private void manualDetectPrinter() {
        if (usbManager == null) {
            toast("❌ الـ UsbManager غير مدعوم في هذا الموبايل");
            return;
        }
        try {
            Map<String, UsbDevice> devices = usbManager.getDeviceList();
            if (devices == null || devices.isEmpty()) {
                tvPrinterStatus.setText("❌ لا توجد طابعات USB متصلة");
                tvPrinterStatus.setTextColor(0xFFD50000);
                return;
            }
            tvPrinterStatus.setText("✅ تم العثور على جهاز USB");
            tvPrinterStatus.setTextColor(0x1b5e20);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getString(DataSnapshot snap, String key, String def) {
        Object v = snap.child(key).getValue();
        return v != null ? v.toString() : def;
    }

    private double getDouble(DataSnapshot snap, String key) {
        Object v = snap.child(key).getValue();
        if (v instanceof Long)   return ((Long) v).doubleValue();
        if (v instanceof Double) return (Double) v;
        return 0;
    }

    private long getLong(DataSnapshot snap, String key) {
        Object v = snap.child(key).getValue();
        if (v instanceof Long)   return (Long) v;
        if (v instanceof Double) return ((Double) v).longValue();
        return 0;
    }

    private ArrayAdapter<String> simpleAdapter(List<String> list) {
        return new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list) {{
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }};
    }

    private void setLoading(boolean on) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        btnLoadSales.setEnabled(!on);
        btnPrint.setEnabled(!on);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    static class CompanyInfo implements java.io.Serializable {
        String name = "", text = "", branch = "", phone = "";
    }

    static class SaleRecord implements java.io.Serializable {
        String firebaseKey, customerName, area, address, phone;
        String paymentType, installment, salesperson;
        long   id, timestamp;
        double totalAmount, downPayment, remaining;
        List<ProductLine> products          = new ArrayList<>();
        List<Long>        installmentValues = new ArrayList<>();
    }

    static class ProductLine implements java.io.Serializable {
        String name; double unitPrice, totalPrice; int quantity;
    }

    static class ReceiptData implements java.io.Serializable {
        SaleRecord  sale;
        CompanyInfo company;
        int         installmentIndex, totalInstallments;
        long        installmentAmount;
        int         dueDay, dueMonth, dueYear;
        double      remainingAfter;
    }
}