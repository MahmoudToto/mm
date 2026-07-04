package mhsal.mndop.com;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * شاشة المدير — تحصيل المحصلين
 *
 * مسار Firebase:
 *   /Collection/{collectorName}/{year}/{month}/target
 *   /Collection/{collectorName}/{year}/{month}/collected
 *   /Collection/{collectorName}/{year}/{month}/records/{pushId}/...
 *
 * حقل status في كل سجل: "انتظار" | "موافق" | "مرفوض"
 */
public class CollectionManagerActivity extends AppCompatActivity {

    // ══ Views ══
    private AutoCompleteTextView spinnerCollector;
    private TextInputLayout      tilCollector;
    private TextView             tvMonthTitle, tvTarget, tvCollected, tvRemaining,
            tvProgressPercent, tvEmpty;
    private ProgressBar          progressCollection, progressLoading;
    private RecyclerView         rvCollection;
    private SwipeRefreshLayout   swipeRefresh;
    private MaterialButton       btnYear, btnMonth;
    private ExtendedFloatingActionButton fabAdd, fabAddPlus, fabSubtract, fabToggle;

    // ══ Firebase ══
    private DatabaseReference userRef;
    private DatabaseReference monthRef;
    private ValueEventListener monthListener;

    // ══ Data ══
    private final List<String>           collectorNames = new ArrayList<>();
    private final List<CollectionRecord> records        = new ArrayList<>();
    private CollectionAdapter            adapter;

    private double target    = 0;
    private double collected = 0;

    // ══ الحالة ══
    private String  currentCollector = null;
    private int     currentYear;
    private int     currentMonth;
    private boolean fabsVisible = false;

    // ══ SharedPreferences ══
    private static final String PREFS     = "col_mgr_prefs";
    private static final String KEY_YEAR  = "year";
    private static final String KEY_MONTH = "month";

    private static final String TYPE_NORMAL   = "تحصيل";
    private static final String TYPE_ADD      = "زيادة";
    private static final String TYPE_SUBTRACT = "نقص";

