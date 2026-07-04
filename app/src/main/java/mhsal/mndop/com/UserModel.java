package mhsal.mndop.com;

/**
 * نموذج بيانات المستخدم
 * المسار في Firebase: /user/{username}/
 *
 * البنية:
 * /user/hussein/
 *     name:      "hussein"
 *     password:  "pas12345"
 *     rotbe:     "admin"
 *     warehouse: 1
 */
public class UserModel {

    private String name;       // اسم المستخدم = key في Firebase
    private String password;   // كلمة السر
    private String rotbe;      // الرتبة: admin / مندوب / مشرف
    private int    warehouse;  // رقم المخزن

    /** مطلوب لـ Firebase deserialization */
    public UserModel() {}

    public UserModel(String name, String password, String rotbe, int warehouse) {
        this.name      = name;
        this.password  = password;
        this.rotbe     = rotbe;
        this.warehouse = warehouse;
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────

    public String getName()             { return name; }
    public void   setName(String name)  { this.name = name; }

    public String getPassword()                 { return password; }
    public void   setPassword(String password)  { this.password = password; }

    public String getRotbe()              { return rotbe; }
    public void   setRotbe(String rotbe)  { this.rotbe = rotbe; }

    public int  getWarehouse()                { return warehouse; }
    public void setWarehouse(int warehouse)   { this.warehouse = warehouse; }
}