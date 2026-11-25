package com.example.nbatickethub.ui.arena;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.animation.ValueAnimator;
import com.example.nbatickethub.model.ArenaProfile;
import com.example.nbatickethub.model.ArenaTheme;
import com.example.nbatickethub.model.ArenaStands;
import com.example.nbatickethub.model.SeatStatus;
import com.example.nbatickethub.geometry.ArenaRenderStyle;
import com.example.nbatickethub.model.Seat;
import com.example.nbatickethub.model.SeatRow;
import com.example.nbatickethub.model.Section;
import com.example.nbatickethub.model.SeatGeometry;
import com.example.nbatickethub.R;

/** Canvas arena with bounded Matrix camera and cached, viewport-culled detail levels. */
public final class ArenaView extends View {
    public interface SeatInspectionListener {
        void onSeatInspected(Seat seat, SeatRow row, Section section);
    }

    private SeatInspectionListener inspectionListener;
    private final Path highlightPath = new Path();
    private final float[] touchWorld = new float[2];
    private int highlightedGeometry = -1;
    private final int highlightColor;
    private boolean tapEligible;
    private float downX, downY;
    private long downTime;
    private boolean hasDrawnScene;
    private float drawnScale, drawnX, drawnY;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF labelBounds = new RectF();
    private ArenaRenderData data;
    private ArenaTheme theme;
    private CourtRenderData courtData;
    private final SeatStatus[] statuses = SeatStatus.values();
    private static final float ROW_DETAIL_DP = 6, SEAT_DETAIL_DP = 16;
    private static final float LOD_HYSTERESIS = .15f, TARGET_SEAT_DP = 24;
    final ViewportTransform viewport = new ViewportTransform();
    private final RectF visible = new RectF();
    private int lod, firstPointer = -1, secondPointer = -1;
    private float lastX, lastY, lastSpan;
    private boolean dragging;
    private final int touchSlop;
    private ValueAnimator resetAnimator;
    // Frame diagnostics for device tests/profiling; no allocation or logging per frame.
    int drawnSections, drawnSeatBatches;
    long lastDrawNanos;
    private boolean slowDrawReported;

