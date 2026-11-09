package br.dev.projetor.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends Activity {

    private static final String CYAN = "#00F0FF";
    private static final String MAGENTA = "#FF2E88";
    private static final String DIM = "#5E7C8C";
    private static final String WHITE = "#E8FBFF";

    private static final String[][] APPS = {
            {"com.netflix.mediaclient", "NETFLIX"},
            {"org.jellyfin.androidtv", "JELLYFIN"},
            {"com.globo.globotv", "GLOBOPLAY"},
            {"android.settings.SETTINGS", "SISTEMA"},
            {"@all", "TODOS OS APPS"},
    };

    private TextView clock, date, status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 15000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(build());
        goFullscreen();
    }

    @Override protected void onResume() {
        super.onResume();
        goFullscreen();
        handler.removeCallbacks(tick);
        handler.post(tick);
        updateStatus();
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void goFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private FrameLayout build() {
        FrameLayout root = new FrameLayout(this);
        root.addView(new CyberBackground(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(38), dp(26), dp(38), dp(18));

        // ---- header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView brand = mono("PROJETOR // HY300", 21, CYAN, Typeface.BOLD);
        brand.setLetterSpacing(0.18f);
        titles.addView(brand);

        TextView sub = mono("SISTEMA v2.0  ·  ROOT  ·  ONLINE", 11, MAGENTA, Typeface.NORMAL);
        sub.setLetterSpacing(0.22f);
        titles.addView(sub);
        header.addView(titles, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        clock = mono("--:--", 36, WHITE, Typeface.BOLD);
        clock.setLetterSpacing(0.06f);
        clock.setShadowLayer(14f, 0, 0, 0xFF00F0FF);
        right.addView(clock);
        date = mono("", 11, DIM, Typeface.NORMAL);
        date.setLetterSpacing(0.12f);
        right.addView(date);
        header.addView(right);

        col.addView(header);
        updateClock();

        View sep = new View(this);
        sep.setBackgroundColor(0x3300F0FF);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        slp.topMargin = dp(16);
        col.addView(sep, slp);

        // ---- tiles
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(3);
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        glp.topMargin = dp(18);
        col.addView(grid, glp);

        PackageManager pm = getPackageManager();
        int index = 1;
        for (String[] entry : APPS) {
            String pkg = entry[0];
            String label = entry[1];
            Drawable icon;
            Intent launch;

            if ("@all".equals(pkg)) {
                launch = new Intent(this, AllAppsActivity.class);
                icon = getDrawable(R.drawable.icon);
            } else if ("android.settings.SETTINGS".equals(pkg)) {
                launch = new Intent(android.provider.Settings.ACTION_SETTINGS);
                icon = getDrawable(R.drawable.icon);
                for (String sp : new String[]{"com.htc.htcsettings", "com.android.tv.settings"}) {
                    try { icon = pm.getApplicationIcon(sp); break; } catch (Exception ignored) { }
                }
            } else {
                launch = pm.getLaunchIntentForPackage(pkg);
                if (launch == null) continue;
                icon = getDrawable(R.drawable.icon);
                try {
                    icon = pm.getApplicationIcon(pkg);
                    label = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)))
                            .toUpperCase(Locale.getDefault());
                } catch (Exception ignored) { }
            }

            View tile = makeTile(icon, label, String.format(Locale.US, "%02d", index++), launch);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = tileWidth();
            lp.height = dp(146);
            lp.setMargins(0, 0, dp(18), dp(18));
            grid.addView(tile, lp);
        }

        // ---- footer
        status = mono("", 11, DIM, Typeface.NORMAL);
        status.setLetterSpacing(0.1f);
        col.addView(status);

        root.addView(col, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    private View makeTile(Drawable icon, String label, String tag, final Intent launch) {
        FrameLayout tile = new FrameLayout(this);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        tile.setBackground(new NeonTileDrawable(false, dp(14)));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(dp(14), dp(10), dp(14), dp(12));

        TextView idx = mono(tag, 10, "#3A5C6B", Typeface.NORMAL);
        idx.setLetterSpacing(0.2f);
        inner.addView(idx);

        ImageView img = new ImageView(this);
        img.setImageDrawable(icon);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(54), dp(54));
        ilp.topMargin = dp(2);
        inner.addView(img, ilp);

        TextView text = mono(label, 14, WHITE, Typeface.BOLD);
        text.setLetterSpacing(0.14f);
        text.setMaxLines(1);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(8);
        inner.addView(text, tlp);

        tile.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean has) {
                v.setBackground(new NeonTileDrawable(has, dp(14)));
                v.animate().scaleX(has ? 1.045f : 1f).scaleY(has ? 1.045f : 1f)
                        .setDuration(80).start();
            }
        });

        tile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launch);
                } catch (Exception e) {
                    android.widget.Toast.makeText(HomeActivity.this,
                            "FALHA: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
        return tile;
    }

    private TextView mono(String text, int sp, String color, int style) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTypeface(Typeface.create("monospace", style));
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        v.setTextColor(android.graphics.Color.parseColor(color));
        return v;
    }

    private void updateClock() {
        if (clock == null) return;
        clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        String d = new SimpleDateFormat("EEE dd MMM yyyy", new Locale("pt", "BR"))
                .format(new Date()).toUpperCase(new Locale("pt", "BR"));
        date.setText(d);
    }

    private void updateStatus() {
        if (status == null) return;
        String ip = "-", load = "-", temp = "-";
        try {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager)
                    getApplicationContext().getSystemService(WIFI_SERVICE);
            int raw = wm.getConnectionInfo().getIpAddress();
            if (raw != 0) {
                ip = String.format(Locale.US, "%d.%d.%d.%d",
                        raw & 0xff, (raw >> 8) & 0xff, (raw >> 16) & 0xff, (raw >> 24) & 0xff);
            }
        } catch (Exception ignored) { }
        load = readLine("/proc/loadavg");
        if (load.length() > 4) load = load.substring(0, 4);
        try {
            String t = readLine("/sys/class/thermal/thermal_zone0/temp").trim();
            temp = String.valueOf(Integer.parseInt(t) / 1000);
        } catch (Exception ignored) { }
        status.setText("SYS:ONLINE   NET:" + ip + "   LOAD:" + load + "   SOC:" + temp + "C   RAM:1G");
    }

    private String readLine(String path) {
        try (BufferedReader r = new BufferedReader(new FileReader(path))) {
            String l = r.readLine();
            return l == null ? "-" : l;
        } catch (Exception e) {
            return "-";
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private int tileWidth() {
        int w = getResources().getDisplayMetrics().widthPixels;
        return (w - dp(38) * 2 - dp(18) * 2) / 3;
    }
}
