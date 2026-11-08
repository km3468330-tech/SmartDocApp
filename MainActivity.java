package com.egyptianhacker.qureo;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class MainActivity extends Activity {
    private static final int GOLD = Color.rgb(218, 176, 75);
    private static final int BG = Color.rgb(10, 10, 10);
    private static final int PANEL = Color.rgb(27, 27, 27);
    private static final int MUTED = Color.rgb(185, 185, 185);
    private SharedPreferences prefs;
    private LinearLayout page;
    private String currentUser = "";
    private boolean isAdmin = false;
    private ImageView logoView;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        prefs = getSharedPreferences("qureo_local", MODE_PRIVATE);
        if (!prefs.getBoolean("setup_done", false)) showSetup(); else showLogin();
    }

    private void base(String subtitle) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(28));
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setBackgroundColor(BG);
        scroll.addView(page);
        setContentView(scroll);
        TextView brand = text("QUREO AI SOLVER", 22, GOLD, true);
        brand.setGravity(Gravity.CENTER);
        page.addView(brand, matchWrap());
        TextView sub = text(subtitle, 13, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = matchWrap(); sp.bottomMargin = dp(20);
        page.addView(sub, sp);
    }

    private void showSetup() {
        base("GOLD EDITION • إنشاء حساب المسؤول لأول مرة");
        addLogo();
        EditText user = field("اسم المسؤول");
        EditText pass = field("كلمة مرور قوية");
        pass.setInputType(129);
        EditText confirm = field("تأكيد كلمة المرور");
        confirm.setInputType(129);
        page.addView(user); page.addView(pass); page.addView(confirm);
        button("إنشاء حساب Egyptian hacker", () -> {
            String u = user.getText().toString().trim();
            String p = pass.getText().toString();
            if (u.isEmpty() || p.length() < 8 || !p.equals(confirm.getText().toString())) {
                toast("أدخل اسمًا وكلمة مرور من 8 أحرف على الأقل وتأكد من التطابق."); return;
            }
            JSONArray users = new JSONArray();
            JSONObject admin = new JSONObject();
            try {
                admin.put("username", u); admin.put("hash", hash(p)); admin.put("salt", "local-v1");
                admin.put("role", "admin"); admin.put("blocked", false);
                users.put(admin);
            } catch (Exception e) { toast("تعذر إنشاء الحساب."); return; }
            prefs.edit().putString("users", users.toString()).putBoolean("setup_done", true).apply();
            toast("تم إنشاء حساب المسؤول. سجّل الدخول الآن.");
            showLogin();
        });
        note("سيتم حفظ الحسابات على هذا الهاتف فقط. لا يوجد خادم أو مزامنة سحابية في هذه النسخة.");
    }

    private void showLogin() {
        base("تسجيل الدخول");
        addLogo();
        EditText user = field("اسم المستخدم");
        EditText pass = field("كلمة المرور"); pass.setInputType(129);
        page.addView(user); page.addView(pass);
        button("دخول", () -> {
            String name = user.getText().toString().trim();
            String password = pass.getText().toString();
            JSONObject found = findUser(name);
            if (found == null || found.optBoolean("blocked", false) ||
                    !hash(password).equals(found.optString("hash"))) {
                toast("بيانات الدخول غير صحيحة أو الحساب موقوف."); return;
            }
            currentUser = name;
            isAdmin = "admin".equals(found.optString("role"));
            showHome();
        });
        note("حساب Egyptian hacker منفصل عن أسماء المستخدمين. كل مستخدم يدخل باسمه الخاص.");
    }

    private void showHome() {
        base(isAdmin ? "EGYPTIAN HACKER • لوحة المسؤول" : "مساحة المستخدم");
        addLogo();
        TextView welcome = text("مرحبًا، " + currentUser, 20, Color.WHITE, true);
        page.addView(welcome, matchWrap());
        if (isAdmin) {
            button("إدارة المستخدمين", this::showUsers);
            button("رفع / تغيير اللوجو", this::pickLogo);
            button("إعداد صلاحية المساعد الاختيارية", this::openAccessibilitySettings);
        }
        button("فتح منصة QUREO", () -> {
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://qureo.jp/"));
                startActivity(i);
            } catch (Exception e) { toast("تعذر فتح المتصفح."); }
        });
        button("عرض آخر نص مقروء من Chrome", this::showLastText);
        button("تسجيل الخروج", this::showLogin);
        note("هذه النسخة لا تدخل كلمات مرور QUREO ولا تحل أو ترسل إجابات تلقائيًا. استخدمها وفق قواعد المنصة، ولا تستخدم المساعد في الاختبارات الرسمية.");
    }

    private void showUsers() {
        base("إدارة الحسابات");
        button("إضافة مستخدم", this::addUserDialog);
        JSONArray arr = users();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject u = arr.optJSONObject(i); if (u == null) continue;
            String name = u.optString("username");
            String role = u.optString("role");
            if ("admin".equals(role)) continue;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackgroundColor(PANEL);
            TextView label = text(name + (u.optBoolean("blocked") ? " • محظور" : " • نشط"), 16, GOLD, true);
            row.addView(label);
            LinearLayout actions = new LinearLayout(this);
            Button block = smallButton(u.optBoolean("blocked") ? "إلغاء الحظر" : "حظر");
            block.setOnClickListener(v -> setBlocked(name, !u.optBoolean("blocked")));
            Button delete = smallButton("حذف");
            delete.setOnClickListener(v -> deleteUser(name));
            actions.addView(block); actions.addView(delete); row.addView(actions);
            LinearLayout.LayoutParams rp = matchWrap(); rp.bottomMargin = dp(8); page.addView(row, rp);
        }
        button("رجوع", this::showHome);
    }

    private void addUserDialog() {
        LinearLayout box = new LinearLayout(this); box.setPadding(30, 10, 30, 0);
        box.setOrientation(LinearLayout.VERTICAL);
        EditText name = field("اسم المستخدم الجديد");
        EditText pass = field("كلمة المرور (8 أحرف على الأقل)"); pass.setInputType(129);
        box.addView(name); box.addView(pass);
        new AlertDialog.Builder(this).setTitle("إضافة مستخدم").setView(box)
            .setPositiveButton("إضافة", (d, w) -> {
                String n = name.getText().toString().trim(), p = pass.getText().toString();
                if (n.isEmpty() || p.length() < 8 || findUser(n) != null) {
                    toast("الاسم مستخدم بالفعل أو البيانات غير مكتملة."); return;
                }
                JSONObject obj = new JSONObject();
                try {
                    obj.put("username", n); obj.put("hash", hash(p)); obj.put("salt", "local-v1");
                    obj.put("role", "user"); obj.put("blocked", false);
                    JSONArray a = users(); a.put(obj);
                    prefs.edit().putString("users", a.toString()).apply();
                    showUsers();
                } catch (Exception e) { toast("تعذر إضافة المستخدم."); }
            }).setNegativeButton("إلغاء", null).show();
    }

    private void setBlocked(String name, boolean blocked) {
        JSONArray a = users();
        for (int i=0;i<a.length();i++) {
            JSONObject o=a.optJSONObject(i);
            if (o != null && name.equals(o.optString("username"))) {
                try { o.put("blocked", blocked); } catch(Exception ignored) {}
            }
        }
        prefs.edit().putString("users", a.toString()).apply(); showUsers();
    }

    private void deleteUser(String name) {
        JSONArray old = users(), fresh = new JSONArray();
        for (int i=0;i<old.length();i++) {
            JSONObject o=old.optJSONObject(i);
            if (o != null && !name.equals(o.optString("username"))) fresh.put(o);
        }
        prefs.edit().putString("users", fresh.toString()).apply(); showUsers();
    }

    private void showLastText() {
        String s = prefs.getString("last_visible_page_text", "");
        if (s.isEmpty()) s = "لا يوجد نص بعد. إذا رغبت في استخدام القراءة المحلية، فعّل خدمة إمكانية الوصول الاختيارية من إعدادات الهاتف.";
        new AlertDialog.Builder(this).setTitle("آخر نص ظاهر في Chrome")
            .setMessage(s).setPositiveButton("حسنًا", null).show();
    }

    private void openAccessibilitySettings() {
        new AlertDialog.Builder(this)
            .setTitle("صلاحية حساسة واختيارية")
            .setMessage("تسمح هذه الخدمة بقراءة النص الظاهر في Chrome وتخزين آخر نص محليًا على الهاتف. لا ترسل البيانات إلى الإنترنت، ولا تضغط أو تكتب أو ترسل إجابات. فعّلها فقط إذا فهمت ذلك وتثق بالتطبيق.")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("فتح إعدادات إمكانية الوصول", (d,w) ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).show();
    }

    private void pickLogo() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        startActivityForResult(Intent.createChooser(i, "اختر شعار التطبيق"), 41);
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req == 41 && result == RESULT_OK && data != null && data.getData() != null) {
            try (InputStream in = getContentResolver().openInputStream(data.getData());
                 FileOutputStream out = openFileOutput("custom_logo.png", MODE_PRIVATE)) {
                byte[] buf = new byte[8192]; int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                toast("تم حفظ الشعار على الهاتف.");
                showHome();
            } catch (Exception e) { toast("تعذر حفظ الصورة."); }
        }
    }

    private void addLogo() {
        logoView = new ImageView(this);
        File f = new File(getFilesDir(), "custom_logo.png");
        if (f.exists()) logoView.setImageBitmap(BitmapFactory.decodeFile(f.getAbsolutePath()));
        else logoView.setImageResource(android.R.drawable.ic_menu_gallery);
        logoView.setColorFilter(f.exists() ? null : GOLD);
        logoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(84), dp(84));
        lp.bottomMargin = dp(16); page.addView(logoView, lp);
    }

    private JSONArray users() {
        try { return new JSONArray(prefs.getString("users", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
    private JSONObject findUser(String name) {
        JSONArray a=users();
        for(int i=0;i<a.length();i++) {
            JSONObject o=a.optJSONObject(i);
            if(o!=null && name.equalsIgnoreCase(o.optString("username"))) return o;
        }
        return null;
    }
    private String hash(String value) {
        try {
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            byte[] b=md.digest(("QUREO-GOLD-LOCAL:"+value).getBytes("UTF-8"));
            StringBuilder s=new StringBuilder();
            for(byte x:b) s.append(String.format("%02x", x));
            return s.toString();
        } catch(Exception e) { return ""; }
    }
    private EditText field(String hint) {
        EditText e=new EditText(this); e.setSingleLine(true); e.setHint(hint);
        e.setHintTextColor(MUTED); e.setTextColor(Color.WHITE);
        e.setPadding(dp(14),dp(10),dp(14),dp(10));
        e.setBackgroundColor(PANEL);
        LinearLayout.LayoutParams lp=matchWrap(); lp.bottomMargin=dp(12); e.setLayoutParams(lp);
        return e;
    }
    private void button(String label, Runnable action) {
        Button b=new Button(this); b.setText(label); b.setTextColor(Color.BLACK);
        b.setAllCaps(false); b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GOLD));
        LinearLayout.LayoutParams lp=matchWrap(); lp.bottomMargin=dp(10);
        page.addView(b,lp); b.setOnClickListener(v -> action.run());
    }
    private Button smallButton(String label) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false);
        b.setTextColor(GOLD); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        return b;
    }
    private void note(String s) {
        TextView t=text(s,12,MUTED,false); t.setPadding(4,dp(12),4,dp(8)); page.addView(t,matchWrap());
    }
    private TextView text(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1,-2);
    }
    private int dp(int v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    private void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
}
