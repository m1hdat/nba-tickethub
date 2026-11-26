package com.example.nbatickethub.ui.arena;

import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.nbatickethub.R;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.model.*;
import com.example.nbatickethub.state.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;

/** Compact native bottom panel. Search and SQLite revalidation run off the UI thread. */
public final class SeatFinderController implements AutoCloseable {
    private static final float PANEL_HEIGHT_FRACTION = .42f;
    private final AppCompatActivity activity;
    private final ArenaSelectionViewModel model;
    private final ArenaView view;
    private final SeatSheetController sheet;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private Dialog panel;
    private int generation;
    private boolean closed;
    private Runnable saveDraft;
    public void saveState() { if (saveDraft != null && panel != null && panel.isShowing()) saveDraft.run(); }
    private int accent = 0xff102c50;
    public void setAccent(int value) { accent = value; }

    public SeatFinderController(AppCompatActivity activity, ArenaSelectionViewModel model,
                                ArenaView view, SeatSheetController sheet) {
        this.activity = activity; this.model = model; this.view = view; this.sheet = sheet;
        activity.findViewById(R.id.suggest_seats).setOnClickListener(v -> show());
    }
    private int dp(int n) { return Math.round(n * activity.getResources().getDisplayMetrics().density); }
    private TextView text(LinearLayout parent, String value) {
        TextView label = new TextView(activity); label.setText(value); label.setTextColor(0xff10283f);
        label.setPadding(dp(8), dp(6), dp(8), dp(6)); parent.addView(label); return label;
    }
    private Button button(LinearLayout parent, int label) {
        Button button = new Button(activity); button.setText(label); parent.addView(button);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent));
        button.setTextColor(androidx.core.graphics.ColorUtils.calculateLuminance(accent) > .179 ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
        return button;
    }
    private Spinner options(LinearLayout parent, List<String> labels, int selected) {
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item, labels));
        spinner.setSelection(selected); parent.addView(spinner); return spinner;
    }
    private String money(long minor) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        format.setCurrency(Currency.getInstance(model.selection.currency()));
        format.setMinimumFractionDigits(2); format.setMaximumFractionDigits(2);
        return format.format(BigDecimal.valueOf(minor, 2));
    }
    public void show() {
        ArenaSelection state = model.selection;
        if (state.game() == null || !state.game().id.equals(model.matchId)) return;
        if (panel != null) panel.dismiss();
        final String matchId = state.game().id;
        final ArenaDataset dataset = state.dataset();
        boolean restoring = model.finderOpen && matchId.equals(model.finderDraft.get("match"));
        model.finderOpen = true;
        sheet.collapse();
        panel = new com.google.android.material.bottomsheet.BottomSheetDialog(activity);
        LinearLayout root = new LinearLayout(activity); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(8), dp(10), dp(8)); root.setBackgroundColor(0xfff6f8fc);
        text(root, activity.getString(R.string.finder_title)).setTextColor(accent);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        text(content, activity.getString(R.string.finder_people));
        EditText people = new EditText(activity); people.setInputType(InputType.TYPE_CLASS_NUMBER); people.setText("1"); content.addView(people);
        people.setId(R.id.finder_people_input);
        text(content, activity.getString(R.string.finder_budget));
        EditText budget = new EditText(activity); budget.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        budget.setId(R.id.finder_budget_input);
        budget.setHint(R.string.finder_unlimited);
        if (state.budget() != Long.MAX_VALUE) budget.setText(BigDecimal.valueOf(state.budget(), 2).toPlainString()); content.addView(budget);
        List<String> levels = new ArrayList<>(), levelNames = new ArrayList<>(); levels.add(null); levelNames.add(activity.getString(R.string.all_levels));
        for (ArenaLevel level : dataset.levels.values()) { levels.add(level.id); levelNames.add(level.code); }
        text(content, activity.getString(R.string.filter_level));
        Spinner level = options(content, levelNames, Math.max(0, levels.indexOf(state.levelId())));
        List<String> sections = new ArrayList<>(), sectionNames = new ArrayList<>(); sections.add(null); sectionNames.add(activity.getString(R.string.all_sections));
        for (Section section : dataset.sections.values()) { sections.add(section.id); sectionNames.add(section.code); }
        text(content, activity.getString(R.string.filter_section));
        Spinner section = options(content, sectionNames, Math.max(0, sections.indexOf(state.sectionId())));
        List<String> categories = Arrays.asList(null, "VIP", "STANDARD", "STANDING");
        text(content, activity.getString(R.string.ticket_type));
        Spinner category = options(content, Arrays.asList(activity.getString(R.string.all_types), activity.getString(R.string.seat_type_vip),
                activity.getString(R.string.seat_type_standard), activity.getString(R.string.seat_type_standing)), Math.max(0, categories.indexOf(state.category())));
        text(content, activity.getString(R.string.finder_priority));
        Spinner priority = options(content, Arrays.asList(activity.getString(R.string.finder_price), activity.getString(R.string.finder_court)), 0);
        Button search = button(content, R.string.finder_search);
        search.setId(R.id.finder_search_button);
        TextView message = text(content, activity.getString(R.string.finder_hint));
        message.setId(R.id.finder_message);
        LinearLayout results = new LinearLayout(activity); results.setOrientation(LinearLayout.VERTICAL); content.addView(results);
        results.setId(R.id.finder_results);
        if (restoring) {
            Map<String, String> draft = model.finderDraft;
            people.setText(draft.get("people")); budget.setText(draft.get("budget"));
            level.setSelection(Math.max(0, levels.indexOf(draft.get("level"))));
            section.setSelection(Math.max(0, sections.indexOf(draft.get("section"))));
            category.setSelection(Math.max(0, categories.indexOf(draft.get("category"))));
            priority.setSelection("court".equals(draft.get("priority")) ? 1 : 0);
        }
        saveDraft = () -> {
            Map<String, String> draft = model.finderDraft;
            draft.put("match", matchId); draft.put("people", people.getText().toString()); draft.put("budget", budget.getText().toString());
            draft.put("level", levels.get(level.getSelectedItemPosition())); draft.put("section", sections.get(section.getSelectedItemPosition()));
            draft.put("category", categories.get(category.getSelectedItemPosition())); draft.put("priority", priority.getSelectedItemPosition() == 0 ? "price" : "court");
        };
        Button close = button(root, R.string.finder_close); close.setOnClickListener(v -> panel.dismiss());
        search.setOnClickListener(v -> {
            saveDraft.run();
            final SeatFinder.Request request;
            try {
                int count = Integer.parseInt(people.getText().toString().trim());
                String raw = budget.getText().toString().trim();
                long max = raw.isEmpty() ? Long.MAX_VALUE : new BigDecimal(raw).movePointRight(2).longValueExact();
                request = new SeatFinder.Request(count, max, levels.get(level.getSelectedItemPosition()), sections.get(section.getSelectedItemPosition()),
                        categories.get(category.getSelectedItemPosition()), priority.getSelectedItemPosition() == 0 ? SeatFinder.Priority.PRICE : SeatFinder.Priority.COURT);
            } catch (IllegalArgumentException | ArithmeticException error) { message.setText(R.string.finder_invalid); return; }
            int token = ++generation; results.removeAllViews(); search.setEnabled(false); message.setText(R.string.booking_loading);
            worker.execute(() -> {
                try (BookingStore store = new BookingStore(activity.getApplicationContext())) {
                    ArenaDataset fresh = store.overlay(dataset, dataset.games.get(matchId));
                    List<SeatFinder.Suggestion> found = new SeatFinder().find(fresh, matchId, request);
                    main.post(() -> {
                        if (!active(token, matchId)) return;
                        search.setEnabled(true); message.setText(found.isEmpty() ? R.string.finder_empty : R.string.finder_results);
                        for (SeatFinder.Suggestion result : found) {
                            StringBuilder label = new StringBuilder();
                            for (String id : result.ids) label.append(BookingStore.seatLabel(fresh, id)).append(" · ").append(money(fresh.inventory.get(matchId).get(id).price.amountMinor)).append('\n');
                            label.append(activity.getString(R.string.booking_total, money(result.total))).append('\n')
                                    .append(activity.getString(request.priority == SeatFinder.Priority.PRICE ? R.string.finder_reason_price : R.string.finder_reason_court));
                            if (request.people > 1) label.append('\n').append(activity.getString(R.string.finder_reason_group));
                            TextView card = text(results, label.toString()); card.setBackgroundColor(0xffffffff);
                            card.setOnClickListener(tap -> {
                                state.filterLevel(request.levelId); state.filterSection(request.sectionId); state.filterTickets(request.category, request.budget);
                                state.preview(result.ids.get(0)); model.changed();
                                view.focusSeat(result.ids.get(0));
                                // Existing sheet contains the Canvas perspective and the 6A continue action.
                                sheet.openLargePreview();
                            });
                            Button select = button(results, request.people > 1 ? R.string.finder_select_group : R.string.confirm_seat);
                            if (results.getChildCount() == 2) { card.setId(R.id.finder_preview_first); select.setId(R.id.finder_select_first); }
                            select.setOnClickListener(tap -> {
                                select.setEnabled(false);
                                worker.execute(() -> {
                                    try (BookingStore check = new BookingStore(activity.getApplicationContext())) {
                                        ArenaDataset latest = check.overlay(dataset, dataset.games.get(matchId));
                                        main.post(() -> {
                                            if (!active(token, matchId)) return;
                                            boolean ok = state.selectSuggestion(result, id -> latest.inventory.get(matchId).get(id).baseStatus == SeatStatus.AVAILABLE);
                                            if (ok) {
                                                state.filterLevel(request.levelId); state.filterSection(request.sectionId); state.filterTickets(request.category, request.budget);
                                                model.changed(); panel.dismiss(); sheet.expand();
                                            }
                                            else { results.removeAllViews(); message.setText(R.string.finder_conflict); }
                                        });
                                    } catch (Exception error) { failure(token, matchId, message, select); }
                                });
                            });
                        }
                    });
                } catch (Exception error) { failure(token, matchId, message, search); }
            });
        });
        panel.setContentView(root, new ViewGroup.LayoutParams(-1, (int) (activity.getResources().getDisplayMetrics().heightPixels * PANEL_HEIGHT_FRACTION)));
        panel.setOnDismissListener(d -> { generation++; if (!closed) model.finderOpen = false; }); panel.show();
        Window window = panel.getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(-1, (int) (activity.getResources().getDisplayMetrics().heightPixels * PANEL_HEIGHT_FRACTION));
        com.google.android.material.bottomsheet.BottomSheetBehavior<android.widget.FrameLayout> behavior =
                ((com.google.android.material.bottomsheet.BottomSheetDialog) panel).getBehavior();
        behavior.setMaxHeight((int) (activity.getResources().getDisplayMetrics().heightPixels * PANEL_HEIGHT_FRACTION));
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        if (restoring) search.performClick();
    }
    private boolean active(int token, String matchId) {
        return !closed && token == generation && panel != null && panel.isShowing() && matchId.equals(model.matchId);
    }
    private void failure(int token, String matchId, TextView message, Button button) {
        main.post(() -> { if (active(token, matchId)) { message.setText(R.string.finder_error); button.setEnabled(true); } });
    }
    @Override public void close() { closed = true; generation++; if (panel != null) panel.dismiss(); worker.shutdownNow(); }
}
