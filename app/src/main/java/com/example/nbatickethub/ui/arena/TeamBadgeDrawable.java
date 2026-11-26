package com.example.nbatickethub.ui.arena;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Reuses a prepared local logo; an explicit team-code badge is the asset fallback. */
public final class TeamBadgeDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF imageBounds = new RectF();
    private final Bitmap logo;
    private final String code;
    private final int color, size;
    public TeamBadgeDrawable(Bitmap logo, String code, int color, int size) {
        this.logo = logo; this.code = code; this.color = color; this.size = size;
    }
    @Override public int getIntrinsicWidth() { return size; }
    @Override public int getIntrinsicHeight() { return size; }
    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        imageBounds.set(bounds); paint.setColor(Color.WHITE);
        canvas.drawOval(imageBounds, paint);
        if (logo != null) {
            float scale = Math.min(bounds.width() * .8f / logo.getWidth(), bounds.height() * .8f / logo.getHeight());
            float w = logo.getWidth() * scale, h = logo.getHeight() * scale;
            imageBounds.set(bounds.exactCenterX() - w / 2, bounds.exactCenterY() - h / 2,
                    bounds.exactCenterX() + w / 2, bounds.exactCenterY() + h / 2);
            canvas.drawBitmap(logo, null, imageBounds, paint);
        } else {
            paint.setColor(color); paint.setTypeface(Typeface.DEFAULT_BOLD); paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(bounds.height() * .28f);
            canvas.drawText(code, bounds.exactCenterX(), bounds.exactCenterY() - (paint.ascent() + paint.descent()) / 2, paint);
        }
    }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
