package br.dev.projetor.home;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
import java.util.List;
import java.util.Locale;

public class AllAppsActivity extends Activity {

    private static class AppEntry {
        String label;
        Drawable icon;
        Intent intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("// TODOS OS APPS");

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
                row.setPadding(28, 16, 28, 16);

                ImageView img = new ImageView(AllAppsActivity.this);
                img.setImageDrawable(e.icon);
                row.addView(img, new LinearLayout.LayoutParams(dp(40), dp(40)));

                TextView text = new TextView(AllAppsActivity.this);
                text.setText(e.label);
                text.setTextColor(0xFFE8FBFF);
                text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
                text.setLetterSpacing(0.12f);
                text.setTypeface(Typeface.create("monospace", Typeface.BOLD));
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
        list.setBackgroundColor(0xFF05040F);
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

    private List<AppEntry> loadApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = pm.queryIntentActivities(main, 0);
        List<AppEntry> out = new ArrayList<AppEntry>();
        for (ResolveInfo ri : resolved) {
            if (ri.activityInfo == null) continue;
            if (getPackageName().equals(ri.activityInfo.packageName)) continue;
            AppEntry e = new AppEntry();
            e.label = String.valueOf(ri.loadLabel(pm));
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
        return out;
    }
}
