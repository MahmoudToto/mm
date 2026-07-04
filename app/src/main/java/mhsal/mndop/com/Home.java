package mhsal.mndop.com;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Home extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────
    private ImageView internt;

    // ─── بطاقات الـ Dashboard ─────────────────────────────────────────────────
    private View card_clients;           // العملاء (إضافة+تعديل+حذف)
    private View card_products;          // المنتجات
    private View card_inventory;         // المخزن
    private View card_add_salesperson;   // المندوبين
    private View card_collectors;        // المحصلين
    private View card_deliver_collection;// تسليم تحصيل
    private View statistics;             // الإحصائيات
    private View card_filter;            // التصفية
    private View card_users;             // إدارة المستخدمين

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception e) {
            // Persistence might be already enabled
        }

        initViews();
        setupClicks();
        checkInternetConnection();
    }

    // ─── تهيئة الـ Views ──────────────────────────────────────────────────────
    private void initViews() {
        internt               = findViewById(R.id.internt);
        card_clients          = findViewById(R.id.card_clients);
        card_products         = findViewById(R.id.card_products);
        card_inventory        = findViewById(R.id.card_inventory);
        card_add_salesperson  = findViewById(R.id.card_add_salesperson);
        card_collectors       = findViewById(R.id.card_collectors);
        card_deliver_collection = findViewById(R.id.card_deliver_collection);
        statistics            = findViewById(R.id.statistics);
        card_filter           = findViewById(R.id.card_filter);
        card_users            = findViewById(R.id.card_users);
    }

    // ─── إعداد الضغطات ───────────────────────────────────────────────────────
    private void setupClicks() {

        // العملاء → clint.class (شاشة واحدة فيها إضافة+تعديل+حذف)
        card_clients.setOnClickListener(v ->
                startActivity(new Intent(this, PrintActivity.class)));

        // المنتجات → card_products.class
        card_products.setOnClickListener(v ->
                startActivity(new Intent(this, card_products.class)));

        // المخزن → card_inventory.class
        card_inventory.setOnClickListener(v ->
                startActivity(new Intent(this, card_inventory.class)));

        // المندوبين → add_salesperson.class
        card_add_salesperson.setOnClickListener(v ->
                startActivity(new Intent(this, add_salesperson.class)));

        // المحصلين → card_collectors.class
        card_collectors.setOnClickListener(v -> {
            startActivity(new Intent(this, CollectionManagerActivity.class));
        });

        // تسليم تحصيل → card_deliver_collection.class
        card_deliver_collection.setOnClickListener(v ->
                startActivity(new Intent(this, card_deliver_collection.class)));

        // الإحصائيات → statistics.class
        statistics.setOnClickListener(v ->
                startActivity(new Intent(this, statistics.class)));

        // التصفية → card_filter.class
        card_filter.setOnClickListener(v ->
                startActivity(new Intent(this, card_filter.class)));

        // إدارة المستخدمين → UsersActivity.class
        card_users.setOnClickListener(v ->
                startActivity(new Intent(this, UsersActivity.class)));
    }

    // ─── فحص الاتصال بـ Firebase ──────────────────────────────────────────────
    private void checkInternetConnection() {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("user");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // وصلنا لـ Firebase → إنترنت متاح
                internt.setImageResource(R.drawable.wifi);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // فشل الاتصال
                internt.setImageResource(R.drawable.no_wifi);
            }
        });
    }
}