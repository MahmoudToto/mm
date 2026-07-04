package mhsal.mndop.com;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * MainActivity: شاشة تسجيل الدخول.
 * يحفظ جلسة المستخدم في SharedPreferences عند النجاح.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_USER_KEY = "user_key";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROTBE = "rotbe";

    private TextInputEditText etName, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            Log.e(TAG, "Persistence already enabled");
        }

        // edge-to-edge padding (كما في كودك سابقًا)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- STEP 1: افحص SharedPreferences هل المستخدم مسجل سابقًا ---
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedUserKey = prefs.getString(KEY_USER_KEY, null);
        if (savedUserKey != null) {
            // المستخدم مسجل مسبقًا - نتخطى شاشة الدخول وندخل Home مباشرة
            Intent intent = new Intent(MainActivity.this, Home.class);
            // نمرر username و rotbe أيضاً من الـ prefs
            intent.putExtra("username", prefs.getString(KEY_USERNAME, ""));
            intent.putExtra("rotbe", prefs.getString(KEY_ROTBE, ""));
            startActivity(intent);
            finish();
            return;
        }

        // --- لم يتم حفظ جلسة -> نعرض شاشة تسجيل الدخول ---
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.et_name);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progress = findViewById(R.id.progress);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    private void attemptLogin() {
        final String rawName = etName.getText() != null ? etName.getText().toString() : "";
        final String name = rawName.trim();
        final String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (TextUtils.isEmpty(name)) {
            etName.setError("الرجاء إدخال الاسم");
            etName.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("الرجاء إدخال كلمة السر");
            etPassword.requestFocus();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        // مرجع إلى مسار "user" في Realtime Database
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("user");
        Query query = usersRef.orderByChild("name").equalTo(name);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "query onDataChange exists=" + snapshot.exists() + " count=" + snapshot.getChildrenCount());
                if (snapshot.exists()) {
                    // وجدنا نتائج بالضبط
                    boolean matched = false;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String dbPassword = child.child("password").getValue() != null ? child.child("password").getValue().toString() : "";
                        String dbRotbe = child.child("rotbe").getValue() != null ? child.child("rotbe").getValue().toString() : "";

                        if (dbPassword.equals(password)) {
                            matched = true;
                            onLoginSuccess(child.getKey(), child.child("name").getValue() != null ? child.child("name").getValue().toString() : "", dbRotbe);
                            break;
                        }
                    }
                    if (!matched) {
                        progress.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        Toast.makeText(MainActivity.this, "اسم المستخدم أو كلمة السر غير صحيحة", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // لا نتيجة => نسوي فحص احتياطي case-insensitive
                    fallbackCaseInsensitiveSearch(usersRef, name, password);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progress.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Log.e(TAG, "Query cancelled: " + error.getMessage());
                if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                    Toast.makeText(MainActivity.this, "لا تملك صلاحية قراءة البيانات من Firebase. تحقق من قواعد الأمان.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "خطأ بالاتصال: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void fallbackCaseInsensitiveSearch(DatabaseReference usersRef, final String inputName, final String inputPassword) {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Log.d(TAG, "Fallback scan children=" + snapshot.getChildrenCount());
                if (!snapshot.exists()) {
                    progress.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(MainActivity.this, "قاعدة المستخدمين فارغة أو لا يمكن قراءتها.", Toast.LENGTH_LONG).show();
                    return;
                }

                boolean matched = false;
                String inputLower = inputName.toLowerCase();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Object nameObj = child.child("name").getValue();
                    if (nameObj == null) continue;
                    String dbName = nameObj.toString().trim();
                    if (dbName.toLowerCase().equals(inputLower)) {
                        String dbPassword = child.child("password").getValue() != null ? child.child("password").getValue().toString() : "";
                        String dbRotbe = child.child("rotbe").getValue() != null ? child.child("rotbe").getValue().toString() : "";

                        if (dbPassword.equals(inputPassword)) {
                            matched = true;
                            onLoginSuccess(child.getKey(), dbName, dbRotbe);
                            break;
                        } else {
                            progress.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                            Toast.makeText(MainActivity.this, "كلمة السر غير صحيحة", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                }

                if (!matched) {
                    progress.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                    Toast.makeText(MainActivity.this, "المستخدم غير موجود", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progress.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Log.e(TAG, "Fallback cancelled: " + error.getMessage());
            }
        });
    }

    // عند نجاح تسجيل الدخول -> نحفظ الجلسة في SharedPreferences ثم ننتقل إلى Home
    private void onLoginSuccess(String userKey, String username, String rotbe) {
        Log.d(TAG, "Login success userKey=" + userKey + " rotbe=" + rotbe);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USER_KEY, userKey)
                .putString(KEY_USERNAME, username)
                .putString(KEY_ROTBE, rotbe != null ? rotbe : "")
                .apply();

        progress.setVisibility(View.GONE);
        btnLogin.setEnabled(true);

        Intent intent = new Intent(MainActivity.this, Home.class);
        intent.putExtra("username", username);
        intent.putExtra("rotbe", rotbe);
        startActivity(intent);
        finish();
    }
}