    private static final String[] MONTH_NAMES = {
            "يناير","فبراير","مارس","أبريل","مايو","يونيو",
            "يوليو","أغسطس","سبتمبر","أكتوبر","نوفمبر","ديسمبر"
    };

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy  hh:mm a", new Locale("ar"));

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection_manager);

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Calendar cal = Calendar.getInstance();
        currentYear  = prefs.getInt(KEY_YEAR,  cal.get(Calendar.YEAR));
        currentMonth = prefs.getInt(KEY_MONTH, cal.get(Calendar.MONTH) + 1);

        initViews();
        setupDateButtons();
        setupRecyclerView();
        setupFabs();
        loadCollectors();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeMonthListener();
    }

    // ══════════════════════════════════════════
    //  Init Views
    // ══════════════════════════════════════════

    private void initViews() {
        spinnerCollector   = findViewById(R.id.spinner_collector);
        tilCollector       = findViewById(R.id.til_collector);
        tvMonthTitle       = findViewById(R.id.tv_month_title);
        tvTarget           = findViewById(R.id.tv_target);
        tvCollected        = findViewById(R.id.tv_collected);
        tvRemaining        = findViewById(R.id.tv_remaining);
        tvProgressPercent  = findViewById(R.id.tv_progress_percent);
        tvEmpty            = findViewById(R.id.tv_empty);
        progressCollection = findViewById(R.id.progress_collection);
        progressLoading    = findViewById(R.id.progress_loading);
        rvCollection       = findViewById(R.id.rv_collection);
        swipeRefresh       = findViewById(R.id.swipe_refresh_collection);
        btnYear            = findViewById(R.id.btn_year);
        btnMonth           = findViewById(R.id.btn_month);
        fabAdd             = findViewById(R.id.fab_add_collection);
        fabAddPlus         = findViewById(R.id.fab_add_plus);
        fabSubtract        = findViewById(R.id.fab_subtract);
        fabToggle          = findViewById(R.id.fab_toggle);

        updateDateButtons();
        tvMonthTitle.setText(MONTH_NAMES[currentMonth - 1] + " " + currentYear);

        swipeRefresh.setOnRefreshListener(() -> {
            if (currentCollector != null) {
                removeMonthListener();
                loadMonthData();
            } else {
                swipeRefresh.setRefreshing(false);
            }
        });

        userRef = FirebaseDatabase.getInstance().getReference("user");
    }

    // ══════════════════════════════════════════
    //  تحميل المحصلين من /user
    // ══════════════════════════════════════════

    private void loadCollectors() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                collectorNames.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    if (name != null) collectorNames.add(name);
                }

                ArrayAdapter<String> adpt = new ArrayAdapter<>(
                        CollectionManagerActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        collectorNames);
                spinnerCollector.setAdapter(adpt);

                if (collectorNames.size() == 1) {
                    currentCollector = collectorNames.get(0);
                    spinnerCollector.setText(currentCollector, false);
                    loadMonthData();
                }

                spinnerCollector.setOnItemClickListener((parent, view, position, id) -> {
                    tilCollector.setError(null);
                    currentCollector = collectorNames.get(position);
                    removeMonthListener();
                    loadMonthData();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(CollectionManagerActivity.this,
                        "خطأ في جلب المحصلين: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // ══════════════════════════════════════════
    //  RecyclerView — وضع المدير
    // ══════════════════════════════════════════

    private void setupRecyclerView() {
        adapter = new CollectionAdapter(
                this,
                records,
                record -> showRecordDetails(record),
                new CollectionAdapter.OnManagerAction() {
                    @Override public void onApprove(CollectionRecord r) { confirmApprove(r); }
                    @Override public void onReject(CollectionRecord r)  { confirmReject(r);  }
                    @Override public void onEdit(CollectionRecord r)    { showEditDialog(r); }
                    @Override public void onDelete(CollectionRecord r)  { confirmDelete(r);  }
                }
        );
        rvCollection.setLayoutManager(new LinearLayoutManager(this));
        rvCollection.setNestedScrollingEnabled(false);
        rvCollection.setHasFixedSize(false);
        rvCollection.setAdapter(adapter);
    }

    // ══════════════════════════════════════════
    //  أزرار التاريخ
    // ══════════════════════════════════════════

    private void setupDateButtons() {
        btnYear.setOnClickListener(v -> {
            int thisYear = Calendar.getInstance().get(Calendar.YEAR);
            List<String> years = new ArrayList<>();
            for (int y = 2023; y <= thisYear + 1; y++) years.add(String.valueOf(y));
            String[] arr = years.toArray(new String[0]);
            int sel = years.indexOf(String.valueOf(currentYear));
            new AlertDialog.Builder(this)
                    .setTitle("اختر السنة")
                    .setSingleChoiceItems(arr, sel, (dialog, which) -> {
                        currentYear = Integer.parseInt(arr[which]);
                        saveAndReload(); dialog.dismiss();
                    })
                    .setNegativeButton("إلغاء", null).show();
        });

        btnMonth.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("اختر الشهر")
                        .setSingleChoiceItems(MONTH_NAMES, currentMonth - 1, (dialog, which) -> {
                            currentMonth = which + 1;
                            saveAndReload(); dialog.dismiss();
                        })
                        .setNegativeButton("إلغاء", null).show());
    }

    private void saveAndReload() {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt(KEY_YEAR, currentYear).putInt(KEY_MONTH, currentMonth).apply();
        updateDateButtons();
        tvMonthTitle.setText(MONTH_NAMES[currentMonth - 1] + " " + currentYear);
        if (currentCollector != null) { removeMonthListener(); loadMonthData(); }
    }

    private void updateDateButtons() {
        if (btnYear  != null) btnYear.setText(String.valueOf(currentYear));
        if (btnMonth != null) btnMonth.setText(MONTH_NAMES[currentMonth - 1]);
    }

    // ══════════════════════════════════════════
    //  FABs
    // ══════════════════════════════════════════

    private void setupFabs() {
        fabToggle.setOnClickListener(v -> {
            fabsVisible = !fabsVisible;
            int vis = fabsVisible ? View.VISIBLE : View.GONE;
            fabAdd.setVisibility(vis);
            fabAddPlus.setVisibility(vis);
            fabSubtract.setVisibility(vis);
            fabToggle.setText(fabsVisible ? ">" : "<");
        });
        fabAdd.setOnClickListener(v      -> { if (checkCollector()) showAddDialog(TYPE_NORMAL);   });
        fabAddPlus.setOnClickListener(v  -> { if (checkCollector()) showAddDialog(TYPE_ADD);      });
        fabSubtract.setOnClickListener(v -> { if (checkCollector()) showAddDialog(TYPE_SUBTRACT); });
    }

    private boolean checkCollector() {
        if (currentCollector == null || currentCollector.isEmpty()) {
            tilCollector.setError("اختر المحصل أولاً");
            spinnerCollector.requestFocus();
            return false;
        }
        return true;
    }

    // ══════════════════════════════════════════
    //  Firebase — تحميل بيانات الشهر
    // ══════════════════════════════════════════

    private DatabaseReference getMonthDbRef() {
        return FirebaseDatabase.getInstance().getReference("Collection")
                .child(currentCollector)
                .child(String.valueOf(currentYear))
                .child(String.valueOf(currentMonth));
    }

    private void loadMonthData() {
        if (currentCollector == null) return;
        progressLoading.setVisibility(View.VISIBLE);

        monthRef = getMonthDbRef();
        monthListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                target    = readDouble(snapshot, "target");
                collected = readDouble(snapshot, "collected");

                records.clear();
                for (DataSnapshot child : snapshot.child("records").getChildren()) {
                    try {
                        CollectionRecord r = new CollectionRecord();
                        r.setId(child.getKey());

                        Object amt = child.child("amount").getValue();
                        if (amt instanceof Long)        r.setAmount(((Long) amt).doubleValue());
                        else if (amt instanceof Double) r.setAmount((Double) amt);

                        Object ts = child.child("timestamp").getValue();
                        if (ts instanceof Long) r.setTimestamp((Long) ts);

                        Object recv = child.child("receiverName").getValue();
                        if (recv != null) r.setReceiverName(String.valueOf(recv));

                        Object phone = child.child("clientPhone").getValue();
                        if (phone != null) r.setClientPhone(String.valueOf(phone));

                        Object note = child.child("note").getValue();
                        if (note != null) r.setNote(String.valueOf(note));

                        Object type = child.child("type").getValue();
                        r.setType(type != null ? String.valueOf(type) : TYPE_NORMAL);

                        // ══ قراءة حالة الموافقة ══
                        Object status = child.child("status").getValue();
                        r.setStatus(status != null
                                ? String.valueOf(status)
                                : CollectionRecord.STATUS_PENDING);

                        records.add(r);
                    } catch (Exception ignored) {}
                }

                // ترتيب: الانتظار أولاً ← ثم الأحدث
                records.sort((a, b) -> {
                    int sa = statusOrder(a.getStatus());
                    int sb = statusOrder(b.getStatus());
                    if (sa != sb) return sa - sb;
                    return Long.compare(b.getTimestamp(), a.getTimestamp());
                });

                updateSummaryUI();
                adapter.notifyDataSetChanged();
                tvEmpty.setVisibility(records.isEmpty() ? View.VISIBLE : View.GONE);
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(CollectionManagerActivity.this,
                        "خطأ: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        monthRef.addValueEventListener(monthListener);
    }

    /** ترتيب: انتظار=0، مرفوض=1، موافق=2 */
    private int statusOrder(String status) {
        switch (status) {
            case CollectionRecord.STATUS_PENDING:  return 0;
            case CollectionRecord.STATUS_REJECTED: return 1;
            default:                               return 2;
        }
    }

    private void removeMonthListener() {
        if (monthListener != null && monthRef != null) {
            monthRef.removeEventListener(monthListener);
            monthListener = null;
        }
    }

    private double readDouble(DataSnapshot snap, String key) {
        Object v = snap.child(key).getValue();
        if (v instanceof Long)   return ((Long) v).doubleValue();
        if (v instanceof Double) return (Double) v;
        return 0;
    }

    // ══════════════════════════════════════════
    //  بطاقة الملخص
    // ══════════════════════════════════════════

    private void updateSummaryUI() {
        double remaining = target - collected;
        int percent = target > 0 ? (int) Math.min((collected / target) * 100, 100) : 0;
        tvTarget.setText(String.format("%.0f ج", target));
        tvCollected.setText(String.format("%.0f ج", collected));
        tvRemaining.setText(String.format("%.0f ج", Math.max(remaining, 0)));
        progressCollection.setProgress(Math.max(percent, 0));
        tvProgressPercent.setText(percent + "%");
    }

    // ══════════════════════════════════════════
    //  ① الموافقة على سجل
    // ══════════════════════════════════════════

    private void confirmApprove(CollectionRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("تأكيد الموافقة")
                .setMessage("هل توافق على هذا السجل؟\nالمبلغ: "
                        + String.format("%.0f", record.getAmount()) + " جنيه\nالعميل: "
                        + record.getReceiverName())
                .setPositiveButton("موافقة ✔", (d, w) ->
                        setRecordStatus(record, CollectionRecord.STATUS_APPROVED))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    // ══════════════════════════════════════════
    //  ② رفض سجل
    // ══════════════════════════════════════════

    private void confirmReject(CollectionRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("تأكيد الرفض")
                .setMessage("هل تريد رفض هذا السجل؟\nالمبلغ: "
                        + String.format("%.0f", record.getAmount()) + " جنيه\nالعميل: "
                        + record.getReceiverName())
                .setPositiveButton("رفض ✖", (d, w) ->
                        setRecordStatus(record, CollectionRecord.STATUS_REJECTED))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void setRecordStatus(CollectionRecord record, String newStatus) {
        if (record.getId() == null) return;

        getMonthDbRef().child("records").child(record.getId())
                .child("status").setValue(newStatus)
                .addOnSuccessListener(unused -> {
                    if (CollectionRecord.STATUS_APPROVED.equals(newStatus)) {
                        updateTotalsOnApprove(record);
                    }
                    String msg = CollectionRecord.STATUS_APPROVED.equals(newStatus)
                            ? "تمت الموافقة ✔" : "تم الرفض ✖";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "فشل التحديث", Toast.LENGTH_SHORT).show());
    }

    /** عند الموافقة → حدّث target أو collected حسب نوع السجل */
    private void updateTotalsOnApprove(CollectionRecord record) {
        DatabaseReference monthDbRef = getMonthDbRef();
        switch (record.getType()) {
            case TYPE_ADD:
                monthDbRef.child("target").setValue(target + record.getAmount());
                break;
            case TYPE_SUBTRACT:
                monthDbRef.child("target").setValue(Math.max(target - record.getAmount(), 0));
                break;
            default: // تحصيل
                monthDbRef.child("collected").setValue(collected + record.getAmount());
                break;
        }
    }

    // ══════════════════════════════════════════
    //  ③ تعديل سجل
    // ══════════════════════════════════════════

    private void showEditDialog(CollectionRecord record) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_collection, null);

        EditText etAmount   = dialogView.findViewById(R.id.et_amount);
        EditText etReceiver = dialogView.findViewById(R.id.et_receiver);
        EditText etPhone    = dialogView.findViewById(R.id.et_client_phone);
        EditText etNote     = dialogView.findViewById(R.id.et_note);

        etAmount.setText(String.format("%.0f", record.getAmount()));
        etReceiver.setText(record.getReceiverName() != null ? record.getReceiverName() : "");
        etPhone.setText(record.getClientPhone() != null ? record.getClientPhone() : "");
        etNote.setText(record.getNote() != null ? record.getNote() : "");

        new AlertDialog.Builder(this)
                .setTitle("✏️ تعديل السجل")
                .setView(dialogView)
                .setPositiveButton("حفظ التعديل", (dialog, which) -> {
                    String amtStr = etAmount.getText().toString().trim();
                    String recv   = etReceiver.getText().toString().trim();
                    String phone  = etPhone.getText().toString().trim();
                    String note   = etNote.getText().toString().trim();

                    if (amtStr.isEmpty()) {
                        Toast.makeText(this, "أدخل المبلغ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (recv.isEmpty()) {
                        Toast.makeText(this, "أدخل اسم العميل", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double newAmount;
                    try { newAmount = Double.parseDouble(amtStr); }
                    catch (Exception e) {
                        Toast.makeText(this, "مبلغ غير صحيح", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    applyEdit(record, newAmount, recv, phone, note);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void applyEdit(CollectionRecord record, double newAmount,
                           String recv, String phone, String note) {
        if (record.getId() == null) return;

        DatabaseReference recRef = getMonthDbRef()
                .child("records").child(record.getId());

        recRef.child("amount").setValue(newAmount);
        recRef.child("receiverName").setValue(recv);
        recRef.child("clientPhone").setValue(phone);
        recRef.child("note").setValue(note);
        // status يبقى كما هو

        // إذا كان موافقاً → حدّث الإجماليات بالفرق
        if (record.isApproved()) {
            double diff = newAmount - record.getAmount();
            DatabaseReference monthDbRef = getMonthDbRef();
            switch (record.getType()) {
                case TYPE_ADD:
                    monthDbRef.child("target").setValue(target + diff);
                    break;
                case TYPE_SUBTRACT:
                    monthDbRef.child("target").setValue(Math.max(target - diff, 0));
                    break;
                default:
                    monthDbRef.child("collected").setValue(Math.max(collected + diff, 0));
                    break;
            }
        }

        Toast.makeText(this, "تم التعديل ✔", Toast.LENGTH_SHORT).show();
    }

    // ══════════════════════════════════════════
    //  ④ حذف سجل
    // ══════════════════════════════════════════

    private void confirmDelete(CollectionRecord record) {
        new AlertDialog.Builder(this)
                .setTitle("حذف السجل")
                .setMessage("هل تريد حذف هذا السجل نهائياً؟")
                .setPositiveButton("حذف", (d, w) -> deleteRecord(record))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void deleteRecord(CollectionRecord record) {
        if (record.getId() == null || currentCollector == null) return;

        DatabaseReference monthDbRef = getMonthDbRef();
        monthDbRef.child("records").child(record.getId()).removeValue()
                .addOnSuccessListener(unused -> {
                    // إذا كان موافقاً → اطرح قيمته من الإجماليات
                    if (record.isApproved()) {
                        switch (record.getType()) {
                            case TYPE_ADD:
                                monthDbRef.child("target")
                                        .setValue(Math.max(target - record.getAmount(), 0));
                                break;
                            case TYPE_SUBTRACT:
                                monthDbRef.child("target")
                                        .setValue(target + record.getAmount());
                                break;
                            default:
                                monthDbRef.child("collected")
                                        .setValue(Math.max(collected - record.getAmount(), 0));
                                break;
                        }
                    }
                    Toast.makeText(this, "تم الحذف", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "فشل الحذف", Toast.LENGTH_SHORT).show());
    }

    // ══════════════════════════════════════════
    //  Dialog إضافة سجل جديد (من المدير → موافق مباشرة)
    // ══════════════════════════════════════════

    private void showAddDialog(String type) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_collection, null);

        EditText etAmount   = dialogView.findViewById(R.id.et_amount);
        EditText etReceiver = dialogView.findViewById(R.id.et_receiver);
        EditText etPhone    = dialogView.findViewById(R.id.et_client_phone);
        EditText etNote     = dialogView.findViewById(R.id.et_note);

        String title;
        switch (type) {
            case TYPE_ADD:      title = "▲ زيادة في الهدف";  break;
            case TYPE_SUBTRACT: title = "▼ نقص من الهدف";    break;
            default:            title = "✓ إضافة تحصيل";     break;
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String amtStr = etAmount.getText().toString().trim();
                    String recv   = etReceiver.getText().toString().trim();
                    String phone  = etPhone.getText().toString().trim();
                    String note   = etNote.getText().toString().trim();

                    if (amtStr.isEmpty()) { Toast.makeText(this, "أدخل المبلغ", Toast.LENGTH_SHORT).show(); return; }
                    if (recv.isEmpty())   { Toast.makeText(this, "أدخل اسم العميل", Toast.LENGTH_SHORT).show(); return; }
                    double amount;
                    try { amount = Double.parseDouble(amtStr); }
                    catch (Exception e) { Toast.makeText(this, "مبلغ غير صحيح", Toast.LENGTH_SHORT).show(); return; }
                    saveRecord(amount, recv, phone, note, type);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void saveRecord(double amount, String receiverName,
                            String clientPhone, String note, String type) {

        DatabaseReference monthDbRef = getMonthDbRef();

        double newTarget = target, newCollected = collected;
        switch (type) {
            case TYPE_ADD:      newTarget    += amount; break;
            case TYPE_SUBTRACT: newTarget     = Math.max(newTarget - amount, 0); break;
            default:            newCollected += amount; break;
        }

        monthDbRef.child("target").setValue(newTarget);
        monthDbRef.child("collected").setValue(newCollected);

        String pushId = monthDbRef.child("records").push().getKey();
        if (pushId == null) return;

        CollectionRecord record = new CollectionRecord(
                amount, receiverName, clientPhone,
                note, System.currentTimeMillis(), type);
        record.setStatus(CollectionRecord.STATUS_APPROVED); // المدير يضيف مباشرة موافق

        String successMsg;
        switch (type) {
            case TYPE_ADD:      successMsg = "تم حفظ الزيادة ✓";  break;
            case TYPE_SUBTRACT: successMsg = "تم حفظ النقص ✓";    break;
            default:            successMsg = "تم حفظ التحصيل ✓";  break;
        }

        monthDbRef.child("records").child(pushId).setValue(record)
                .addOnSuccessListener(u ->
                        Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "فشل الحفظ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ══════════════════════════════════════════
    //  تفاصيل السجل
    // ══════════════════════════════════════════

    private void showRecordDetails(CollectionRecord record) {
        String typeLabel;
        if (TYPE_ADD.equals(record.getType()))           typeLabel = "▲ زيادة في الهدف";
        else if (TYPE_SUBTRACT.equals(record.getType())) typeLabel = "▼ نقص من الهدف";
        else                                              typeLabel = "✓ تحصيل";

        String statusLabel;
        switch (record.getStatus()) {
            case CollectionRecord.STATUS_APPROVED: statusLabel = "✔ موافق"; break;
            case CollectionRecord.STATUS_REJECTED: statusLabel = "✖ مرفوض"; break;
            default:                               statusLabel = "⏳ انتظار"; break;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 النوع: ").append(typeLabel).append("\n\n");
        sb.append("📌 الحالة: ").append(statusLabel).append("\n\n");
        sb.append("💰 المبلغ: ").append(String.format("%.0f", record.getAmount())).append(" جنيه\n\n");
        sb.append("👤 العميل: ").append(record.getReceiverName() != null ? record.getReceiverName() : "—").append("\n\n");

        String phone = record.getClientPhone();
        if (phone != null && !phone.isEmpty())
            sb.append("📞 رقم العميل: ").append(phone).append("\n\n");

        sb.append("📅 التاريخ: ").append(record.getTimestamp() > 0
                ? DATE_FORMAT.format(new Date(record.getTimestamp())) : "—");

        String note = record.getNote();
        if (note != null && !note.isEmpty())
            sb.append("\n\n📝 الملاحظة:\n").append(note);

        new AlertDialog.Builder(this)
                .setTitle("تفاصيل السجل")
                .setMessage(sb.toString())
                .setPositiveButton("إغلاق", null)
                .show();
    }
}