    public ArenaView(Context context) { this(context, null); }
    public ArenaView(Context context, AttributeSet attributes) {
        super(context, attributes);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        highlightColor = context.getColor(R.color.arena_hit_outline);
        setBackgroundColor(ArenaRenderStyle.BACKGROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setRenderData(ArenaRenderData value) {
        if (data == value) { return; }
        if (value != null) { value.validateStands(); setBackgroundColor(value.scene.profile.stands.background); }
        stopReset();
        cancelGesture();
        data = value;
        hasDrawnScene = false;
        inspectSeat(-1);
        lod = 0;
        slowDrawReported = false;
        updateFit(true);
        invalidate();
    }

    public String displayedArenaId() { return data == null ? null : data.arenaId(); }
    public String displayedThemeId() { return theme == null ? null : theme.id; }

    public void setSeatInspectionListener(SeatInspectionListener listener) { inspectionListener = listener; }

    public String highlightedSeatId() {
        return data == null || highlightedGeometry < 0 ? null : data.seatsByGeometry[highlightedGeometry].id;
    }

    private void inspectSeat(int geometryIndex) {
        highlightedGeometry = geometryIndex;
        highlightPath.reset();
        Seat seat = null;
        SeatRow row = null;
        Section section = null;
        if (data != null && geometryIndex >= 0) {
            SeatGeometry shape = data.scene.seats.get(geometryIndex);
            highlightPath.moveTo(shape.x(0), shape.y(0));
            for (int vertex = 1; vertex < 4; vertex++) { highlightPath.lineTo(shape.x(vertex), shape.y(vertex)); }
            highlightPath.close();
            seat = data.seatsByGeometry[geometryIndex];
            row = data.scene.dataset.rows.get(seat.rowId);
            section = data.scene.dataset.sections.get(row.sectionId);
        }
        if (inspectionListener != null) { inspectionListener.onSeatInspected(seat, row, section); }
        postInvalidateOnAnimation();
    }

    private boolean insideViewport(float x, float y) {
        return x >= getPaddingLeft() && x < getWidth() - getPaddingRight()
                && y >= getPaddingTop() && y < getHeight() - getPaddingBottom();
    }

    private boolean movedBeyondTapSlop(MotionEvent event, int pointerIndex) {
        if (Math.hypot(event.getX(pointerIndex) - downX, event.getY(pointerIndex) - downY) > touchSlop) {
            return true;
        }
        for (int history = 0; history < event.getHistorySize(); history++) {
            if (Math.hypot(event.getHistoricalX(pointerIndex, history) - downX,
                    event.getHistoricalY(pointerIndex, history) - downY) > touchSlop) { return true; }
        }
        return false;
    }

    private void inspectTap(float x, float y) {
        // lod describes the last drawn detail level: never query invisible seats at overview.
        if (!hasDrawnScene || lod != 2 || !insideViewport(x, y)
                || drawnScale != viewport.scale() || drawnX != viewport.translationX()
                || drawnY != viewport.translationY()) { return; }
        touchWorld[0] = x;
        touchWorld[1] = y;
        viewport.screenToWorld(touchWorld);
        inspectSeat(data.hitTester.find(touchWorld[0], touchWorld[1]));
    }

    /** Appearance-only update: reuses all scene coordinates and worker-built paths. */
    public void setArenaTheme(ArenaTheme value) {
        if (theme == value) { return; }
        try { setArenaTheme(value, new CourtRenderData(value, null)); }
        catch (java.io.IOException impossible) { throw new IllegalStateException(impossible); }
    }

    public void setArenaTheme(ArenaTheme value, CourtRenderData graphics) {
        if (value == null || graphics == null || graphics.theme != value) {
            throw new IllegalArgumentException("Matching prepared court required");
        }
        if (theme == value && courtData == graphics) { return; }
        theme = value;
        courtData = graphics;
        invalidate();
    }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        stopReset();
        cancelGesture();
        updateFit(false);
        hasDrawnScene = false;
    }

    private void updateFit(boolean reset) {
        if (data == null) { return; }
        com.example.nbatickethub.model.WorldRect bounds = data.scene.profile.geometry.displayBounds;
        viewport.configure(bounds.left, bounds.top, bounds.right, bounds.bottom,
                getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom(),
                TARGET_SEAT_DP * getResources().getDisplayMetrics().density / data.minSeatWidth, reset);
    }

    public void fitArena() {
        if (data == null || !viewport.isReady()) { return; }
        stopReset();
        cancelGesture();
        if (android.os.Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled()) {
            viewport.reset();
            postInvalidateOnAnimation();
            return;
        }
        float scale = viewport.scale(), x = viewport.translationX(), y = viewport.translationY();
        resetAnimator = ValueAnimator.ofFloat(0, 1);
        resetAnimator.setDuration(250);
        resetAnimator.addUpdateListener(animation -> {
            viewport.animateToFit((float) animation.getAnimatedValue(), scale, x, y);
            postInvalidateOnAnimation();
        });
        resetAnimator.start();
    }

    private void stopReset() {
        if (resetAnimator != null) {
            resetAnimator.cancel();
            resetAnimator.removeAllUpdateListeners();
            resetAnimator = null;
        }
    }

    private void cancelGesture() {
        firstPointer = secondPointer = -1;
        dragging = false;
        tapEligible = false;
        if (getParent() != null) { getParent().requestDisallowInterceptTouchEvent(false); }
    }

    /** Rebase IDs and midpoint whenever fingers change; pointer indices are never retained. */
    private void rebasePointers(MotionEvent event, int excludedIndex) {
        firstPointer = secondPointer = -1;
        for (int i = 0; i < event.getPointerCount(); i++) {
            if (i == excludedIndex) { continue; }
            if (firstPointer == -1) { firstPointer = event.getPointerId(i); }
            else if (secondPointer == -1) { secondPointer = event.getPointerId(i); }
        }
        int first = event.findPointerIndex(firstPointer);
        if (first < 0) { return; }
        lastX = event.getX(first);
        lastY = event.getY(first);
        if (secondPointer != -1) {
            int second = event.findPointerIndex(secondPointer);
            lastSpan = (float) Math.hypot(event.getX(second) - lastX, event.getY(second) - lastY);
            lastX = (lastX + event.getX(second)) / 2;
            lastY = (lastY + event.getY(second)) / 2;
            dragging = true;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (data == null || !viewport.isReady()) { return false; }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!insideViewport(event.getX(), event.getY())) { return false; }
                stopReset();
                dragging = false;
                tapEligible = true;
                downX = event.getX();
                downY = event.getY();
                downTime = event.getEventTime();
                rebasePointers(event, -1);
                if (getParent() != null) { getParent().requestDisallowInterceptTouchEvent(true); }
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                tapEligible = false;
                rebasePointers(event, -1);
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                tapEligible = false;
                rebasePointers(event, event.getActionIndex());
                return true;
            case MotionEvent.ACTION_MOVE:
                int first = event.findPointerIndex(firstPointer);
                if (first < 0) { return true; }
                float x = event.getX(first), y = event.getY(first);
                if (movedBeyondTapSlop(event, first)) { tapEligible = false; }
                int second = event.findPointerIndex(secondPointer);
                if (second >= 0) {
                    float span = (float) Math.hypot(event.getX(second) - x, event.getY(second) - y);
                    x = (x + event.getX(second)) / 2;
                    y = (y + event.getY(second)) / 2;
                    if (lastSpan > 0 && span > 0) { viewport.zoom(span / lastSpan, lastX, lastY, x, y); }
                    lastSpan = span;
                } else {
                    if (!dragging && Math.hypot(x - lastX, y - lastY) <= touchSlop) { return true; }
                    dragging = true;
                    viewport.pan(x - lastX, y - lastY);
                }
                lastX = x;
                lastY = y;
                postInvalidateOnAnimation();
                return true;
            case MotionEvent.ACTION_UP:
                if (tapEligible && !dragging && event.getPointerId(0) == firstPointer
                        && !movedBeyondTapSlop(event, 0)
                        && event.getEventTime() - downTime <= ViewConfiguration.getLongPressTimeout()) {
                    inspectTap(event.getX(), event.getY());
                    performClick();
                }
                cancelGesture();
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancelGesture();
                return true;
            default:
                return true;
        }
    }

