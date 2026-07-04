package mhsal.mndop.com;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
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

public class UsersActivity extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────
    private TextInputLayout    tilUsername, tilPassword, tilRotbe, tilWarehouse;
    private TextInputEditText  etUsername, etPassword, etWarehouse, etSearch;
    private AutoCompleteTextView spinnerRotbe;
    private MaterialButton     btnSave, btnCancelEdit;
    private RecyclerView       rvUsers;
    private ProgressBar        progressBar;
    private FloatingActionButton fabScrollTop;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private TextView           tvFormTitle;

    // ─── Firebase ─────────────────────────────────────────────────────────────
    private DatabaseReference userRef;
    private ValueEventListener usersListener;

    // ─── Data ─────────────────────────────────────────────────────────────────
    private final List<UserModel> userList       = new ArrayList<>();
    private final List<UserModel> userListBackup = new ArrayList<>();
    private UserAdapter adapter;

    /**
     * اسم المستخدم قبل التعديل (= key القديم في Firebase)
     * null  → وضع الإضافة
     * قيمة → وضع التعديل
     */
    private String editingKey = null;

    // الرتب المتاحة في الـ dropdown
    private static final String[] ROTBE_OPTIONS = {"admin", "مشرف", "مندوب"};

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        initViews();
        setupRotbeDropdown();
        setupRecyclerView();
        setupFirebase();
        setupListeners();
        loadUsersRealtime();
    }

    // ─── تهيئة الـ Views ──────────────────────────────────────────────────────
    private void initViews() {
        tilUsername  = findViewById(R.id.til_username);
        tilPassword  = findViewById(R.id.til_password);
        tilRotbe     = findViewById(R.id.til_rotbe);
        tilWarehouse = findViewById(R.id.til_warehouse);

        etUsername  = findViewById(R.id.et_username);
        etPassword  = findViewById(R.id.et_password);
        etWarehouse = findViewById(R.id.et_warehouse);
        etSearch    = findViewById(R.id.et_search);
        spinnerRotbe = findViewById(R.id.spinner_rotbe);
        tvFormTitle  = findViewById(R.id.tv_form_title);

        btnSave       = findViewById(R.id.btn_save);
        btnCancelEdit = findViewById(R.id.btn_cancel_edit);
        swipeRefresh  = findViewById(R.id.swipe);
        fabScrollTop  = findViewById(R.id.fab_scroll_top);
        progressBar   = findViewById(R.id.progress);
        rvUsers       = findViewById(R.id.rv_users);
    }

    // ─── إعداد dropdown الرتبة ────────────────────────────────────────────────
    private void setupRotbeDropdown() {
        ArrayAdapter<String> rotbeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                ROTBE_OPTIONS
        );
        spinnerRotbe.setAdapter(rotbeAdapter);
        // الاختيار الافتراضي = مندوب
        spinnerRotbe.setText(ROTBE_OPTIONS[2], false);
    }

    // ─── إعداد RecyclerView ───────────────────────────────────────────────────
    private void setupRecyclerView() {
        adapter = new UserAdapter(this, userList, new UserAdapter.Listener() {
            @Override
            public void onEdit(UserModel user) {
                enterEditMode(user);
            }

            @Override
            public void onDelete(UserModel user) {
                confirmDelete(user);
            }
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);
    }

    // ─── إعداد Firebase ───────────────────────────────────────────────────────
    private void setupFirebase() {
        // المسار: /user
        userRef = FirebaseDatabase.getInstance().getReference("user");
    }

    // ─── إعداد المستمعين ──────────────────────────────────────────────────────
    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveUser());
        btnCancelEdit.setOnClickListener(v -> cancelEdit());
        swipeRefresh.setOnRefreshListener(this::reloadUsers);

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                filterUsers(s.toString().trim());
            }
            @Override public void afterTextChanged(android.text.Editable e) {}
        });

        rvUsers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 30) fabScrollTop.show();
                else if (!rv.canScrollVertically(-1)) fabScrollTop.hide();
            }
        });

        fabScrollTop.setOnClickListener(v -> rvUsers.smoothScrollToPosition(0));
    }

    // ─── تحميل المستخدمين بشكل لحظي ──────────────────────────────────────────
    private void loadUsersRealtime() {
        progressBar.setVisibility(View.VISIBLE);

        if (usersListener != null) userRef.removeEventListener(usersListener);

        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                userListBackup.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    UserModel u = child.getValue(UserModel.class);
                    if (u != null) {
                        // الـ key هو اسم المستخدم
                        if (u.getName() == null) u.setName(child.getKey());
                        userList.add(u);
                    }
                }

                // ترتيب أبجدي
                Collections.sort(userList,
                        (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                userListBackup.addAll(userList);

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(UsersActivity.this,
                        "خطأ: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        userRef.addValueEventListener(usersListener);
    }

    private void reloadUsers() {
        swipeRefresh.setRefreshing(true);
        loadUsersRealtime();
    }

    // ─── حفظ المستخدم (إضافة أو تعديل) ──────────────────────────────────────
    private void saveUser() {

        // ① التحقق من الحقول
        String username  = getText(etUsername);
        String password  = getText(etPassword);
        String rotbe     = spinnerRotbe.getText().toString().trim();
        String warehouseStr = getText(etWarehouse);

        if (TextUtils.isEmpty(username)) {
            tilUsername.setError("اسم المستخدم مطلوب");
            etUsername.requestFocus();
            return;
        } else { tilUsername.setError(null); }

        // اسم المستخدم بدون مسافات أو رموز خاصة
        if (username.contains(" ") || username.contains(".") || username.contains("#")) {
            tilUsername.setError("لا يسمح بمسافات أو رموز خاصة");
            etUsername.requestFocus();
            return;
        } else { tilUsername.setError(null); }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("كلمة السر مطلوبة");
            etPassword.requestFocus();
            return;
        } else if (password.length() < 6) {
            tilPassword.setError("كلمة السر يجب أن تكون 6 أحرف على الأقل");
            etPassword.requestFocus();
            return;
        } else { tilPassword.setError(null); }

        if (TextUtils.isEmpty(rotbe)) {
            tilRotbe.setError("اختر الرتبة");
            spinnerRotbe.requestFocus();
            return;
        } else { tilRotbe.setError(null); }

        int warehouse = parseInteger(warehouseStr);

        // ② في وضع الإضافة: التأكد أن الاسم غير موجود
        if (editingKey == null) {
            for (UserModel u : userListBackup) {
                if (u.getName().equalsIgnoreCase(username)) {
                    tilUsername.setError("هذا المستخدم موجود مسبقاً");
                    etUsername.requestFocus();
                    return;
                }
            }
        }

        btnSave.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        UserModel newUser = new UserModel(username, password, rotbe, warehouse);

        if (editingKey == null) {
            // ════ إضافة جديدة ════
            // المسار: /user/{username}
            userRef.child(username).setValue(newUser)
                    .addOnCompleteListener(task ->
                            handleSaveResult(task.isSuccessful(), "تم إضافة المستخدم بنجاح ✅"));

        } else {
            // ════ تعديل ════
            String oldKey = editingKey;
            String newKey = username;

            if (oldKey.equals(newKey)) {
                // الاسم لم يتغير → تحديث مباشر
                userRef.child(newKey).setValue(newUser)
                        .addOnCompleteListener(task ->
                                handleSaveResult(task.isSuccessful(), "تم تعديل المستخدم بنجاح ✅"));
            } else {
                // الاسم تغير → نكتب الجديد ثم نحذف القديم
                userRef.child(newKey).setValue(newUser)
                        .addOnCompleteListener(addTask -> {
                            if (addTask.isSuccessful()) {
                                userRef.child(oldKey).removeValue()
                                        .addOnCompleteListener(delTask ->
                                                handleSaveResult(delTask.isSuccessful(),
                                                        "تم تعديل المستخدم بنجاح ✅"));
                            } else {
                                handleSaveResult(false, "");
                            }
                        });
            }
        }
    }

    // ─── الدخول لوضع التعديل ──────────────────────────────────────────────────
    private void enterEditMode(UserModel user) {
        editingKey = user.getName();

        etUsername.setText(user.getName());
        etPassword.setText(user.getPassword());
        etWarehouse.setText(user.getWarehouse() > 0 ? String.valueOf(user.getWarehouse()) : "");
        spinnerRotbe.setText(user.getRotbe() != null ? user.getRotbe() : "مندوب", false);

        // تعطيل حقل الاسم أثناء التعديل لمنع تغيير الـ key عن طريق الخطأ
        // (إذا أردت السماح بتغييره احذف السطرين التاليين)
        // etUsername.setEnabled(false);
        // tilUsername.setHint("اسم المستخدم (لا يمكن تعديله)");

        tvFormTitle.setText("تعديل المستخدم: " + user.getName());
        btnSave.setText("حفظ التعديل ✏️");
        btnCancelEdit.setVisibility(View.VISIBLE);

        // مرر للأعلى
        rvUsers.smoothScrollToPosition(0);
        Toast.makeText(this, "تعديل: " + user.getName(), Toast.LENGTH_SHORT).show();
    }

    // ─── نتيجة الحفظ ──────────────────────────────────────────────────────────
    private void handleSaveResult(boolean success, String successMsg) {
        btnSave.setEnabled(true);
        progressBar.setVisibility(View.GONE);

        if (success) {
            Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
            clearInputs();
            editingKey = null;
            tvFormTitle.setText("إضافة مستخدم جديد");
            btnSave.setText("إضافة المستخدم");
            btnCancelEdit.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "فشلت العملية، حاول مرة أخرى", Toast.LENGTH_LONG).show();
        }
    }

    // ─── تأكيد الحذف ──────────────────────────────────────────────────────────
    private void confirmDelete(UserModel user) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ حذف المستخدم")
                .setMessage("هل تريد حذف المستخدم \"" + user.getName() + "\" نهائياً؟\n"
                        + "سيتم حذف جميع بياناته.")
                .setPositiveButton("حذف", (d, w) -> deleteUser(user))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void deleteUser(UserModel user) {
        if (user == null || user.getName() == null) return;

        progressBar.setVisibility(View.VISIBLE);
        userRef.child(user.getName()).removeValue()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "تم حذف المستخدم 🗑", Toast.LENGTH_SHORT).show();
                        // إن كنا نعدّل نفس المستخدم → إلغاء وضع التعديل
                        if (user.getName().equals(editingKey)) cancelEdit();
                    } else {
                        Toast.makeText(this, "فشل الحذف", Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─── فلترة البحث (بالاسم أو الرتبة) ──────────────────────────────────────
    private void filterUsers(String query) {
        if (query.isEmpty()) {
            userList.clear();
            userList.addAll(userListBackup);
        } else {
            String q = query.toLowerCase();
            List<UserModel> filtered = new ArrayList<>();
            for (UserModel u : userListBackup) {
                boolean matchName  = u.getName() != null
                        && u.getName().toLowerCase().contains(q);
                boolean matchRotbe = u.getRotbe() != null
                        && u.getRotbe().toLowerCase().contains(q);
                if (matchName || matchRotbe) filtered.add(u);
            }
            userList.clear();
            userList.addAll(filtered);
        }
        adapter.notifyDataSetChanged();
    }

    // ─── أدوات مساعدة ─────────────────────────────────────────────────────────
    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private int parseInteger(String v) {
        try { return TextUtils.isEmpty(v) ? 0 : Integer.parseInt(v); }
        catch (Exception e) { return 0; }
    }

    private void cancelEdit() {
        editingKey = null;
        clearInputs();
        tvFormTitle.setText("إضافة مستخدم جديد");
        btnSave.setText("إضافة المستخدم");
        btnCancelEdit.setVisibility(View.GONE);
        etUsername.setEnabled(true);
        tilUsername.setHint("اسم المستخدم *");
    }

    private void clearInputs() {
        etUsername.setText("");
        etPassword.setText("");
        etWarehouse.setText("");
        spinnerRotbe.setText(ROTBE_OPTIONS[2], false); // إعادة للافتراضي "مندوب"
        etUsername.requestFocus();
    }

    // ─── دورة الحياة ──────────────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersListener != null) userRef.removeEventListener(usersListener);
    }
}