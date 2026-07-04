package mhsal.mndop.com;

/**
 * نموذج بيانات المنتج
 * ─────────────────────────────────────────────
 * المسار في Firebase: /products/{delegateName}/{productKey}
 * مثال: /products/hussein/بطانيه_كبيره
 */
public class Product {

    private String id;            // مفتاح Firebase = اسم المنتج بعد تنظيفه
    private String name;          // اسم المنتج
    private double price;         // السعر الأساسي
    private int    quantity;      // الكمية
    private double priceCash;     // سعر الكاش
    private double priceOther;    // سعر آخر
    private double commission;    // مكافأة المندوب

    /**
     * اسم المندوب المخزّن في المسار /products/{delegateName}/...
     * لا يُخزَّن في Firebase بل يُعبَّأ برمجياً عند القراءة
     */
    private transient String delegateName;

    /** مطلوب لـ Firebase deserialization */
    public Product() {}

    public Product(String id, String name, double price, int quantity,
                   double priceCash, double priceOther, double commission) {
        this.id         = id;
        this.name       = name;
        this.price      = price;
        this.quantity   = quantity;
        this.priceCash  = priceCash;
        this.priceOther = priceOther;
        this.commission = commission;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getId()           { return id; }
    public void   setId(String id)  { this.id = id; }

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public double getPrice()              { return price; }
    public void   setPrice(double price)  { this.price = price; }

    public int  getQuantity()               { return quantity; }
    public void setQuantity(int quantity)   { this.quantity = quantity; }

    public double getpriceCash()                { return priceCash; }
    public void   setpriceCash(double priceCash){ this.priceCash = priceCash; }

    public double getpriceOther()                   { return priceOther; }
    public void   setpriceOther(double priceOther)  { this.priceOther = priceOther; }

    public double getCommission()                   { return commission; }
    public void   setCommission(double commission)  { this.commission = commission; }

    /** اسم المندوب — يُعبَّأ من مسار Firebase ولا يُخزَّن داخل المنتج */
    public String getDelegateName()                     { return delegateName; }
    public void   setDelegateName(String delegateName)  { this.delegateName = delegateName; }
}