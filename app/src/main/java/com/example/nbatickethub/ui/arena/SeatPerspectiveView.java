package com.example.nbatickethub.ui.arena;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.example.nbatickethub.geometry.SeatPerspective;

/** Reuses the prepared court artwork with a four-point perspective Matrix. No images/I/O. */
public final class SeatPerspectiveView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix courtMatrix = new Matrix();
    private final float[] source = {0, 0, 94, 0, 94, 50, 0, 50};
    private final float[] destination = new float[8];
    private float[] normalized;
    private SeatPerspective perspective;
    private final Path stands = new Path(), baskets = new Path();
    private final float[] projected = new float[2];
    private CourtRenderData court;
    private int accent = 0xff1656a5;
    private boolean valid;
    private float frameWidth, frameHeight, frameLeft, frameTop;

    public SeatPerspectiveView(Context context) { this(context, null); }
    public SeatPerspectiveView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void setPerspective(SeatPerspective perspective, CourtRenderData prepared) {
        this.perspective = perspective;
        normalized = perspective == null ? null : perspective.courtQuad();
        court = prepared;
        if (prepared != null) { accent = prepared.theme.primaryColor; }
        rebuild();
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { rebuild(); }
    private void rebuild() {
        valid = normalized != null && court != null && getWidth() > 0 && getHeight() > 0;
        if (!valid) { return; }
        frameWidth = Math.min(getWidth(), getHeight() * 16f / 9);
        frameHeight = frameWidth * 9f / 16;
        frameLeft = (getWidth() - frameWidth) / 2;
        frameTop = (getHeight() - frameHeight) / 2;
        for (int i = 0; i < 4; i++) {
            destination[2 * i] = frameLeft + normalized[2 * i] * frameWidth;
            destination[2 * i + 1] = frameTop + normalized[2 * i + 1] * frameHeight;
        }
        valid = courtMatrix.setPolyToPoly(source, 0, destination, 0, 4);
        stands.reset(); baskets.reset();
        // Opposite bowl, physically projected from court-space feet; skip the camera side.
        for (int row = 0; row < 24; row++) {
            boolean started = false;
            for (int step = 0; step <= 120; step++) {
                double angle = step * Math.PI * 2 / 120;
                float x = (float) (47 + (63 + row * 1.8) * Math.cos(angle));
                float y = (float) (25 + (39 + row * 1.5) * Math.sin(angle));
                if ((x - 47) * perspective.eyeX + (y - 25) * perspective.eyeY > 0) { started = false; continue; }
                perspective.project3d(x, y, row * 1.1f + 2, projected, 0);
                float px = frameLeft + projected[0] * frameWidth, py = frameTop + projected[1] * frameHeight;
                if (!started) { stands.moveTo(px, py); started = true; } else { stands.lineTo(px, py); }
            }
        }
        for (float x : new float[]{4, 90}) {
            line(baskets, x, 25, 0, x, 25, 10);
            line(baskets, x, 22, 12, x, 28, 12);
            line(baskets, x, 22, 10, x, 28, 10);
            line(baskets, x, 22, 10, x, 22, 12);
            line(baskets, x, 28, 10, x, 28, 12);
            for (int i = 0; i < 20; i++) {
                double a = i * Math.PI / 10, b = (i + 1) * Math.PI / 10;
                float rimX = x == 4 ? 5.3f : 88.7f;
                line(baskets, rimX + (float)Math.cos(a)*.75f, 25+(float)Math.sin(a)*.75f, 10,
                        rimX+(float)Math.cos(b)*.75f, 25+(float)Math.sin(b)*.75f, 10);
            }
        }
    }

    private void line(Path path, float x, float y, float z, float a, float b, float c) {
        perspective.project3d(x,y,z,projected,0); path.moveTo(frameLeft+projected[0]*frameWidth, frameTop+projected[1]*frameHeight);
        perspective.project3d(a,b,c,projected,0); path.lineTo(frameLeft+projected[0]*frameWidth, frameTop+projected[1]*frameHeight);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int clip = canvas.save();
        if (valid) canvas.clipRect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawColor(0xff10283f);
        paint.setColor(0xff233f56);
        canvas.drawOval(-getWidth() * .2f, getHeight() * .12f, getWidth() * 1.2f, getHeight() * .95f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2, getHeight() * .035f));
        paint.setColor(accent);
        canvas.drawOval(-getWidth() * .15f, getHeight() * .2f, getWidth() * 1.15f, getHeight() * 1.1f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(Math.max(1, getHeight() * .006f));
        paint.setColor(0xff64748b); paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(stands, paint); paint.setStyle(Paint.Style.FILL);
        if (valid) {
            int saved = canvas.save();
            canvas.concat(courtMatrix);
            court.draw(canvas, paint);
            canvas.restoreToCount(saved);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.max(1.5f, getHeight() * .006f));
            paint.setColor(0xfff0e8da); canvas.drawPath(baskets, paint); paint.setStyle(Paint.Style.FILL);
        }
        paint.setColor(0xff071b2b);
        canvas.drawRect(frameLeft, frameTop + frameHeight * .95f, frameLeft + frameWidth, frameTop + frameHeight, paint);
        canvas.restoreToCount(clip);
    }
}
