package mhsal.mndop.com;

import java.nio.charset.Charset;

public class ReceiptBuilder {

    private static final String TD   = "style='border:1px solid #000;padding:1px 3px;text-align:center;vertical-align:middle;overflow:hidden;'";
    private static final String TDB  = "style='border:1px solid #000;padding:1px 3px;text-align:center;vertical-align:middle;font-weight:bold;overflow:hidden;'";
    private static final String TDL  = "style='border:1px solid #000;padding:1px 3px;text-align:center;vertical-align:middle;font-weight:bold;background:#eee;overflow:hidden;'";
    private static final String TDS  = "style='border:1px solid #000;padding:1px 3px;text-align:center;vertical-align:middle;font-size:7px;overflow:hidden;'";
    private static final String ITD  = "style='border:1px solid #000;padding:1px 2px;text-align:center;vertical-align:middle;overflow:hidden;'";
    private static final String ITDL = "style='border:1px solid #000;padding:1px 2px;text-align:center;vertical-align:middle;font-weight:bold;overflow:hidden;'";

    public static String buildHtml(PrintActivity.ReceiptData r) {
        PrintActivity.SaleRecord  s = r.sale;
        PrintActivity.CompanyInfo c = r.company;

        int    installmentNo  = r.installmentIndex + 1;
        int    totalInst      = r.totalInstallments;
        long   instAmount     = r.installmentAmount;
        String customerName   = nvl(s.customerName);
        String area           = nvl(s.area);
        String phone          = nvl(s.phone);
        String workPlace      = nvl(s.address);
        long   invoiceId      = s.id;
        double totalAmount    = s.totalAmount;
        double downPayment    = s.downPayment;

        String product1 = "", product2 = "", product3 = "";
        if (s.products != null && !s.products.isEmpty()) {
            int total = s.products.size();
            int base  = total / 3;
            int extra = total % 3;
            int size1 = base + (extra > 0 ? 1 : 0);
            int size2 = base + (extra > 1 ? 1 : 0);
            product1 = joinProducts(s.products, 0,             size1);
            product2 = joinProducts(s.products, size1,         size1 + size2);
            product3 = joinProducts(s.products, size1 + size2, total);
        }

        long paidSoFar = (long) downPayment;
        if (s.installmentValues != null) {
            for (int i = 0; i <= r.installmentIndex; i++) {
                if (i < s.installmentValues.size()) paidSoFar += s.installmentValues.get(i);
            }
        }
        long remainingOnReceipt = (long) Math.max(0, totalAmount - paidSoFar);

        String installmentSystem = buildInstallmentSystem(s.installmentValues);

        java.util.Calendar firstCal = java.util.Calendar.getInstance();
        firstCal.set(r.dueYear, r.dueMonth - 1, 1);
        firstCal.add(java.util.Calendar.MONTH, -r.installmentIndex);
        int firstMonth = firstCal.get(java.util.Calendar.MONTH) + 1;
        int firstYear  = firstCal.get(java.util.Calendar.YEAR);

        return "<!DOCTYPE html><html dir='rtl'><head>"
                + "<meta charset='UTF-8'/>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "body { font-family:Arial,sans-serif; font-size:10px; direction:rtl; padding-right:12px !important; background:#fff; }"
                + "body { font-family:Arial,sans-serif; font-size:10px; direction:rtl; margin:0; padding:0; background:#fff; }"
                + ".page{width:100%;max-width:100%;margin:0;}"
                + "td { overflow:hidden; vertical-align:middle; padding: 2px 4px !important; }"
                + ".fit { display:inline-block; white-space:nowrap; max-width:100%; font-size:10px; }"
                + ".fit-sm { font-size:8px; }"
                + ".fit-bold { font-weight:bold; }"
                + ".fit-lg { font-size:14px; font-weight:bold; }"
                + ".fit-xl { font-size:16px; font-weight:bold; }"
                + ".fit-sub { font-size:11px; }"
                + ".fit-xs { font-size:7px; }"
                + "</style>"
                + "<script>"
                + "function fitText() {"
                + "  var spans = document.querySelectorAll('.fit');"
                + "  for (var i=0; i<spans.length; i++) {"
                + "    var sp = spans[i];"
                + "    var parent = sp.parentElement;"
                + "    var maxW = parent.clientWidth - 6;"
                + "    if (maxW <= 0) continue;"
                + "    var fs = parseFloat(window.getComputedStyle(sp).fontSize);"
                + "    while (sp.scrollWidth > maxW && fs > 6) {"
                + "      fs -= 0.5;"
                + "      sp.style.fontSize = fs + 'px';"
                + "    }"
                + "  }"
                + "}"
                + "window.onload = fitText;"
                + "</script>"
                + "</head><body>"
                + "<div class='page'>"
                + "<table border='1' cellspacing='0' cellpadding='0' "
                + "style='width:100%;table-layout:fixed;border-collapse:collapse;border:2px solid #000;'>"
                + "<colgroup>"
                + "<col style='width:33mm;'/>"
                + "<col style='width:22mm;'/>"
                + "<col style='width:10mm;'/>"
                + "<col style='width:52mm;'/>"
                + "<col style='width:15mm;'/>"
                + "<col style='width:63mm;'/>"
                + "</colgroup>"
                + "<tr style='height:7.5mm;'>"
                + "<td style='border:1px solid #000;text-align:center;vertical-align:middle;font-weight:bold;font-size:7px;'>"
                +   "<span class='fit fit-bold fit-xs' style='font-size:7px;'>راحة العميل غايتنا</span>"
                + "</td>"
                + "<td " + TD + "></td>"
                + "<td style='border:1px solid #000;text-align:center;vertical-align:middle;font-weight:bold;'>"
                +   "<span class='fit fit-bold'>" + installmentNo + "</span>"
                + "</td>"
                + "<td rowspan='3' style='border:1px solid #000;padding:2px 3px;text-align:center;vertical-align:middle;'>"
                +   "<div class='fit fit-xl' style='display:block;width:100%;text-align:center;'>" + nvl(c.name) + "</div>"
                +   "<div class='fit fit-sub' style='display:block;width:100%;text-align:center;margin-top:2px;'>" + nvl(c.text) + "</div>"
                + "</td>"
                + "<td rowspan='3' " + TDL + "><span class='fit fit-bold'>المـنـتـج</span></td>"
                + "<td " + TDS + "><span class='fit fit-sm'>" + product1 + "</span></td>"
                + "</tr>"
                + "<tr style='height:7.5mm;'>"
                + "<td " + TDL + "><span class='fit fit-bold'>المنطقة</span></td>"
                + "<td " + TD  + "><span class='fit'>" + area + "</span></td>"
                + "<td " + TDB + "><span class='fit fit-bold'>" + totalInst + "</span></td>"
                + "<td " + TDS + "><span class='fit fit-sm'>" + product2 + "</span></td>"
                + "</tr>"
                + "<tr style='height:7.5mm;'>"
                + "<td " + TDL + "><span class='fit fit-bold'>رقم العميل</span></td>"
                + "<td style='border:1px solid #000;text-align:center;vertical-align:middle;'>"
                +   "<span class='fit fit-lg'>" + invoiceId + "</span>"
                + "</td>"
                + "<td " + TD + "></td>"
                + "<td " + TDS + "><span class='fit fit-sm'>" + product3 + "</span></td>"
                + "</tr>"
                + "<tr style='height:7.5mm;'>"
                + "<td " + TDL + "><span class='fit fit-bold'>الأقـسـاط</span></td>"
                + "<td colspan='3' style='border:1px solid #000;text-align:center;vertical-align:middle;'>"
                +   "<span class='fit' style='font-size:9px;'>" + installmentSystem + "</span>"
                + "</td>"
                + "<td colspan='2' style='border:1px solid #000;padding:0;'>"
                +   "<table border='1' cellspacing='0' cellpadding='0' "
                +   "style='width:100%;height:100%;border-collapse:collapse;table-layout:fixed;'><tr>"
                +   "<td " + ITDL + " style='width:20%;'><span class='fit' style='font-size:8px;font-weight:bold;'>إجمالي</span></td>"
                +   "<td " + ITD  + " style='width:14%;'><span class='fit' style='font-size:8px;'>" + fmt((long)totalAmount) + "</span></td>"
                +   "<td " + ITDL + " style='width:14%;'><span class='fit' style='font-size:8px;font-weight:bold;'>مقدم</span></td>"
                +   "<td " + ITD  + " style='width:14%;'><span class='fit' style='font-size:8px;'>" + fmt((long)downPayment) + "</span></td>"
                +   "<td " + ITDL + " style='width:14%;'><span class='fit' style='font-size:8px;font-weight:bold;'>الباقي</span></td>"
                +   "<td " + ITD  + " style='width:24%;'><span class='fit' style='font-size:8px;'>" + fmt(remainingOnReceipt) + "</span></td>"
                +   "</tr></table>"
                + "</td>"
                + "</tr>"
                + "<tr style='height:7.5mm;'>"
                + "<td " + TDL + "><span class='fit fit-bold'>الاسـم</span></td>"
                + "<td colspan='3' " + TD + "><span class='fit'>" + customerName + "</span></td>"
                + "<td " + TDL + "><span class='fit fit-bold'>هـاتـف</span></td>"
                + "<td " + TD  + "><span class='fit'>" + phone + "</span></td>"
                + "</tr>"
                + "<tr style='height:7.5mm;'>"
                + "<td " + TDL + "><span class='fit fit-bold'>العـمـل</span></td>"
                + "<td colspan='3' " + TD + "><span class='fit'>" + workPlace + "</span></td>"
                + "<td " + TDL + "><span class='fit fit-bold'>أول قـسـط</span></td>"
                + "<td " + TD  + "><span class='fit'>1/" + firstMonth + "/" + firstYear + "</span></td>"
                + "</tr>"
                + "<tr style='height:9mm;'>"
                + "<td " + TDL + "><span class='fit fit-bold'>فـي تـاريـخ</span></td>"
                + "<td colspan='2' " + TDB + "><span class='fit fit-bold'>1/" + r.dueMonth + "/" + r.dueYear + "</span></td>"
                + "<td colspan='3' style='border:1px solid #000;text-align:center;vertical-align:middle;'>"
                +   "<span class='fit' style='font-size:9px;white-space:nowrap;'>"
                +     "بموجب هذه الكمبيالة أتعهد بأن أدفع مبلغ&nbsp;"
                +     "<span style='border:2px solid #000;padding:2px 8px;font-weight:bold;font-size:12px;'>" + fmt(instAmount) + "</span>"
                +     "&nbsp;<b>جنيـه فقـط</b>"
                +   "</span>"
                + "</td>"
                + "</tr>"
                + "<tr style='height:8mm;'>"
                + "<td " + TDL + "><span class='fit fit-bold'>المـنـدوب</span></td>"
                + "<td colspan='2' " + TDB + "><span class='fit fit-bold'>" + nvl(s.salesperson) + "</span></td>"
                + "<td colspan='3' style='border:1px solid #000;padding:2px 4px;font-size:8px;text-align:right;vertical-align:middle;'>"
                +   "البضاعة المباعة لاترد ولا تستبدل الا خلال 14 يوما من تاريخ الشراء "
                +   "شرط سلامة البضاعة مع العلم أن الضمان مسئولية الشركات المصنعه"
                + "</td>"
                + "</tr>"
                + "<tr style='height:7.5mm;'>"
                + "<td " + TDB + "><span class='fit fit-bold'>" + nvl(c.branch) + "</span></td>"
                + "<td colspan='5' " + TDB + "><span class='fit fit-bold'>" + nvl(c.phone) + "</span></td>"
                + "</tr>"
                + "</table></div></body></html>";
    }

    private static String joinProducts(java.util.List<PrintActivity.ProductLine> products, int from, int to) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (sb.length() > 0) sb.append(" / ");
            PrintActivity.ProductLine p = products.get(i);
            if (p.quantity > 1) sb.append(p.quantity).append("×");
            sb.append(nvl(p.name));
        }
        return sb.toString();
    }

    private static String buildInstallmentSystem(java.util.List<Long> values) {
        if (values == null || values.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < values.size()) {
            long val = values.get(i);
            int count = 0;
            while (i < values.size() && values.get(i) == val) { count++; i++; }
            if (sb.length() > 0) sb.append("+");
            sb.append(count).append("×").append(val);
        }
        return sb.toString();
    }

    public static byte[] build(PrintActivity.ReceiptData r) {
        return buildHtml(r).getBytes(Charset.forName("UTF-8"));
    }

    private static String fmt(long v)   { return String.valueOf(v); }
    private static String nvl(String s) { return s != null ? s : ""; }
}