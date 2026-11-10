package br.dev.projetor.console;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AllAppsActivity extends Activity {

    private static final int BG = 0xFF06070C;
    private static final int PANEL = 0xFF0C1018;
    private static final int LINE = 0xFF1B2433;
    private static final int INK = 0xFFDDF6FF;
    private static final int DIM = 0xFF8B9FB3;
    private static final int MUTE = 0xFF66788C;
    private static final int CYAN = 0xFF22E8FF;

    private Typeface mono;
    private Typeface monoMedium;

    private static class AppEntry {
        String label;
        String pkg;
        Drawable icon;
        Intent intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Aplicativos");
        mono = Typeface.createFromAsset(getAssets(), "fonts/JetBrainsMono-Regular.ttf");
        monoMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);

        final List<AppEntry> apps = loadApps();
        BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() { return apps.size(); }

            @Override
            public Object getItem(int position) { return apps.get(position); }

            @Override
            public long getItemId(int position) { return position; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                AppEntry e = apps.get(position);
                LinearLayout row = new LinearLayout(AllAppsActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(24), dp(9), dp(24), dp(9));
                GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{PANEL, BG});
                bg.setCornerRadius(dp(3));
                bg.setStroke(dp(1), LINE);
                row.setBackground(bg);

                ImageView img = new ImageView(AllAppsActivity.this);
                img.setImageDrawable(e.icon);
                row.addView(img, new LinearLayout.LayoutParams(dp(40), dp(40)));

                TextView text = new TextView(AllAppsActivity.this);
                text.setText(e.label);
                text.setTextColor(INK);
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                text.setTypeface(mono);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.leftMargin = dp(20);
                row.addView(text, lp);
                return row;
            }

            private int dp(int v) {
                return Math.round(v * getResources().getDisplayMetrics().density);
            }
        };

        ListView list = new ListView(this);
        list.setBackgroundColor(BG);
        list.setDivider(null);
        list.setDividerHeight(dp(4));
        list.setPadding(dp(28), dp(6), dp(28), dp(6));
        list.setClipToPadding(false);
        list.setSelector(new android.graphics.drawable.ColorDrawable(0));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(24), dp(14), dp(24), dp(10));
        TextView brand = new TextView(this);
        brand.setText("TODOS OS APPS");
        brand.setTextColor(INK);
        brand.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        brand.setTypeface(monoMedium);
        brand.setLetterSpacing(0.8f);
        header.addView(brand);
        TextView sub = new TextView(this);
        sub.setText(apps.size() + " INSTALADOS · ESC PARA VOLTAR");
        sub.setTextColor(MUTE);
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        sub.setTypeface(mono);
        sub.setLetterSpacing(0.6f);
        header.addView(sub);
        View rule = new View(this);
        rule.setBackgroundColor(LINE);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        rlp.topMargin = dp(10);
        header.addView(rule, rlp);
        list.addHeaderView(header, null, false);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = apps.get(position).intent;
                try {
                    startActivity(i);
                } catch (Exception ignored) {
                }
            }
        });
        setContentView(list);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private List<AppEntry> loadApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(main, 0);
        List<AppEntry> out = new ArrayList<AppEntry>();
        for (ResolveInfo ri : resolved) {
            if (ri.activityInfo == null) continue;
            if (getPackageName().equals(ri.activityInfo.packageName)) continue;
            // frozen legacy launcher (system-signed, cannot be updated): never list it
            if ("br.dev.projetor.home".equals(ri.activityInfo.packageName)) continue;
            AppEntry e = new AppEntry();
            e.label = String.valueOf(ri.loadLabel(pm));
            e.pkg = ri.activityInfo.packageName;
            e.icon = ri.loadIcon(pm);
            e.intent = new Intent(Intent.ACTION_MAIN);
            e.intent.addCategory(Intent.CATEGORY_LAUNCHER);
            e.intent.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
            e.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            out.add(e);
        }
        final Collator collator = Collator.getInstance(new Locale("pt", "BR"));
        Collections.sort(out, new Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return collator.compare(a.label, b.label);
            }
        });
        // The ROM ships two activities labelled "Configurações": the stock
        // com.android.tv.settings and the vendor com.htc.htcsettings (where the projector
        // settings live). Collapse same-label entries, keeping the vendor one.
        List<AppEntry> unique = new ArrayList<AppEntry>();
        Map<String, Integer> seen = new HashMap<String, Integer>();
        for (AppEntry e : out) {
            String key = e.label.toLowerCase(Locale.getDefault());
            Integer at = seen.get(key);
            if (at == null) {
                seen.put(key, unique.size());
                unique.add(e);
            } else if (unique.get(at).pkg.startsWith("com.android.")
                    && !e.pkg.startsWith("com.android.")) {
                unique.set(at, e);
            }
        }
        return unique;
    }
}
