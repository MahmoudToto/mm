package mhsal.mndop.com;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    // ─── واجهة الأحداث ────────────────────────────────────────────────────────
    public interface Listener {
        void onEdit(Product product);
        void onDelete(Product product);
        void onItemClick(Product product);
    }

    private final Context      context;
    private final List<Product> list;
    private final Listener     listener;

    public ProductAdapter(Context context, List<Product> list, Listener listener) {
        this.context  = context;
        this.list     = list;
        this.listener = listener;
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView   tvName, tvPrice, tvQty, tvPrice2, tvPrice2s, tvCommission;
        MaterialButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName       = itemView.findViewById(R.id.tv_product_name);
            tvPrice      = itemView.findViewById(R.id.tv_price);
            tvQty        = itemView.findViewById(R.id.tv_quantity);
            tvPrice2     = itemView.findViewById(R.id.tv_price2);
            tvPrice2s    = itemView.findViewById(R.id.tv_price2s);
            tvCommission = itemView.findViewById(R.id.tv_commission);
            btnEdit      = itemView.findViewById(R.id.btn_edit);
            btnDelete    = itemView.findViewById(R.id.btn_delete);
        }
    }

    // ─── Inflate ──────────────────────────────────────────────────────────────
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    // ─── Bind ─────────────────────────────────────────────────────────────────
    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Product p = list.get(position);

        h.tvName.setText(p.getName());
        h.tvPrice.setText("السعر: " + formatNum(p.getPrice()));
        h.tvQty.setText("الكمية: " + p.getQuantity());
        h.tvPrice2.setText("الكاش: " + formatNum(p.getpriceCash()));
        h.tvPrice2s.setText("سعر آخر: " + formatNum(p.getpriceOther()));

        // مكافأة المندوب
        if (p.getCommission() > 0) {
            h.tvCommission.setVisibility(View.VISIBLE);
            h.tvCommission.setText("مكافأة المندوب: " + formatNum(p.getCommission()));
        } else {
            h.tvCommission.setVisibility(View.GONE);
        }

        h.btnEdit.setOnClickListener(v -> listener.onEdit(p));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(p));
        h.itemView.setOnClickListener(v -> listener.onItemClick(p));
    }

    @Override
    public int getItemCount() { return list.size(); }

    // ─── مساعد تنسيق الأرقام ──────────────────────────────────────────────────
    private String formatNum(double val) {
        if (val == (long) val) return String.valueOf((long) val);
        return String.valueOf(val);
    }
}