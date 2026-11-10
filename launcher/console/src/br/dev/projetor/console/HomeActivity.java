package br.dev.projetor.console;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.os.SystemClock;
import android.app.ActivityManager;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import java.util.Locale;

/**
 * CATHODE HYBRID home: smooth card layer, anti-aliased numbers, no bitmap fonts.
 * Layout rolled out from Shared/design/hybrid/projetor-1024x600-home@2x.png,
 * with the 2026-11-10 09:38 correction: numbers are round/smooth, not pixelated.
 */
public class HomeActivity extends Activity {

    private static final int BG = 0xFF06070C;
    private static final int SUNKEN = 0xFF0A0C14;
    private static final int PANEL = 0xFF0C1018;
    private static final int LINE = 0xFF1B2433;
    private static final int LINE_HI = 0xFF2C3A50;
    private static final int INK = 0xFFDDF6FF;
    private static final int DIM = 0xFF8B9FB3;
    private static final int MUTE = 0xFF66788C;
    private static final int CYAN = 0xFF22E8FF;
    private static final int MAGENTA = 0xFFFF2E88;
    private static final int LIME = 0xFF9CFF4A;
    private static final int AMBER = 0xFFFFC24A;
    private static final int RED = 0xFFFF4D5E;

    private static final String[][] APPS = {
            {"com.netflix.mediaclient", "Netflix"},
            {"org.jellyfin.androidtv", "Jellyfin"},
            {"com.globo.globotv", "Globoplay"},
            {"com.android.vending", "Play"},
            {"android.settings.SETTINGS", "Ajustes"},
    };

    private TextView clock;
    private TextView date;
    private TextView heroRam;
    private TextView memValue;
    private TextView tempValue;
    private TextView loadValue;
    private TextView netIp;
    private TextView netTraffic;
    private TextView netUptime;
    private TextView heroStorage;
    private TextView storageRows;
    private TextView storageUsed;
    private TextView footLoad;
    private TextView footRam;
    private TextView footSoc;
    private TextView footNet;
    private TextView footSys;