    @Override public boolean performClick() { super.performClick(); return true; }

    @Override protected void onDetachedFromWindow() {
        stopReset();
        cancelGesture();
        super.onDetachedFromWindow();
    }

    private void updateLod() {
        float size = data.minSeatWidth * viewport.scale() / getResources().getDisplayMetrics().density;
        if (lod == 0 && size >= ROW_DETAIL_DP * (1 + LOD_HYSTERESIS)) { lod = 1; }
        if (lod == 1 && size >= SEAT_DETAIL_DP * (1 + LOD_HYSTERESIS)) { lod = 2; }
        if (lod == 2 && size < SEAT_DETAIL_DP * (1 - LOD_HYSTERESIS)) { lod = 1; }
        if (lod == 1 && size < ROW_DETAIL_DP * (1 - LOD_HYSTERESIS)) { lod = 0; }
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null || theme == null || !viewport.isReady()) { return; }
        ArenaStands stands = data.scene.profile.stands;
        long started = android.os.SystemClock.elapsedRealtimeNanos();
        android.os.Trace.beginSection("ArenaView.draw");
        try {
            int saved = canvas.save();
            canvas.clipRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
                    getHeight() - getPaddingBottom());
            viewport.concat(canvas);
            viewport.visibleWorld(visible);
            // Conservative world margin covers section strokes and screen-sized labels.
            float margin = 48 * getResources().getDisplayMetrics().scaledDensity / viewport.scale();
            visible.inset(-margin, -margin);
            updateLod();
            drawnSections = drawnSeatBatches = 0;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(stands.concourse);
            canvas.drawPath(data.envelope, paint);
            for (int i = 0; i < data.sectionPaths.length; i++) {
                if (!RectF.intersects(visible, data.sectionBounds[i])) { continue; }
                drawnSections++;
                paint.setColor(theme.seatTier(data.sectionLevelOrders[i]).section);
                canvas.drawPath(data.sectionPaths[i], paint);
            }
            paint.setStrokeWidth(ArenaRenderStyle.SECTION_STROKE);
            paint.setStyle(Paint.Style.STROKE);
            for (int i = 0; i < data.sectionPaths.length; i++) {
                if (!RectF.intersects(visible, data.sectionBounds[i])) { continue; }
                paint.setStrokeWidth(ArenaRenderStyle.SECTION_STROKE + 1);
                paint.setColor(stands.sectionBorder);
                canvas.drawPath(data.sectionPaths[i], paint);
                paint.setStrokeWidth(ArenaRenderStyle.SECTION_STROKE);
                paint.setColor(theme.seatTier(data.sectionLevelOrders[i]).accent);
                canvas.drawPath(data.sectionPaths[i], paint);
            }
            paint.setColor(stands.rowLine);
            paint.setStrokeWidth(ArenaRenderStyle.ROW_STROKE);
            if (lod >= 1) {
                for (int section = 0; section < data.rowLines.length; section++) {
                    if (RectF.intersects(visible, data.sectionBounds[section])) {
                        canvas.drawPath(data.rowLines[section], paint);
                    }
                }
            }
            paint.setStyle(Paint.Style.FILL);
            if (lod == 2) {
                for (int section = 0; section < data.sectionBatches.length; section++) {
                    if (!RectF.intersects(visible, data.sectionBounds[section])) { continue; }
                    for (int i : data.sectionBatches[section]) {
                        if (!RectF.intersects(visible, data.seatBounds[i])) { continue; }
                        drawnSeatBatches++;
                        paint.setColor(theme.seatTier(data.seatLevelOrders[i]).seatBackground);
                        canvas.drawPath(data.seatPaths[i], paint);
                        paint.setColor(0xffffffff);
                        canvas.drawPath(data.markerPaths[i], paint);
                        paint.setColor(theme.statuses.color(statuses[data.seatStatuses[i]]));
                        canvas.drawPath(data.statusDotPaths[i], paint);
                    }
                }
            }
            paint.setTextAlign(Paint.Align.CENTER);
            // Labels retain a readable screen size across camera scales.
            float textSize = ArenaRenderStyle.LABEL_SIZE
                    * getResources().getDisplayMetrics().scaledDensity / viewport.scale();
            paint.setTextSize(textSize);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            for (int i = 0; i < data.labels.length; i++) {
                float x = data.labelPoints[i * 2], y = data.labelPoints[i * 2 + 1];
                if (!visible.contains(x, y)) { continue; }
                float halfWidth = paint.measureText(data.labels[i]) / 2 + textSize * .25f;
                labelBounds.set(x - halfWidth, y - textSize * .65f, x + halfWidth, y + textSize * .65f);
                paint.setColor(stands.labelBackground);
                canvas.drawRoundRect(labelBounds, textSize * .25f, textSize * .25f, paint);
                paint.setColor(stands.labelText);
                canvas.drawText(data.labels[i], x, y + textSize * .34f, paint);
            }
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            drawCourt(canvas, data.scene.profile);
            if (lod == 2 && highlightedGeometry >= 0) {
                // Outline only: keep the original seat fill and ticket-status marker intact.
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5 * getResources().getDisplayMetrics().density / viewport.scale());
                paint.setColor(0xffffffff);
                canvas.drawPath(highlightPath, paint);
                paint.setStrokeWidth(2 * getResources().getDisplayMetrics().density / viewport.scale());
                paint.setColor(highlightColor);
                canvas.drawPath(highlightPath, paint);
                paint.setStyle(Paint.Style.FILL);
            }
            canvas.restoreToCount(saved);
            drawnScale = viewport.scale();
            drawnX = viewport.translationX();
            drawnY = viewport.translationY();
            hasDrawnScene = true;
        } finally {
            android.os.Trace.endSection();
            long elapsed = android.os.SystemClock.elapsedRealtimeNanos() - started;
            lastDrawNanos = elapsed;
            if (!slowDrawReported && elapsed > 32_000_000L) {
                slowDrawReported = true;
                android.util.Log.w("ArenaPipeline", "onDraw CPU >32ms: ns=" + elapsed
                        + " arena=" + data.arenaId() + " seats=" + data.seatCount()
                        + " batches=" + data.seatBatchCount() + " (does not measure GPU/FPS)");
            }
        }
    }

    private void drawCourt(Canvas canvas, ArenaProfile profile) {
        int saved = canvas.save();
        canvas.translate(profile.geometry.displayBounds.centerX(), profile.geometry.displayBounds.centerY());
        canvas.rotate(profile.geometry.courtAngle);
        canvas.scale(profile.geometry.courtLength / 94, profile.geometry.courtWidth / 50);
        canvas.translate(-47, -25);
        courtData.draw(canvas, paint);
        canvas.restoreToCount(saved);
    }
}
