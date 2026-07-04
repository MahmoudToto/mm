package mhsal.mndop.com;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PrintInvoiceAdapter extends RecyclerView.Adapter<PrintInvoiceAdapter.VH> {

    private final Context                         ctx;
    private final List<PrintActivity.SaleRecord>  allSales;
    private final List<PrintActivity.SaleRecord>  selectedSales;
    private final Runnable                        onSelectionChanged;

    public PrintInvoiceAdapter(Context ctx,
                               List<PrintActivity.SaleRecord> allSales,
                               List<PrintActivity.SaleRecord> selectedSales,
                               Runnable onSelectionChanged) {
        this.ctx               = ctx;
        this.allSales          = allSales;
        this.selectedSales     = selectedSales;
        this.onSelectionChanged = onSelectionChanged;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_print_invoice, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        PrintActivity.SaleRecord sr = allSales.get(pos);

        h.tvId.setText("فاتورة #" + sr.id);
        h.tvCustomer.setText(sr.customerName + " — " + sr.area);
        h.tvAmount.setText(String.format("%.0f جنيه | %d قسط", sr.totalAmount, sr.installmentValues.size()));

        h.checkBox.setOnCheckedChangeListener(null);
        h.checkBox.setChecked(selectedSales.contains(sr));

        h.checkBox.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                if (!selectedSales.contains(sr)) selectedSales.add(sr);
            } else {
                selectedSales.remove(sr);
            }
            onSelectionChanged.run();
        });

        h.itemView.setOnClickListener(v -> h.checkBox.setChecked(!h.checkBox.isChecked()));
    }

    @Override
    public int getItemCount() { return allSales.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView tvId, tvCustomer, tvAmount;

        VH(View v) {
            super(v);
            checkBox   = v.findViewById(R.id.cb_invoice);
            tvId       = v.findViewById(R.id.tv_invoice_id);
            tvCustomer = v.findViewById(R.id.tv_invoice_customer);
            tvAmount   = v.findViewById(R.id.tv_invoice_amount);
        }
    }
}