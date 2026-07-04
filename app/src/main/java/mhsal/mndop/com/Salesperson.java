package mhsal.mndop.com;

public class Salesperson {

    private String id;
    private String name;
    private String code;
    private String warehouse;
    private String phone;        // رقم الهاتف الجديد

    // Constructor فارغ لـ Firebase
    public Salesperson() {}

    public Salesperson(String id, String name, String code, String warehouse, String phone) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.warehouse = warehouse;
        this.phone = phone;
    }

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getWarehouse() { return warehouse; }
    public void setWarehouse(String warehouse) { this.warehouse = warehouse; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}