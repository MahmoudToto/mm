package mhsal.mndop.com;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class CollectionRecord {

    // ══ حالات الموافقة ══
    public static final String STATUS_PENDING  = "انتظار";
    public static final String STATUS_APPROVED = "موافق";
    public static final String STATUS_REJECTED = "مرفوض";

    private String id;
    private double amount;
    private String receiverName;
    private String clientPhone;
    private String note;
    private long   timestamp;
    private String type;       // "تحصيل" | "زيادة" | "نقص"
    private String status;     // "انتظار" | "موافق" | "مرفوض"

    public CollectionRecord() {}

    public CollectionRecord(double amount, String receiverName, String clientPhone,
                            String note, long timestamp, String type) {
        this.amount       = amount;
        this.receiverName = receiverName;
        this.clientPhone  = clientPhone;
        this.note         = note;
        this.timestamp    = timestamp;
        this.type         = type;
        this.status       = STATUS_PENDING; // كل سجل جديد → انتظار
    }

    // ── Getters ──
    public String getId()           { return id; }
    public double getAmount()       { return amount; }
    public String getReceiverName() { return receiverName; }
    public String getClientPhone()  { return clientPhone; }
    public String getNote()         { return note; }
    public long   getTimestamp()    { return timestamp; }
    public String getType()         { return type; }
    public String getStatus()       { return status != null ? status : STATUS_PENDING; }

    // ── Setters ──
    public void setId(String id)             { this.id = id; }
    public void setAmount(double amount)     { this.amount = amount; }
    public void setReceiverName(String v)    { this.receiverName = v; }
    public void setClientPhone(String v)     { this.clientPhone = v; }
    public void setNote(String note)         { this.note = note; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setType(String type)         { this.type = type; }
    public void setStatus(String status)     { this.status = status; }

    // ── helpers ──
    public boolean isPending()  { return STATUS_PENDING.equals(getStatus()); }
    public boolean isApproved() { return STATUS_APPROVED.equals(getStatus()); }
    public boolean isRejected() { return STATUS_REJECTED.equals(getStatus()); }
}