package mhsal.mndop.com;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class card_products extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────
    private TextInputEditText etName, etPrice, etQuantity,
            etPrice2, etPrice2s, etCommission, etSearch;
    private AutoCompleteTextView spinnerDelegate;
    private TextInputLayout tilDelegate;
    private MaterialButton btnAdd, btnCancelEdit;
    private RecyclerView rvProducts;
    private ProgressBar progressBar;
    private ImageView btnRefresh,cd;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private FloatingActionButton fabScrollTop;

    // ─── Firebase ─────────────────────────────────────────────────────────────
    private DatabaseReference productsRef;
    private DatabaseReference userRef;
    private ValueEventListener productsListener;
    private DatabaseReference currentDelegateRef; // مرجع المندوب الحالي المحدد
    private LinearLayout card_content;

    // ─── بيانات المناديب ──────────────────────────────────────────────────────
    private final List<String> delegateNames = new ArrayList<>();
    private final List<String> delegateKeys  = new ArrayList<>();
    private ArrayAdapter<String> delegateAdapter;

    // ─── بيانات المنتجات ──────────────────────────────────────────────────────
    private final List<Product> productList       = new ArrayList<>();
    private final List<Product> productListBackup = new ArrayList<>();
    private ProductAdapter adapter;

    /** مفتاح المنتج قبل التعديل */
    private String editingKey          = null;
    /** اسم المندوب المحدد وقت التعديل */
    private String editingDelegateName = null;

    // ─── اسم المندوب المحدد حالياً ────────────────────────────────────────────
    private String currentSelectedDelegate = null;

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_products);

        initViews();
        setupRecyclerView();
        setupFirebase();
        setupListeners();
        loadDelegates();
        cd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (card_content.getVisibility()!=view.GONE)
                {
                    card_content.setVisibility(View.GONE);
                    cd.setRotation(180);
                }else {
                    card_content.setVisibility(View.VISIBLE);
                    cd.setRotation(0);
                }

            }
        });
    }

    // ─── تهيئة الـ Views ──────────────────────────────────────────────────────
    private void initViews() {
        card_content= findViewById(R.id.card_content);
        etName         = findViewById(R.id.et_product_name);
        etPrice        = findViewById(R.id.et_price);
        etQuantity     = findViewById(R.id.et_quantity);
        etPrice2       = findViewById(R.id.et_price2);
        etPrice2s      = findViewById(R.id.et_price2s);
        etCommission   = findViewById(R.id.et_commission);
        etSearch       = findViewById(R.id.et_search);
        spinnerDelegate = findViewById(R.id.spinner_delegate);
        tilDelegate    = findViewById(R.id.til_delegate);

        btnAdd        = findViewById(R.id.btn_add_product);
        btnCancelEdit = findViewById(R.id.btn_cancel_edit);
        btnRefresh    = findViewById(R.id.btn_refresh);
        swipeRefresh  = findViewById(R.id.swipe);
        fabScrollTop  = findViewById(R.id.fab_scroll_top);
        progressBar   = findViewById(R.id.progress);
        rvProducts    = findViewById(R.id.rv_products);
        cd = findViewById(R.id.cd);
    }

    // ─── إعداد RecyclerView ───────────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new ProductAdapter(this, productList, new ProductAdapter.Listener() {

            @Override
            public void onEdit(Product product) {
                enterEditMode(product);
            }

            @Override
            public void onDelete(Product product) {
                confirmDelete(product);
            }

            @Override
            public void onItemClick(Product product) {
                Toast.makeText(card_products.this,
                        "المنتج: " + product.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        rvProducts.setLayoutManager(new LinearLayoutManager(this));
        rvProducts.setAdapter(adapter);
    }

    // ─── إعداد Firebase ───────────────────────────────────────────────────────
    private void setupFirebase() {
        productsRef = FirebaseDatabase.getInstance().getReference("products");
        userRef     = FirebaseDatabase.getInstance().getReference("user");
    }

    // ─── تحميل أسماء المناديب من /user/{userId}/name ─────────────────────────
    private void loadDelegates() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                delegateNames.clear();
                delegateKeys.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String name = child.child("name").getValue(String.class);
                    if (name != null) {
                        delegateKeys.add(child.getKey());
                        delegateNames.add(name);
                    }
                }

                delegateAdapter = new ArrayAdapter<>(
                        card_products.this,
                        android.R.layout.simple_dropdown_item_1line,
                        delegateNames
                );
                spinnerDelegate.setAdapter(delegateAdapter);

                // إذا كان هناك مندوب واحد فقط → اختره تلقائياً وحمّل منتجاته
                if (delegateNames.size() == 1) {
                    spinnerDelegate.setText(delegateNames.get(0), false);
                    currentSelectedDelegate = delegateNames.get(0);
                    loadProductsByDelegate(currentSelectedDelegate);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(card_products.this,
                        "خطأ في تحميل المناديب: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // ─── إعداد المستمعين ──────────────────────────────────────────────────────
    private void setupListeners() {
        btnAdd.setOnClickListener(v -> saveProduct());
        btnCancelEdit.setOnClickListener(v -> cancelEdit());
        btnRefresh.setOnClickListener(v -> reloadProducts());
        swipeRefresh.setOnRefreshListener(this::reloadProducts);

        // ★ عند اختيار مندوب من الـ dropdown → حمّل منتجاته فقط
        spinnerDelegate.setOnItemClickListener((parent, view, position, id) -> {
            String selected = delegateNames.get(position);
            currentSelectedDelegate = selected;
            etSearch.setText(""); // مسح البحث عند تغيير المندوب
            loadProductsByDelegate(selected);
        });

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                filterProducts(s.toString().trim());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 30) fabScrollTop.show();
                else if (!rv.canScrollVertically(-1)) fabScrollTop.hide();
            }
        });

        fabScrollTop.setOnClickListener(v -> rvProducts.smoothScrollToPosition(0));
    }

    // ─── تحميل منتجات مندوب محدد فقط من /products/{delegateName} ─────────────
    private void loadProductsByDelegate(String delegateName) {
        if (delegateName == null || delegateName.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);

        // إزالة المستمع القديم إن وجد
        if (productsListener != null && currentDelegateRef != null) {
            currentDelegateRef.removeEventListener(productsListener);
        }

        currentDelegateRef = productsRef.child(delegateName);

        productsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                productList.clear();
                productListBackup.clear();

                for (DataSnapshot productSnap : snapshot.getChildren()) {
                    Product p = productSnap.getValue(Product.class);
                    if (p != null) {
                        p.setId(productSnap.getKey());
                        p.setDelegateName(delegateName);
                        productList.add(p);
                    }
                }

                Collections.sort(productList,
                        (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                productListBackup.addAll(productList);

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(card_products.this,
                        "خطأ في تحميل البيانات: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };

        currentDelegateRef.addValueEventListener(productsListener);
    }

    private void reloadProducts() {
        if (currentSelectedDelegate != null) {
            swipeRefresh.setRefreshing(true);
            loadProductsByDelegate(currentSelectedDelegate);
        } else {
            swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "اختر مندوباً أولاً", Toast.LENGTH_SHORT).show();
        }
    }

    // ─── حفظ المنتج (إضافة أو تعديل) ────────────────────────────────────────
    private void saveProduct() {

        // ① التحقق من اختيار المندوب
        String selectedDelegate = spinnerDelegate.getText().toString().trim();
        if (TextUtils.isEmpty(selectedDelegate)) {
            tilDelegate.setError("اختر المندوب أولاً");
            spinnerDelegate.requestFocus();
            return;
        } else {
            tilDelegate.setError(null);
        }

        // ② التحقق من اسم المنتج
        String name = getText(etName);
        if (TextUtils.isEmpty(name)) {
            etName.setError("اسم المنتج مطلوب");
            etName.requestFocus();
            return;
        }

        // ③ في وضع الإضافة: التأكد أن المنتج غير موجود مسبقاً لنفس المندوب
        String key = nameToKey(name);
        if (editingKey == null) {
            boolean exists = false;
            for (Product p : productListBackup) {
                if (selectedDelegate.equals(p.getDelegateName())
                        && nameToKey(p.getName()).equals(key)) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                etName.setError("منتج بهذا الاسم موجود مسبقاً لهذا المندوب");
                etName.requestFocus();
                return;
            }
        }

        double price      = parseDouble(getText(etPrice));
        double price2     = parseDouble(getText(etPrice2));
        double price2s    = parseDouble(getText(etPrice2s));
        int    quantity   = parseInteger(getText(etQuantity));
        double commission = parseDouble(getText(etCommission));

        btnAdd.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        Product product = new Product(key, name, price, quantity, price2, price2s, commission);

        DatabaseReference targetRef = productsRef.child(selectedDelegate);

        if (editingKey == null) {
            // ════ إضافة جديدة ════
            targetRef.child(key).setValue(product)
                    .addOnCompleteListener(task -> {
                        handleSaveResult(task.isSuccessful(), "تم إضافة المنتج بنجاح");
                        // بعد الإضافة: إذا تغيّر المندوب المحدد في الـ spinner نحدّث العرض
                        if (task.isSuccessful()
                                && !selectedDelegate.equals(currentSelectedDelegate)) {
                            currentSelectedDelegate = selectedDelegate;
                            loadProductsByDelegate(currentSelectedDelegate);
                        }
                    });

        } else {
            // ════ تعديل ════
            String oldKey          = editingKey;
            String oldDelegateName = editingDelegateName;
            String newKey          = key;

            boolean sameDelegate = selectedDelegate.equals(oldDelegateName);
            boolean sameName     = oldKey.equals(newKey);

            if (sameDelegate && sameName) {
                // لا يوجد تغيير في الاسم أو المندوب → تحديث مباشر
                targetRef.child(newKey).setValue(product)
                        .addOnCompleteListener(task ->
                                handleSaveResult(task.isSuccessful(), "تم تعديل المنتج بنجاح"));

            } else {
                // تغيّر الاسم أو المندوب → نكتب الجديد ثم نحذف القديم
                DatabaseReference oldRef = productsRef.child(oldDelegateName).child(oldKey);
                targetRef.child(newKey).setValue(product)
                        .addOnCompleteListener(addTask -> {
                            if (addTask.isSuccessful()) {
                                oldRef.removeValue()
                                        .addOnCompleteListener(delTask -> {
                                            handleSaveResult(delTask.isSuccessful(),
                                                    "تم تعديل المنتج بنجاح");
                                            // إذا تغيّر المندوب نعيد تحميل القائمة
                                            if (delTask.isSuccessful() && !sameDelegate) {
                                                currentSelectedDelegate = selectedDelegate;
                                                loadProductsByDelegate(currentSelectedDelegate);
                                            }
                                        });
                            } else {
                                handleSaveResult(false, "");
                            }
                        });
            }
        }
    }

    // ─── الدخول لوضع التعديل ──────────────────────────────────────────────────
    private void enterEditMode(Product product) {
        editingKey          = nameToKey(product.getName());
        editingDelegateName = product.getDelegateName();

        etName.setText(product.getName());
        etPrice.setText(product.getPrice() > 0       ? String.valueOf(product.getPrice())       : "");
        etQuantity.setText(product.getQuantity() > 0  ? String.valueOf(product.getQuantity())    : "");
        etPrice2.setText(product.getpriceCash() > 0   ? String.valueOf(product.getpriceCash())   : "");
        etPrice2s.setText(product.getpriceOther() > 0 ? String.valueOf(product.getpriceOther())  : "");
        etCommission.setText(product.getCommission() > 0 ? String.valueOf(product.getCommission()) : "");

        if (product.getDelegateName() != null) {
            spinnerDelegate.setText(product.getDelegateName(), false);
        }

        btnAdd.setText("تعديل المنتج ✏️");
        btnCancelEdit.setVisibility(View.VISIBLE);

        rvProducts.smoothScrollToPosition(0);
        Toast.makeText(this, "تعديل: " + product.getName(), Toast.LENGTH_SHORT).show();
    }

    // ─── نتيجة الحفظ ──────────────────────────────────────────────────────────
    private void handleSaveResult(boolean success, String successMsg) {
        btnAdd.setEnabled(true);
        progressBar.setVisibility(View.GONE);

        if (success) {
            Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
            clearInputs();
            editingKey          = null;
            editingDelegateName = null;
            btnAdd.setText("إضافة المنتج");
            btnCancelEdit.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "فشلت العملية، حاول مرة أخرى", Toast.LENGTH_LONG).show();
        }
    }

    // ─── تأكيد الحذف ──────────────────────────────────────────────────────────
    private void confirmDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("حذف المنتج")
                .setMessage("هل تريد حذف \"" + product.getName() + "\" نهائياً؟")
                .setPositiveButton("حذف", (d, w) -> deleteProduct(product))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void deleteProduct(Product product) {
        if (product == null || product.getId() == null) return;

        String delegateName = product.getDelegateName();
        if (delegateName == null || delegateName.isEmpty()) {
            Toast.makeText(this, "لا يمكن تحديد مسار المنتج", Toast.LENGTH_SHORT).show();
            return;
        }

        productsRef.child(delegateName).child(product.getId()).removeValue()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "تم حذف المنتج", Toast.LENGTH_SHORT).show();
                        if (product.getId().equals(editingKey)) {
                            clearInputs();
                            editingKey          = null;
                            editingDelegateName = null;
                            btnAdd.setText("إضافة المنتج");
                            btnCancelEdit.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(this, "فشل الحذف", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─── فلترة البحث (على بيانات المندوب المحدد فقط) ──────────────────────────
    private void filterProducts(String query) {
        if (query.isEmpty()) {
            productList.clear();
            productList.addAll(productListBackup);
        } else {
            String q = query.toLowerCase();
            List<Product> filtered = new ArrayList<>();
            for (Product p : productListBackup) {
                if (p.getName() != null && p.getName().toLowerCase().contains(q)) {
                    filtered.add(p);
                }
            }
            productList.clear();
            productList.addAll(filtered);
        }
        adapter.notifyDataSetChanged();
    }

    // ─── أدوات مساعدة ─────────────────────────────────────────────────────────
    private String nameToKey(String name) {
        if (name == null) return "";
        return name.trim()
                .replace(" ", "_")
                .replace(".", "")
                .replace("$", "")
                .replace("#", "")
                .replace("[", "")
                .replace("]", "")
                .replace("/", "");
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private double parseDouble(String v) {
        try { return TextUtils.isEmpty(v) ? 0.0 : Double.parseDouble(v); }
        catch (Exception e) { return 0.0; }
    }

    private int parseInteger(String v) {
        try { return TextUtils.isEmpty(v) ? 0 : Integer.parseInt(v); }
        catch (Exception e) { return 0; }
    }

    private void cancelEdit() {
        editingKey          = null;
        editingDelegateName = null;
        clearInputs();
        btnAdd.setText("إضافة المنتج");
        btnCancelEdit.setVisibility(View.GONE);
        // إعادة عرض منتجات المندوب المحدد بعد الإلغاء
        if (currentSelectedDelegate != null) {
            spinnerDelegate.setText(currentSelectedDelegate, false);
        }
    }

    private void clearInputs() {
        etName.setText("");
        etPrice.setText("");
        etPrice2.setText("");
        etPrice2s.setText("");
        etQuantity.setText("");
        etCommission.setText("");
        spinnerDelegate.setText("", false);
        etName.requestFocus();
    }

    // ─── دورة الحياة ──────────────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (productsListener != null && currentDelegateRef != null) {
            currentDelegateRef.removeEventListener(productsListener);
        }
    }
}