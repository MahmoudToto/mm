package mhsal.mndop.com;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    // ─── واجهة الأحداث ───────────────────────────────────────────────────────
    public interface Listener {
        void onEdit(UserModel user);
        void onDelete(UserModel user);
    }

    private final Context         context;
    private final List<UserModel> list;
    private final Listener        listener;

    public UserAdapter(Context context, List<UserModel> list, Listener listener) {
        this.context  = context;
        this.list     = list;
        this.listener = listener;
    }

    // ─── ViewHolder ──────────────────────────────────────────────────────────
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView       tvUsername, tvRotbeBadge, tvWarehouse;
        MaterialButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername   = itemView.findViewById(R.id.tv_username);
            tvRotbeBadge = itemView.findViewById(R.id.tv_rotbe_badge);
            tvWarehouse  = itemView.findViewById(R.id.tv_warehouse);
            btnEdit      = itemView.findViewById(R.id.btn_edit);
            btnDelete    = itemView.findViewById(R.id.btn_delete);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        UserModel u = list.get(position);

        h.tvUsername.setText(u.getName());
        h.tvWarehouse.setText("🏭 المخزن رقم: " + u.getWarehouse());

        // ── لون الـ Badge حسب الرتبة ──────────────────────────────────────
        String rotbe = u.getRotbe() != null ? u.getRotbe() : "مندوب";
        h.tvRotbeBadge.setText(rotbe);

        switch (rotbe) {
            case "admin":
                h.tvRotbeBadge.getBackground().setTint(Color.parseColor("#C62828")); // أحمر غامق
                break;
            case "مشرف":
                h.tvRotbeBadge.getBackground().setTint(Color.parseColor("#1565C0")); // أزرق
                break;
            default: // مندوب
                h.tvRotbeBadge.getBackground().setTint(Color.parseColor("#2E7D32")); // أخضر
                break;
        }

        h.btnEdit.setOnClickListener(v -> listener.onEdit(u));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(u));
    }

    @Override
    public int getItemCount() { return list.size(); }
}