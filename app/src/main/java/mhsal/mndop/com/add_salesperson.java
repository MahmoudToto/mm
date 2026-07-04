package mhsal.mndop.com;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class add_salesperson extends AppCompatActivity {

    private TextInputEditText etName, etCode, etWarehouse, etPhone, etSearch;
    private MaterialButton btnAdd;
    private RecyclerView rvSalespersons;
    private ProgressBar progressBar;
    private ImageView btnRefresh;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabScrollTop;

    private DatabaseReference salespersonsRef;
    private ValueEventListener listener;

    private List<Salesperson> salespersonList = new ArrayList<>();
    private List<Salesperson> backupList = new ArrayList<>();
    private SalespersonAdapter adapter;

    private String editingId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_salesperson);

        initViews();
        setupRecyclerView();
        setupFirebase();
        setupListeners();
        loadSalespersonsRealtime();
    }

    private void initViews() {
        etName = findViewById(R.id.et_salesperson_name);
        etCode = findViewById(R.id.et_salesperson_code);
        etWarehouse = findViewById(R.id.et_warehouse);
        etPhone = findViewById(R.id.et_phone);           // الجديد
        etSearch = findViewById(R.id.et_search);

        btnAdd = findViewById(R.id.btn_add_salesperson);
        btnRefresh = findViewById(R.id.btn_refresh);
        swipeRefresh = findViewById(R.id.swipe);
        fabScrollTop = findViewById(R.id.fab_scroll_top);
        progressBar = findViewById(R.id.progress);
        rvSalespersons = findViewById(R.id.rv_salespersons);
    }

    private void setupRecyclerView() {
        adapter = new SalespersonAdapter(this, salespersonList, new SalespersonAdapter.Listener() {
            @Override
            public void onEdit(Salesperson s) { editMode(s); }
            @Override
            public void onDelete(Salesperson s) { confirmDelete(s); }
        });

        rvSalespersons.setLayoutManager(new LinearLayoutManager(this));
        rvSalespersons.setAdapter(adapter);
    }

    private void setupFirebase() {
        salespersonsRef = FirebaseDatabase.getInstance().getReference("salespersons");
    }

    private void setupListeners() {
        btnAdd.setOnClickListener(v -> saveSalesperson());
        btnRefresh.setOnClickListener(v -> reloadData());
        swipeRefresh.setOnRefreshListener(this::reloadData);

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterSalespersons(s.toString().trim());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        rvSalespersons.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 30) fabScrollTop.show();
                else if (!recyclerView.canScrollVertically(-1)) fabScrollTop.hide();
            }
        });

        fabScrollTop.setOnClickListener(v -> rvSalespersons.smoothScrollToPosition(0));
    }

    private void loadSalespersonsRealtime() {
        progressBar.setVisibility(View.VISIBLE);

        if (listener != null) salespersonsRef.removeEventListener(listener);

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                salespersonList.clear();
                backupList.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Salesperson s = child.getValue(Salesperson.class);
                    if (s != null) {
                        s.setId(child.getKey());
                        salespersonList.add(s);
                    }
                }

                Collections.sort(salespersonList, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                backupList.addAll(salespersonList);

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(add_salesperson.this, "خطأ في التحميل", Toast.LENGTH_SHORT).show();
            }
        };

        salespersonsRef.addValueEventListener(listener);
    }

    private void reloadData() {
        swipeRefresh.setRefreshing(true);
        loadSalespersonsRealtime();
    }

    private void saveSalesperson() {
        String name = etName.getText().toString().trim();
        String code = etCode.getText().toString().trim();
        String warehouse = etWarehouse.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("الاسم مطلوب");
            return;
        }
        if (TextUtils.isEmpty(code)) {
            etCode.setError("الكود مطلوب");
            return;
        }

        btnAdd.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Salesperson salesperson = new Salesperson(editingId, name, code, warehouse, phone);

        if (editingId == null) {
            String key = salespersonsRef.push().getKey();
            salesperson.setId(key);
            salespersonsRef.child(key).setValue(salesperson)
                    .addOnCompleteListener(task -> handleResult(task.isSuccessful(), "تم الإضافة بنجاح"));
        } else {
            salespersonsRef.child(editingId).setValue(salesperson)
                    .addOnCompleteListener(task -> handleResult(task.isSuccessful(), "تم التعديل بنجاح"));
        }
    }

    private void handleResult(boolean success, String message) {
        btnAdd.setEnabled(true);
        progressBar.setVisibility(View.GONE);

        if (success) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            clearFields();
            editingId = null;
            btnAdd.setText("إضافة المندوب");
        } else {
            Toast.makeText(this, "فشل العملية", Toast.LENGTH_LONG).show();
        }
    }

    private void editMode(Salesperson s) {
        editingId = s.getId();
        etName.setText(s.getName());
        etCode.setText(s.getCode());
        etWarehouse.setText(s.getWarehouse());
        etPhone.setText(s.getPhone());

        btnAdd.setText("تعديل المندوب");
    }

    private void confirmDelete(Salesperson s) {
        new AlertDialog.Builder(this)
                .setTitle("حذف المندوب")
                .setMessage("حذف " + s.getName() + "؟")
                .setPositiveButton("حذف", (d, w) -> deleteSalesperson(s))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void deleteSalesperson(Salesperson s) {
        salespersonsRef.child(s.getId()).removeValue();
    }

    private void filterSalespersons(String query) {
        if (query.isEmpty()) {
            salespersonList.clear();
            salespersonList.addAll(backupList);
        } else {
            String q = query.toLowerCase();
            List<Salesperson> filtered = new ArrayList<>();
            for (Salesperson s : backupList) {
                if ((s.getName() != null && s.getName().toLowerCase().contains(q)) ||
                        (s.getCode() != null && s.getCode().toLowerCase().contains(q)) ||
                        (s.getPhone() != null && s.getPhone().contains(q))) {
                    filtered.add(s);
                }
            }
            salespersonList.clear();
            salespersonList.addAll(filtered);
        }
        adapter.notifyDataSetChanged();
    }

    private void clearFields() {
        etName.setText("");
        etCode.setText("");
        etWarehouse.setText("");
        etPhone.setText("");
        etName.requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) salespersonsRef.removeEventListener(listener);
    }
}