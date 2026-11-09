package br.dev.projetor.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.View;

/** Static neon grid + vignette background. Draws once, no animation (cheap for 1 GB RAM). */
public class CyberBackground extends View {

    private final Paint bg = new Paint();
    private final Paint grid = new Paint();
    private final Paint bright = new Paint();
    private final Paint vignette = new Paint();
    private int step;

    public CyberBackground(Context c) {
        super(c);
        step = Math.round(44 * c.getResources().getDisplayMetrics().density);
        bg.setColor(0xFF04030A);
        grid.setColor(0x1800F0FF);
        grid.setStrokeWidth(1f);
        bright.setColor(0x5514FFE0);
        bright.setStrokeWidth(2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth(), h = getHeight();
        canvas.drawRect(0, 0, w, h, bg);

        for (int x = 0; x < w; x += step) canvas.drawLine(x, 0, x, h, grid);
        for (int y = 0; y < h; y += step) canvas.drawLine(0, y, w, y, grid);

        // horizon accent
        canvas.drawLine(0, h * 0.16f, w, h * 0.16f, bright);

        vignette.setShader(new RadialGradient(w * 0.5f, h * 0.55f, h * 1.1f,
                new int[]{0x00000000, 0x00000000, 0xCC04030A, 0xFF000000},
                new float[]{0f, 0.45f, 0.85f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, vignette);

        // magenta corner glow
        Paint g = new Paint();
        g.setShader(new RadialGradient(w, 0, h * 0.8f,
                new int[]{0x33FF2E88, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, g);

        Paint c = new Paint();
        c.setShader(new RadialGradient(0, h, h * 0.9f,
                new int[]{0x2200F0FF, 0x00000000}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, c);
    }
}
