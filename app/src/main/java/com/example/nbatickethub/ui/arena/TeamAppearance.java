package com.example.nbatickethub.ui.arena;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import com.example.nbatickethub.model.ArenaTheme;

/** Shared mobile identity system; authored styles, not official team typography. */
public final class TeamAppearance {
    private TeamAppearance() { }

    public static Typeface heading(String teamId) {
        String family;
        switch (teamId) {
            case "bos": case "nyk": case "lal": case "phi": case "sas":
                family = "serif"; break;
            case "bkn": case "chi": case "mia": case "tor": case "lac":
                family = "sans-serif-black"; break;
            default: family = "sans-serif-condensed";
        }
        return Typeface.create(family, Typeface.BOLD);
    }

    public static Drawable background(ArenaTheme theme, boolean light) {
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final int base = light ? androidx.core.graphics.ColorUtils.blendARGB(theme.primaryColor, Color.WHITE, .95f) : theme.primaryColor;
            @Override public void draw(Canvas canvas) {
                paint.setStyle(Paint.Style.FILL); paint.setColor(base); paint.setAlpha(255);
                canvas.drawRect(getBounds(), paint);
                paint.setColor(light ? theme.primaryColor : Color.WHITE);
                paint.setAlpha(light ? 12 : 18); paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(Math.max(1, getBounds().width() / 250f));
                float step = Math.max(28, getBounds().width() / 8f);
                for (float x = -getBounds().height(); x < getBounds().width(); x += step) {
                    canvas.drawLine(x, 0, x + getBounds().height(), getBounds().height(), paint);
                }
                paint.setStrokeWidth(step * .35f);
                canvas.drawCircle(getBounds().width(), 0, step * 2, paint);
            }
            @Override public void setAlpha(int alpha) { }
            @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
            @Override public int getOpacity() { return PixelFormat.OPAQUE; }
        };
    }
}
