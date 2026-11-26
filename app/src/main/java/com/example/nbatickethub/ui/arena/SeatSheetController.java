package com.example.nbatickethub.ui.arena;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.example.nbatickethub.R;
import com.example.nbatickethub.model.*;
import com.example.nbatickethub.state.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

/** Section browsing and ticket preview share one scroll surface; confirmation is explicit. */
public final class SeatSheetController {
    private final Activity activity;
    private final ArenaSelectionViewModel model;
    private final View sheet, stage, header;
    private final BottomSheetBehavior<View> behavior;
    private final ListView list;
    private final LinearLayout body;
    private final TicketAdapter adapter = new TicketAdapter();
    private final SeatPerspectiveView preview;
    private ArenaRenderData data;
    private CourtRenderData court;
    private String lastPreviewId, scrollKey;
    private Dialog largePreview;
    private boolean restoring;

    public SeatSheetController(Activity activity, ArenaSelectionViewModel model, ArenaView arena) {
        this.activity = activity; this.model = model;
        sheet = activity.findViewById(R.id.seat_sheet); stage = activity.findViewById(R.id.arena_stage);
        behavior = BottomSheetBehavior.from(sheet); behavior.setPeekHeight(dp(64));
        list = activity.findViewById(R.id.selected_list);
        body = activity.findViewById(R.id.ticket_body);
        header = activity.getLayoutInflater().inflate(R.layout.section_sheet_header, list, false);
        preview = header.findViewById(R.id.seat_perspective);
        body.addView(header, 0); list.setAdapter(adapter);
        stage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            int width = r-l, height = b-t;
            if (width <= 0 || height <= 0) { return; }
            boolean landscape = width > height * 1.3f;
            ViewGroup.LayoutParams params = sheet.getLayoutParams();
            int w = landscape ? Math.min(dp(680), Math.round(width * .72f)) : -1;
            int h = landscape ? height - dp(12) : Math.min(dp(540), Math.round(height * .88f));
            if (params.width != w || params.height != h) { params.width = w; params.height = h; sheet.setLayoutParams(params); }
            ((View) header.findViewById(R.id.selection_outside).getParent()).setVisibility(landscape ? View.GONE : View.VISIBLE);
            header.findViewById(R.id.seat_details).setVisibility(landscape || model.selection.candidateId() == null ? View.GONE : View.VISIBLE);
            ViewGroup.LayoutParams image = preview.getLayoutParams();
            LinearLayout panel = (LinearLayout) sheet;
            ViewGroup.LayoutParams summary = panel.getChildAt(0).getLayoutParams();
            ViewGroup.LayoutParams actions = panel.getChildAt(2).getLayoutParams();
            int summaryHeight = dp(landscape ? 48 : 64), actionsHeight = dp(landscape ? 48 : 56);
            if (summary.height != summaryHeight) { summary.height = summaryHeight; panel.getChildAt(0).setLayoutParams(summary); }
            if (actions.height != actionsHeight) { actions.height = actionsHeight; panel.getChildAt(2).setLayoutParams(actions); }
            body.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
            int previewWidth = landscape ? Math.min(Math.round(w * .44f), Math.max(dp(100), Math.round((h - summaryHeight - actionsHeight - dp(24)) * 16f / 9))) : width;
            LinearLayout.LayoutParams headerParams = (LinearLayout.LayoutParams) header.getLayoutParams();
            int headerWidth = landscape ? previewWidth : -1, headerHeight = landscape ? -1 : -2;
            if (headerParams.width != headerWidth || headerParams.height != headerHeight) {
                headerParams.width = headerWidth; headerParams.height = headerHeight; header.setLayoutParams(headerParams);
            }
            LinearLayout.LayoutParams listParams = (LinearLayout.LayoutParams) list.getLayoutParams();
            int listWidth = landscape ? 0 : -1, listHeight = landscape ? -1 : 0;
            if (listParams.width != listWidth || listParams.height != listHeight) {
                listParams.width = listWidth; listParams.height = listHeight; listParams.weight = 1; list.setLayoutParams(listParams);
            }
            int imageHeight = Math.round(previewWidth * 9f / 16);
            if (image.height != imageHeight) { image.height = imageHeight; preview.setLayoutParams(image); }
        });
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override public void onSlide(View view, float offset) { }
            @Override public void onStateChanged(View view, int state) {
                if (state == BottomSheetBehavior.STATE_EXPANDED || state == BottomSheetBehavior.STATE_COLLAPSED) {
                    model.sheetExpanded = state == BottomSheetBehavior.STATE_EXPANDED;
                    ((Button) activity.findViewById(R.id.toggle_sheet)).setText(model.sheetExpanded ? R.string.collapse_details : R.string.details);
                }
            }
        });
        activity.findViewById(R.id.toggle_sheet).setOnClickListener(v -> { if (model.sheetExpanded) collapse(); else expand(); });
        activity.findViewById(R.id.change_seat).setOnClickListener(v -> { model.selection.beginReplace(); model.changed(); expand(); });
        activity.findViewById(R.id.cancel_change).setOnClickListener(v -> { model.selection.cancelReplace(); model.changed(); });
        activity.findViewById(R.id.toggle_seat).setOnClickListener(v -> {
            if (data == null) { return; }
            ArenaSelection.Result result = model.selection.confirm();
            if (result == ArenaSelection.Result.UNAVAILABLE) { message(R.string.blocked_seat); }
            model.changed();
        });
        header.findViewById(R.id.clear_selection).setOnClickListener(v -> { model.selection.clearSelection(); model.changed(); });
        preview.setOnClickListener(v -> openLargePreview());
        header.findViewById(R.id.fullscreen_view).setOnClickListener(v -> openLargePreview());
        list.setOnItemClickListener((parent, view, position, id) -> {
            int index = position - list.getHeaderViewsCount();
            if (index < 0 || index >= adapter.ids.size()) { return; }
            model.selection.preview(adapter.ids.get(index)); model.changed();
        });
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView view, int state) { }
            @Override public void onScroll(AbsListView view, int first, int visible, int total) {
                if (!restoring && scrollKey != null && list.getChildCount() > 0) {
                    model.ticketScroll.put(scrollKey, new int[]{first, list.getChildAt(0).getTop()});
                }
            }
        });
    }
    public void setData(ArenaRenderData value, CourtRenderData graphics) {
        data = value; court = graphics; lastPreviewId = null;
        if (value == null) {
            scrollKey = null;
            adapter.ids = Collections.emptyList(); adapter.notifyDataSetChanged();
            preview.setPerspective(null, null);
            sheet.setVisibility(View.INVISIBLE); return;
        }
        boolean expanded = model.sheetExpanded;
        render(); sheet.post(() -> { if (expanded) expand(); else collapse(); });
    }
    public void setAccent(int color, int ink) {
        for (int id : new int[]{R.id.change_seat, R.id.toggle_seat}) {
            Button button = activity.findViewById(id);
            button.setBackgroundTintList(ColorStateList.valueOf(color)); button.setTextColor(ink);
        }
    }
    public void expand() { if (data == null || model.selection.browsingSection() == null) return; sheet.setVisibility(View.VISIBLE); behavior.setState(BottomSheetBehavior.STATE_EXPANDED); model.sheetExpanded = true; }
    public void collapse() { behavior.setState(BottomSheetBehavior.STATE_COLLAPSED); model.sheetExpanded = false; }
    public void message(int resource) { Snackbar.make(stage, resource, Snackbar.LENGTH_SHORT).show(); }

    public void render() {
        if (data == null) { return; }
        ArenaSelection state = model.selection;
        String sectionId = state.browsingSection();
        sheet.setVisibility(sectionId == null ? View.INVISIBLE : View.VISIBLE);
        if (sectionId == null) { return; }
        Section section = data.dataset().sections.get(sectionId);
        List<String> ids = state.tickets();
        long minimum = Long.MAX_VALUE;
        for (String id : ids) { if (state.available(id)) minimum = Math.min(minimum, state.offer(id).price.amountMinor); }
        String title = minimum == Long.MAX_VALUE ? activity.getString(R.string.section_no_tickets, section.code)
                : activity.getString(R.string.section_from, section.code, money(minimum));
        ((TextView) activity.findViewById(R.id.selection_summary)).setText(state.selectedIds().isEmpty() ? title
                : activity.getString(R.string.section_selection_summary, title,
                activity.getString(R.string.ticket_selected_summary, state.selectedIds().size(), money(state.totalMinor()))));
        ((TextView) header.findViewById(R.id.selection_outside)).setText(activity.getString(R.string.ticket_selected_summary, state.selectedIds().size(), money(state.totalMinor())));
        activity.findViewById(R.id.cancel_change).setVisibility(state.replacingId() == null ? View.GONE : View.VISIBLE);
        activity.findViewById(R.id.change_seat).setEnabled(state.focusedId() != null && state.replacingId() == null && state.selectedIds().contains(state.focusedId()));
        String candidate = state.candidateId();
        boolean selected = candidate != null && state.selectedIds().contains(candidate);
        Button confirm = activity.findViewById(R.id.toggle_seat);
        confirm.setEnabled(candidate != null && state.available(candidate) && state.matches(candidate) && !selected);
        confirm.setText(selected ? R.string.status_selected : R.string.confirm_seat);
        confirm.setVisibility(candidate == null ? View.INVISIBLE : View.VISIBLE);
        confirm.setAlpha(confirm.isEnabled() ? 1 : .45f);
        View change = activity.findViewById(R.id.change_seat);
        change.setVisibility(state.selectedIds().isEmpty() ? View.INVISIBLE : View.VISIBLE);
        change.setAlpha(change.isEnabled() ? 1 : .45f);
        String representative = state.representative();
        if (representative == null) {
            for (Seat seat : data.dataset().seatOrder) {
                if (sectionId.equals(data.dataset().rows.get(seat.rowId).sectionId)) { representative = seat.id; break; }
            }
        }
        header.findViewById(R.id.seat_details).setVisibility(candidate == null || stage.getWidth() > stage.getHeight() * 1.3f ? View.GONE : View.VISIBLE);
        ((TextView) header.findViewById(R.id.seat_details)).setText(candidate == null ? title : describe(candidate) + " · " + money(state.offer(candidate).price.amountMinor));
        if (!Objects.equals(lastPreviewId, representative)) {
            preview.setPerspective(representative == null ? null : data.perspective(representative), court); lastPreviewId = representative;
        }
        preview.setEnabled(representative != null);
        String key = data.arenaId() + ":" + sectionId + ":" + state.category() + ":" + state.budget();
        boolean newSection = !key.equals(scrollKey);
        if (!adapter.ids.equals(ids)) { adapter.ids = ids; adapter.notifyDataSetChanged(); }
        else { adapter.notifyDataSetChanged(); }
        if (newSection) {
            scrollKey = key; restoring = true;
            int[] saved = model.ticketScroll.get(key);
            list.setSelectionFromTop(saved == null ? 0 : saved[0], saved == null ? 0 : saved[1]);
            list.post(() -> restoring = false);
        }
    }
    public void openLargePreview() {
        if (data == null || model.selection.representative() == null) { return; }
        largePreview = new Dialog(activity, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        LinearLayout content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL); content.setBackgroundColor(0xff10283f);
        Button close = new Button(activity); close.setText(R.string.fullscreen_close); close.setOnClickListener(v -> largePreview.dismiss()); content.addView(close);
        TextView label = new TextView(activity); label.setText(activity.getString(R.string.view_context, activity.getString(R.string.simulated_view_title), describe(model.selection.representative()))); label.setTextColor(0xffffffff); content.addView(label);
        SeatPerspectiveView large = new SeatPerspectiveView(activity); large.setPerspective(data.perspective(model.selection.representative()), court);
        content.addView(large, new LinearLayout.LayoutParams(-1, 0, 1)); largePreview.setContentView(content); largePreview.show();
    }
    public void close() { if (largePreview != null) largePreview.dismiss(); }
    private int dp(float value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private String describe(String id) {
        Seat seat = data.dataset().seats.get(id); SeatRow row = data.dataset().rows.get(seat.rowId);
        return activity.getString(R.string.selection_row, data.dataset().sections.get(row.sectionId).code, row.rowLabel, seat.positionNumber);
    }
    private String money(long minor) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.getDefault());
        Currency currency = Currency.getInstance(model.selection.currency()); format.setCurrency(currency);
        return format.format(BigDecimal.valueOf(minor, currency.getDefaultFractionDigits()));
    }
    private final class TicketAdapter extends BaseAdapter {
        private List<String> ids = Collections.emptyList();
        @Override public int getCount() { return ids.size(); }
        @Override public String getItem(int position) { return ids.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View recycled, ViewGroup parent) {
            View row = recycled == null ? activity.getLayoutInflater().inflate(R.layout.selection_item, parent, false) : recycled;
            String id = getItem(position); ArenaSelection state = model.selection;
            Seat seat = data.dataset().seats.get(id);
            Section section = data.dataset().sections.get(data.dataset().rows.get(seat.rowId).sectionId);
            String status = activity.getString(state.available(id) ? R.string.available_plain : state.offer(id).baseStatus == SeatStatus.BOOKED ? R.string.booked_plain : R.string.locked_plain);
            TextView text = row.findViewById(R.id.selection_item_text);
            text.setText(activity.getString(R.string.ticket_line, describe(id), activity.getString(state.ticketCategory(id) == Section.Category.VIP ? R.string.seat_type_vip : state.ticketCategory(id) == Section.Category.STANDING ? R.string.seat_type_standing : R.string.seat_type_standard), money(state.offer(id).price.amountMinor), status));
            String rowText = text.getText().toString();
            String price = money(state.offer(id).price.amountMinor);
            int start = rowText.indexOf(price);
            android.text.SpannableString emphasis = new android.text.SpannableString(rowText);
            if (start >= 0) {
                emphasis.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, start + price.length(), 0);
                emphasis.setSpan(new android.text.style.ForegroundColorSpan(0xff005b48), start, start + price.length(), 0);
            }
            text.setText(emphasis);
            text.setTextSize(15); text.setTextColor(0xff10283f);
            row.setBackgroundColor(id.equals(state.candidateId()) ? 0xffdeedff : 0xffffffff);
            row.setAlpha(state.available(id) ? 1f : .55f);
            Button remove = row.findViewById(R.id.selection_item_remove);
            remove.setVisibility(state.selectedIds().contains(id) ? View.VISIBLE : View.GONE);
            remove.setOnClickListener(v -> { state.remove(id); model.changed(); });
            return row;
        }
    }
}
