package mhsal.mndop.com;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SalespersonAdapter extends RecyclerView.Adapter<SalespersonAdapter.ViewHolder> {

    private final Context context;
    private final List<Salesperson> list;
    private final Listener listener;

    public interface Listener {
        void onEdit(Salesperson salesperson);
        void onDelete(Salesperson salesperson);
    }

    public SalespersonAdapter(Context context, List<Salesperson> list, Listener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_salesperson, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Salesperson s = list.get(position);

        holder.tvName.setText(s.getName());
        holder.tvCode.setText("كود: " + s.getCode());
        holder.tvWarehouse.setText("المخزن: " + s.getWarehouse());

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(s));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(s));
        // أضف هذا داخل onBindViewHolder
        holder.tvPhone.setText(s.getPhone() != null && !s.getPhone().isEmpty()
                ? "📞 " + s.getPhone() : "");




    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCode, tvWarehouse;
        ImageButton btnEdit, btnDelete;
        // وفي الـ ViewHolder أضف:
        TextView tvPhone;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvCode = itemView.findViewById(R.id.tv_code);
            tvWarehouse = itemView.findViewById(R.id.tv_warehouse);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            // في onCreateViewHolder:
            tvPhone = itemView.findViewById(R.id.tv_phone);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}