    private Typeface mono;
    private Typeface monoMedium;
    private Typeface numLight;
    private Typeface numMedium;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            updateStats();
            handler.postDelayed(this, 10000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mono = Typeface.createFromAsset(getAssets(), "fonts/JetBrainsMono-Regular.ttf");
        monoMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        // round numerals (team direction 2026-11-10): Manrope ExtraBold, no pixel face
        numLight = Typeface.createFromAsset(getAssets(), "fonts/Manrope-800.ttf");
        numMedium = numLight;
        setContentView(buildUi());
        goFullscreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        goFullscreen();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void goFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    // ---------------------------------------------------------------- layout

    private View buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.addView(new DotGrid(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(40), dp(18), dp(40), dp(12));

        column.addView(buildHeader(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        column.addView(space(14));

        LinearLayout apps = new LinearLayout(this);
        apps.setOrientation(LinearLayout.HORIZONTAL);
        apps.setClipChildren(false);
        int added = buildAppTiles(apps);
        column.addView(apps, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.35f));

        column.addView(space(10));

        LinearLayout status = new LinearLayout(this);
        status.setOrientation(LinearLayout.HORIZONTAL);
        status.setClipChildren(false);
        status.addView(buildSystemCard(), weightParams(1f, ViewGroup.LayoutParams.MATCH_PARENT));
        status.addView(space(12));
        status.addView(buildNetworkCard(), weightParams(1f, ViewGroup.LayoutParams.MATCH_PARENT));
        status.addView(space(12));
        status.addView(buildStorageCard(), weightParams(1f, ViewGroup.LayoutParams.MATCH_PARENT));
        column.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        column.addView(space(10));

        column.addView(buildAllAppsTile(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.55f));

        column.addView(buildFooter(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(column, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    private View buildHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.HORIZONTAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.addView(text("PROJETOR", 18, INK, monoMedium, 0.6f));
        brand.addView(text("//", 18, CYAN, monoMedium, 0.6f));
        brand.addView(text("HY300", 18, MAGENTA, monoMedium, 0.6f));
        left.addView(brand);
        left.addView(text("SISTEMA V2.0 · ROOT · ONLINE · 1024x600 @30", 10, DIM, mono, 0.3f));

        row.addView(left, weightParams(1f, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);

        clock = text("--:--", 34, INK, numLight, 0f);
        date = text("", 10, DIM, mono, 0.3f);
        date.setGravity(Gravity.RIGHT);
        right.addView(clock);
        right.addView(date);

        row.addView(right, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private int buildAppTiles(LinearLayout row) {
        PackageManager pm = getPackageManager();
        int added = 0;
        for (String[] entry : APPS) {
            if (added >= 3) break;
            final String pkg = entry[0];
            String label = entry[1];
            Drawable icon = null;
            Intent launch;

            if ("android.settings.SETTINGS".equals(pkg)) {
                launch = new Intent(android.provider.Settings.ACTION_SETTINGS);
                for (String sp : new String[]{"com.htc.htcsettings", "com.android.tv.settings"}) {
                    try {
                        icon = pm.getApplicationIcon(sp);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            } else {
                launch = pm.getLaunchIntentForPackage(pkg);
                if (launch == null) continue;
                try {
                    icon = pm.getApplicationIcon(pkg);
                    label = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)));
                } catch (Exception ignored) {
                }
            }
            if (icon == null) {
                Drawable fallback = getDrawable(R.drawable.icon);
                icon = fallback != null ? fallback : getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            }

            if (added > 0) row.addView(space(14));
            View tile = makeAppTile(added + 1, icon, label, launch);
            row.addView(tile, weightParams(1f, ViewGroup.LayoutParams.MATCH_PARENT));
            added++;
        }
        return added;
    }

    private View makeAppTile(int number, Drawable icon, String label, final Intent launch) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setBackground(cardBackground(false));

        TextView index = text(String.format(Locale.US, "%02d", number), 9, MUTE, mono, 1.0f);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ip.leftMargin = dp(14);
        ip.topMargin = dp(10);
        tile.addView(index, ip);

        ImageView img = new ImageView(this);
        img.setImageDrawable(icon);
        LinearLayout.LayoutParams iml = new LinearLayout.LayoutParams(dp(72), dp(72));
        iml.gravity = Gravity.CENTER_HORIZONTAL;
        iml.topMargin = dp(6);
        tile.addView(img, iml);

        TextView title = text(label.toUpperCase(Locale.getDefault()), 18, INK, monoMedium, 0.8f);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(1);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.topMargin = dp(14);
        tile.addView(title, tp);

        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(cardBackground(hasFocus));
                v.animate().scaleX(hasFocus ? 1.02f : 1f).scaleY(hasFocus ? 1.02f : 1f)
                        .setDuration(90).start();
            }
        });
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launch);
                } catch (Exception e) {
                    toast("Nao abriu: " + e.getMessage());
                }
            }
        });
        return tile;
    }

    private View buildSystemCard() {
        LinearLayout card = card();
        card.addView(label("SISTEMA"));

        heroRam = text("--", 34, INK, numLight, 0f);
        card.addView(heroRam);

        memValue = row(card, "MEM", "--");
        tempValue = row(card, "TEMP", "--");
        loadValue = row(card, "LOAD", "--");
        return card;
    }

    private View buildNetworkCard() {
        LinearLayout card = card();
        card.addView(label("REDE"));

        netIp = text("--", 20, INK, numMedium, 0f);
        card.addView(netIp);

        netTraffic = row(card, "TRAFEGO", "--");
        netUptime = row(card, "UPTIME", "--");
        row(card, "DHCP", "ativo");
        return card;
    }

    private View buildStorageCard() {
        LinearLayout card = card();
        card.addView(label("ARMAZENAMENTO"));

        heroStorage = text("--", 34, INK, numLight, 0f);
        card.addView(heroStorage);

        storageRows = row(card, "LIVRE", "--");
        storageUsed = row(card, "USO", "--");
        row(card, "APPS", String.valueOf(countApps()));
        return card;
    }

    private View buildAllAppsTile() {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setBackground(cardBackground(false));
        tile.setPadding(dp(20), 0, dp(20), 0);

        tile.addView(new SquareGlyph(this), new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = dp(18);
        texts.addView(text("TODOS OS APPS", 22, INK, monoMedium, 0.8f));
        texts.addView(text(countApps() + " INSTALADOS", 10, DIM, mono, 0.6f));
        tile.addView(texts, tlp);

        TextView open = text("ABRIR  >", 11, CYAN, monoMedium, 1.2f);
        LinearLayout.LayoutParams olp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        open.setGravity(Gravity.RIGHT);
        tile.addView(open, olp);

        tile.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                v.setBackground(cardBackground(hasFocus));
            }
        });
        tile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, AllAppsActivity.class));
            }
        });
        return tile;
    }

    private View buildFooter() {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);

        View line = new View(this);
        line.setBackgroundColor(LINE);
        wrap.addView(line, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(8), 0, 0);

        footSys = footerItem(row, "SYS", "ONLINE", LIME);
        footNet = footerItem(row, "NET", "--", DIM);
        footLoad = footerItem(row, "LOAD", "--", DIM);
        footSoc = footerItem(row, "TEMP", "--", AMBER);
        footRam = footerItem(row, "RAM", "--", DIM);
        wrap.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    private TextView footerItem(LinearLayout row, String key, String value, int color) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.addView(text(key + " ", 10, MUTE, mono, 0.8f));
        TextView v = text(value, 10, color, monoMedium, 0.8f);
        item.addView(v);
        row.addView(item, weightParams(1f, ViewGroup.LayoutParams.WRAP_CONTENT));
        return v;
    }

    // ---------------------------------------------------------------- blocks

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBackground(false));
        card.setPadding(dp(16), dp(10), dp(16), dp(10));
        return card;
    }

    private TextView row(LinearLayout card, String key, String value) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        TextView k = text(key, 9, MUTE, mono, 0.8f);
        TextView v = text(value, 10, DIM, mono, 0f);
        v.setGravity(Gravity.RIGHT);
        line.addView(k, new LinearLayout.LayoutParams(dp(92), ViewGroup.LayoutParams.WRAP_CONTENT));
        line.addView(v, weightParams(1f, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(2);
        card.addView(line, lp);
        return v;
    }

    private TextView label(String s) {
        return text(s, 9, MUTE, mono, 1.2f);
    }

    private TextView text(String s, int sp, int color, Typeface tf, float tracking) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTypeface(tf);
        if (tracking != 0f) t.setLetterSpacing(tracking);
        t.setIncludeFontPadding(false);
        return t;
    }

    private GradientDrawable cardBackground(boolean focused) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{focused ? 0xFF101722 : PANEL, BG});
        d.setCornerRadius(dp(8));
        d.setStroke(dp(focused ? 2 : 1), focused ? CYAN : LINE);
        return d;
    }

    private View space(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp, dp));
        return v;
    }

    private LinearLayout.LayoutParams weightParams(float weight, int height) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, height, weight);
        return lp;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void toast(String s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show();
    }

    private int countApps() {
        try {
            Intent main = new Intent(Intent.ACTION_MAIN, null);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            java.util.HashSet<String> labels = new java.util.HashSet<String>();
            android.content.pm.PackageManager pm = getPackageManager();
            for (android.content.pm.ResolveInfo ri : pm.queryIntentActivities(main, 0)) {
                if (ri.activityInfo == null) continue;
                String p = ri.activityInfo.packageName;
                if (getPackageName().equals(p) || "br.dev.projetor.home".equals(p)) continue;
                // Same collapse as AllAppsActivity: two settings apps share one label.
                labels.add(String.valueOf(ri.loadLabel(pm)).toLowerCase(java.util.Locale.getDefault()));
            }
            return labels.size();
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------------------------------------------------------- live data

    private void updateClock() {
        if (clock == null) return;
        clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        String d = new SimpleDateFormat("EEE, dd MMM yyyy", new Locale("pt", "BR")).format(new Date());
        date.setText(d.toUpperCase(new Locale("pt", "BR")));
    }

    private void updateStats() {
        int ram = -1;
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            if (mi.totalMem > 0) ram = (int) Math.round(100.0 * (mi.totalMem - mi.availMem) / mi.totalMem);
        } catch (Exception ignored) {
        }
        String ramText = ram < 0 ? "--" : ram + "%";
        if (heroRam != null) heroRam.setText(ram < 0 ? "--" : String.valueOf(ram));
        if (memValue != null) memValue.setText(ramText);
        if (footRam != null) footRam.setText(ramText);

        String temp = thermalTemp();
        if (tempValue != null) tempValue.setText(temp);
        if (footSoc != null) footSoc.setText(temp);

        String load = loadAvg();
        if (loadValue != null) loadValue.setText(load);
        if (footLoad != null) footLoad.setText(load);

        String ip = ipAddress();
        if (netIp != null) netIp.setText(ip);
        if (footNet != null) footNet.setText(ip);

        if (netTraffic != null) netTraffic.setText(netTraffic());
        if (netUptime != null) netUptime.setText(uptime());

        long[] st = storage();
        if (heroStorage != null) heroStorage.setText(st[1] > 0 ? String.valueOf(st[1]) + "G" : "--");
        if (storageRows != null) storageRows.setText(st[1] > 0 ? st[1] + " GB" : "--");
        if (storageUsed != null) storageUsed.setText(st[0] > 0 ? st[0] + "%" : "--");

        if (footSys != null) footSys.setText("ONLINE");
    }

    private String thermalTemp() {
        String raw = null;
        String[] zones = {"thermal_zone0", "thermal_zone1", "thermal_zone2"};
        for (String zone : zones) {
            raw = readFirstLine("/sys/class/thermal/" + zone + "/temp");
            if (raw != null) break;
        }
        if (raw != null) {
            try {
                double c = Double.parseDouble(raw.trim());
                if (c > 1000) c /= 1000.0;
                if (c > 0 && c < 150) return Math.round(c) + "\u00B0C";
            } catch (Exception ignored) {
            }
        }
        try {
            Intent bat = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (bat != null) {
                int t = bat.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Integer.MIN_VALUE);
                if (t != Integer.MIN_VALUE && t > 0) return Math.round(t / 10f) + "\u00B0C";
            }
        } catch (Exception ignored) {
        }
        return "--";
    }

    private String loadAvg() {
        String raw = readFirstLine("/proc/loadavg");
        if (raw == null) return "--";
        String[] parts = raw.trim().split("\\s+");
        if (parts.length == 0) return "--";
        try {
            return String.format(Locale.US, "%.2f", Double.parseDouble(parts[0]));
        } catch (Exception e) {
            return "--";
        }
    }

    private String ipAddress() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network active = cm.getActiveNetwork();
            if (active != null) {
                LinkProperties lp = cm.getLinkProperties(active);
                if (lp != null) {
                    for (LinkAddress la : lp.getLinkAddresses()) {
                        InetAddress addr = la.getAddress();
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        try {
            for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (nif.isLoopback()) continue;
                for (InetAddress addr : Collections.list(nif.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "--";
    }

    private String netTraffic() {
        long[] bytes = netBytes();
        if (bytes[0] < 0) return "--";
        return fmtBytes(bytes[0]) + " rx · " + fmtBytes(bytes[1]) + " tx";
    }

    private long[] netBytes() {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader("/proc/net/dev"));
            String line;
            long rx = 0, tx = 0;
            while ((line = r.readLine()) != null) {
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String iface = line.substring(0, colon).trim();
                if ("lo".equals(iface)) continue;
                String[] f = line.substring(colon + 1).trim().split("\\s+");
                if (f.length < 9) continue;
                rx += Long.parseLong(f[0]);
                tx += Long.parseLong(f[8]);
            }
            return new long[]{rx, tx};
        } catch (Exception e) {
            return new long[]{-1, -1};
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) { }
        }
    }

    private String fmtBytes(long b) {
        if (b < 1024) return b + "B";
        double kb = b / 1024.0;
        if (kb < 1024) return Math.round(kb) + "K";
        double mb = kb / 1024.0;
        if (mb < 1024) return Math.round(mb) + "M";
        return String.format(Locale.US, "%.1fG", mb / 1024.0);
    }

    private String uptime() {
        long ms = SystemClock.elapsedRealtime();
        long h = ms / 3600000;
        long m = (ms / 60000) % 60;
        if (h == 0) return m + "m";
        return h + "h" + (m < 10 ? "0" : "") + m;
    }

    /** {usedPercent, freeGB} */
    private long[] storage() {
        try {
            StatFs fs = new StatFs(getFilesDir().getAbsolutePath());
            long total = fs.getBlockCountLong() * fs.getBlockSizeLong();
            long free = fs.getAvailableBlocksLong() * fs.getBlockSizeLong();
            if (total <= 0) return new long[]{0, 0};
            long usedPct = Math.round(100.0 * (total - free) / total);
            long freeGb = Math.round(free / 1073741824.0);
            return new long[]{usedPct, freeGb};
        } catch (Exception e) {
            return new long[]{0, 0};
        }
    }

    private String readFirstLine(String path) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(path));
            return r.readLine();
        } catch (Exception e) {
            return null;
        } finally {
            try { if (r != null) r.close(); } catch (Exception ignored) { }
        }
    }

    // ---------------------------------------------------------------- decorations

    /** 1 px dot grid, the same texture as the web kit. */
    private static class DotGrid extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        DotGrid(Context ctx) {
            super(ctx);
            paint.setColor(0xFF1A2432);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float step = getResources().getDisplayMetrics().density * 26f;
            float r = Math.max(1f, getResources().getDisplayMetrics().density * 0.6f);
            for (float y = step; y < getHeight(); y += step) {
                for (float x = step; x < getWidth(); x += step) {
                    canvas.drawCircle(x, y, r, paint);
                }
            }
        }
    }

    /** Four rounded squares: the all-apps glyph. */
    private static class SquareGlyph extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        SquareGlyph(Context ctx) {
            super(ctx);
            paint.setColor(CYAN);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float d = getResources().getDisplayMetrics().density;
            float s = getWidth() * 0.42f;
            float gap = getWidth() * 0.16f;
            float r = 3f * d;
            for (int i = 0; i < 4; i++) {
                float x = (i % 2) * (s + gap);
                float y = (i / 2) * (s + gap);
                canvas.drawRoundRect(x, y, x + s, y + s, r, r, paint);
            }
        }
    }
}
