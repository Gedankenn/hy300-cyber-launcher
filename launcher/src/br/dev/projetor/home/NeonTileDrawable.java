package br.dev.projetor.home;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Angled "dog tag" tile with neon stroke; glows when focused. */
public class NeonTileDrawable extends Drawable {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glare = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final boolean focused;
    private final int cut;

    public NeonTileDrawable(boolean focused, int cutPx) {
        this.focused = focused;
        this.cut = cutPx;
        fill.setStyle(Paint.Style.FILL);
        fill.setColor(focused ? 0x2A00F0FF : 0xD9050410);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(focused ? 3f : 1.6f);
        stroke.setColor(focused ? 0xFFFF2E88 : 0x6600F0FF);
        if (focused) {
            stroke.setShadowLayer(cutPx * 0.55f, 0, 0, 0xFFFF2E88);
            glare.setStyle(Paint.Style.FILL);
            glare.setShader(null);
            glare.setColor(0x1400F0FF);
        }
    }

    @Override
    protected void onBoundsChange(Rect b) {
        path.reset();
        float l = b.left + 1, t = b.top + 1, r = b.right - 1, bo = b.bottom - 1;
        path.moveTo(l + cut, t);
        path.lineTo(r - cut, t);
        path.lineTo(r, t + cut);
        path.lineTo(r, bo - cut);
        path.lineTo(r - cut, bo);
        path.lineTo(l + cut, bo);
        path.lineTo(l, bo - cut);
        path.lineTo(l, t + cut);
        path.close();
    }

    @Override
    public void draw(Canvas c) {
        c.drawPath(path, fill);
        if (focused) c.drawPath(path, glare);
        c.drawPath(path, stroke);
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
