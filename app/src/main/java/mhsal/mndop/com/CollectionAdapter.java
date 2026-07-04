package mhsal.mndop.com;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CollectionAdapter extends RecyclerView.Adapter<CollectionAdapter.VH> {

    // ══ Listener ══
    public interface OnRecordClick    { void onClick(CollectionRecord record); }
    public interface OnManagerAction  {
        void onApprove(CollectionRecord record);
        void onReject(CollectionRecord record);
        void onEdit(CollectionRecord record);
        void onDelete(CollectionRecord record);
    }

    private final Context                ctx;
    private final List<CollectionRecord> list;
    private final OnRecordClick          clickListener;
    private final OnManagerAction        managerAction;   // null = وضع المحصل (عرض فقط)
    private final boolean                isManager;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy  hh:mm a", new Locale("ar"));

    // ألوان النوع
    private static final int COLOR_ADD      = Color.parseColor("#2E7D32");
    private static final int COLOR_SUBTRACT = Color.parseColor("#E53935");
    private static final int COLOR_NORMAL   = Color.parseColor("#1565C0");

    // ألوان الحالة
    private static final int COLOR_PENDING  = Color.parseColor("#F57F17"); // برتقالي
    private static final int COLOR_APPROVED = Color.parseColor("#2E7D32"); // أخضر
    private static final int COLOR_REJECTED = Color.parseColor("#B71C1C"); // أحمر داكن

    // ── للمحصل (عرض فقط) ──
    public CollectionAdapter(Context ctx, List<CollectionRecord> list,
                             OnRecordClick clickListener) {
        this.ctx           = ctx;
        this.list          = list;
        this.clickListener = clickListener;
        this.managerAction = null;
        this.isManager     = false;
    }

    // ── للمدير (عرض + موافقة + رفض + تعديل + حذف) ──
    public CollectionAdapter(Context ctx, List<CollectionRecord> list,
                             OnRecordClick clickListener,
                             OnManagerAction managerAction) {
        this.ctx           = ctx;
        this.list          = list;
        this.clickListener = clickListener;
        this.managerAction = managerAction;
        this.isManager     = true;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx)
                .inflate(R.layout.item_collection_record, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CollectionRecord r = list.get(pos);

        // ══ لون ونص نوع العملية ══
        String type = r.getType();
        int typeColor;
        String typeLabel, amountPrefix;

        if ("زيادة".equals(type)) {
            typeColor = COLOR_ADD;    typeLabel = "▲ زيادة";  amountPrefix = "+ ";
        } else if ("نقص".equals(type)) {
            typeColor = COLOR_SUBTRACT; typeLabel = "▼ نقص";  amountPrefix = "- ";
        } else {
            typeColor = COLOR_NORMAL;  typeLabel = "✓ تحصيل"; amountPrefix = "";
        }

        // بادج النوع
        h.tvType.setText(typeLabel);
        setRoundedBackground(h.tvType, typeColor);

        // المبلغ
        h.tvAmount.setText(amountPrefix + String.format("%.0f جنيه", r.getAmount()));
        h.tvAmount.setTextColor(typeColor);

        // التاريخ
        h.tvDate.setText(r.getTimestamp() > 0
                ? DATE_FORMAT.format(new Date(r.getTimestamp())) : "—");

        // اسم العميل
        h.tvReceiver.setText(r.getReceiverName() != null ? r.getReceiverName() : "—");

        // رقم العميل
        String phone = r.getClientPhone();
        if (phone != null && !phone.isEmpty()) {
            h.tvPhone.setVisibility(View.VISIBLE);
            h.tvPhone.setText("📞 " + phone);
        } else {
            h.tvPhone.setVisibility(View.GONE);
        }

        // الملاحظة
        String note = r.getNote();
        if (note != null && !note.isEmpty()) {
            h.tvNote.setVisibility(View.VISIBLE);
            h.tvNote.setText("📝 " + note);
        } else {
            h.tvNote.setVisibility(View.GONE);
        }

        // ══ بادج حالة الموافقة ══
        String status = r.getStatus();
        int statusColor;
        String statusLabel;
        switch (status) {
            case CollectionRecord.STATUS_APPROVED:
                statusColor = COLOR_APPROVED; statusLabel = "✔ موافق"; break;
            case CollectionRecord.STATUS_REJECTED:
                statusColor = COLOR_REJECTED; statusLabel = "✖ مرفوض"; break;
            default:
                statusColor = COLOR_PENDING; statusLabel = "⏳ انتظار"; break;
        }
        h.tvStatus.setText(statusLabel);
        setRoundedBackground(h.tvStatus, statusColor);

        // حد الكارت بلون الحالة
        if (h.cardRecord != null) {
            h.cardRecord.setStrokeColor(statusColor);
            h.cardRecord.setStrokeWidth(3);
        }

        // ══ أزرار المدير ══
        if (isManager && managerAction != null) {
            h.layoutButtons.setVisibility(View.VISIBLE);

            if (r.isPending()) {
                // حالة الانتظار → موافق + رفض
                h.btnAction1.setText("✔ موافقة");
                h.btnAction1.setTextColor(Color.WHITE);
                setButtonBackground(h.btnAction1, COLOR_APPROVED);
                h.btnAction1.setOnClickListener(v -> managerAction.onApprove(r));

                h.btnAction2.setText("✖ رفض");
                h.btnAction2.setTextColor(Color.WHITE);
                setButtonBackground(h.btnAction2, COLOR_REJECTED);
                h.btnAction2.setOnClickListener(v -> managerAction.onReject(r));

            } else {
                // موافق أو مرفوض → تعديل + حذف
                h.btnAction1.setText("✏️ تعديل");
                h.btnAction1.setTextColor(typeColor);
                setButtonBackground(h.btnAction1, Color.TRANSPARENT);
                h.btnAction1.setOnClickListener(v -> managerAction.onEdit(r));

                h.btnAction2.setText("🗑️ حذف");
                h.btnAction2.setTextColor(COLOR_SUBTRACT);
                setButtonBackground(h.btnAction2, Color.parseColor("#FFEBEE"));
                h.btnAction2.setOnClickListener(v -> managerAction.onDelete(r));
            }
        } else {
            h.layoutButtons.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> {
            if (clickListener != null) clickListener.onClick(r);
        });
    }

    @Override public int getItemCount() { return list.size(); }

    // ══ helpers ══
    private void setRoundedBackground(View view, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(40f);
        d.setColor(color);
        view.setBackground(d);
    }

    private void setButtonBackground(MaterialButton btn, int color) {
        btn.setBackgroundColor(color);
    }

    // ══ ViewHolder ══
    static class VH extends RecyclerView.ViewHolder {
        MaterialCardView cardRecord;
        TextView         tvType, tvAmount, tvDate, tvReceiver, tvPhone, tvNote, tvStatus;
        View             layoutButtons;
        MaterialButton   btnAction1, btnAction2;

        VH(View v) {
            super(v);
            cardRecord    = v.findViewById(R.id.card_record);
            tvType        = v.findViewById(R.id.tv_record_type);
            tvAmount      = v.findViewById(R.id.tv_record_amount);
            tvDate        = v.findViewById(R.id.tv_record_date);
            tvReceiver    = v.findViewById(R.id.tv_record_receiver);
            tvPhone       = v.findViewById(R.id.tv_record_phone);
            tvNote        = v.findViewById(R.id.tv_record_note);
            tvStatus      = v.findViewById(R.id.tv_record_status);
            layoutButtons = v.findViewById(R.id.layout_action_buttons);
            btnAction1    = v.findViewById(R.id.btn_action1);
            btnAction2    = v.findViewById(R.id.btn_action2);
        }
    }
}