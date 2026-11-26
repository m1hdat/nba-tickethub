package com.example.nbatickethub.ui.arena;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import com.example.nbatickethub.data.AssetSource;
import com.example.nbatickethub.geometry.CourtMarkings;
import com.example.nbatickethub.model.ArenaTheme;
import com.example.nbatickethub.model.CourtDesign;
import java.io.IOException;
import java.io.InputStream;

/** Reusable court paths and optional local logo, prepared outside onDraw. */
public final class CourtRenderData {
    public final ArenaTheme theme;
    private final Path[] artwork;
    private final Path lines = new Path(), rims = new Path();
    private final Bitmap logo;
    public Bitmap logoBitmap() { return logo; }
    private final RectF logoBounds;

    public CourtRenderData(ArenaTheme theme, AssetSource assets) throws IOException {
        this.theme = theme;
        artwork = new Path[theme.court.design.primitives.size()];
        for (int i = 0; i < artwork.length; i++) {
            artwork[i] = path(theme.court.design.primitives.get(i));
        }
        for (CourtDesign.Primitive p : CourtMarkings.ALL) {
            (p.part.equals("key") ? rims : lines).addPath(path(p));
        }
        Bitmap image = null;
        CourtDesign.Logo mark = theme.court.design.logo;
        if (mark.asset != null && assets != null) {
            try {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                try (InputStream input = assets.open(mark.asset)) { BitmapFactory.decodeStream(input, null, bounds); }
                if (bounds.outWidth > 0 && bounds.outHeight > 0
                        && (long) bounds.outWidth * bounds.outHeight <= 2_000_000) {
                    try (InputStream input = assets.open(mark.asset)) { image = BitmapFactory.decodeStream(input); }
                }
            } catch (IOException | IllegalArgumentException error) {
                android.util.Log.w("ArenaCourt", "Local logo unavailable; using team code: " + theme.teamId);
            }
        }
        logo = image;
        float[] box = mark.fit(image == null ? 1 : image.getWidth(), image == null ? 1 : image.getHeight());
        logoBounds = new RectF(box[0], box[1], box[2], box[3]);
    }

    private static Path path(CourtDesign.Primitive p) {
        float[] points = p.copyPoints();
        Path result = new Path();
        if (p.type.equals("ellipse")) {
            result.addOval(new RectF(points[0], points[1], points[2], points[3]), Path.Direction.CW);
        } else {
            result.moveTo(points[0], points[1]);
            for (int i = 2; i < points.length; i += 2) { result.lineTo(points[i], points[i + 1]); }
            if (p.type.equals("polygon")) { result.close(); }
        }
        return result;
    }

    private void drawPart(Canvas canvas, Paint paint, String part) {
        for (int i = 0; i < artwork.length; i++) {
            CourtDesign.Primitive p = theme.court.design.primitives.get(i);
            if (!p.part.equals(part)) { continue; }
            paint.setColor(p.color);
            paint.setStyle(p.strokeWidth == 0 ? Paint.Style.FILL : Paint.Style.STROKE);
            paint.setStrokeWidth(p.strokeWidth);
            canvas.drawPath(artwork[i], paint);
        }
    }

    /** All team graphics and the center mark are below the final competition-line passes. */
    public void draw(Canvas canvas, Paint paint) {
        ArenaTheme.Court c = theme.court;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(c.apron); canvas.drawRect(-4, -4, 98, 54, paint);
        drawPart(canvas, paint, "border");
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(c.wood); canvas.drawRect(0, 0, 94, 50, paint);
        int clip = canvas.save(); canvas.clipRect(0, 0, 94, 50);
        drawPart(canvas, paint, "wood"); drawPart(canvas, paint, "decor");
        paint.setStyle(Paint.Style.FILL); paint.setColor(c.paint);
        canvas.drawRect(0, 17, 19, 33, paint); canvas.drawRect(75, 17, 94, 33, paint);
        drawPart(canvas, paint, "key");
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER);
        if (logo != null) {
            paint.setFilterBitmap(true); canvas.drawBitmap(logo, null, logoBounds, paint);
        } else {
            paint.setTypeface(Typeface.DEFAULT_BOLD); paint.setColor(c.center);
            paint.setTextSize(Math.min(c.design.logo.width / 3.3f, c.design.logo.height / 2.5f));
            canvas.drawText(c.design.logo.fallback, 47, 25 - (paint.ascent() + paint.descent()) / 2, paint);
        }
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(.7f); paint.setColor(0xff273a48);
        canvas.drawPath(lines, paint);
        paint.setStrokeWidth(.34f); paint.setColor(c.lines); canvas.drawPath(lines, paint);
        paint.setColor(0xffe46b35); canvas.drawPath(rims, paint);
        canvas.restoreToCount(clip);
        paint.setStyle(Paint.Style.FILL); paint.setColor(c.lettering); paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(2.1f); paint.setTextAlign(Paint.Align.CENTER);
        for (int side = 0; side < 2; side++) {
            int save = canvas.save(); canvas.translate(side == 0 ? -2 : 96, 25);
            canvas.rotate(side == 0 ? -90 : 90); canvas.drawText(c.baselineText, 0, .7f, paint);
            canvas.restoreToCount(save);
        }
        paint.setTypeface(Typeface.DEFAULT);
    }
}
