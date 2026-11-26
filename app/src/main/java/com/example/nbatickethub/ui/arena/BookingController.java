package com.example.nbatickethub.ui.arena;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.lifecycle.ViewModelProvider;
import com.example.nbatickethub.R;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.model.ArenaTheme;
import com.example.nbatickethub.state.*;
import org.json.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

/** Native Views checkout and stored ticket cards; no payment or account surface. */
public final class BookingController implements AutoCloseable {
    private final AppCompatActivity activity;
    private final ArenaSelectionViewModel arena;
    private final BookingViewModel model;
    private final ArenaCatalog catalog;
    private final Runnable refreshArena, showTickets;
    private Dialog dialog;
    private LinearLayout content;
    private Button confirm, back;
    private String renderedOrder;
    public BookingController(AppCompatActivity activity, ArenaSelectionViewModel arena, ArenaCatalog catalog,
                             Runnable refreshArena, Runnable showTickets) {
        this.activity = activity; this.arena = arena; this.catalog = catalog;
        this.refreshArena = refreshArena; this.showTickets = showTickets;
        model = new ViewModelProvider(activity).get(BookingViewModel.class);
        activity.findViewById(R.id.booking_continue).setOnClickListener(v -> {
            if (arena.selection.game() != null && arena.selection.game().id.equals(arena.matchId)) {
                model.begin(arena.selection.dataset(), arena.selection.game(), arena.selection.selectedIds());
            }
        });
        model.changes.observe(activity, revision -> render());
    }
    public void tickets() { renderTickets(); model.loadOrders(); renderTickets(); }
    public void selectionChanged() {
        activity.findViewById(R.id.booking_continue).setVisibility(arena.selection.selectedIds().isEmpty() ? View.GONE : View.VISIBLE);
        activity.findViewById(R.id.booking_continue).setEnabled(!arena.selection.selectedIds().isEmpty() && !model.busy);
    }
    private ArenaTheme theme(JSONObject order) {
        for (ArenaTheme theme : catalog.themes) if (theme.teamId.equals(order.optString("homeTeamId"))) return theme;
        return catalog.themes.get(0);
    }
    private String money(long minor, String currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        format.setCurrency(Currency.getInstance(currency)); format.setMinimumFractionDigits(2); format.setMaximumFractionDigits(2);
        return format.format(BigDecimal.valueOf(minor, 2));
    }
    private TextView text(LinearLayout parent, String value, int size, boolean bold) {
        TextView view = new TextView(activity); view.setText(value); view.setTextSize(size);
        view.setTextColor(parent.getId() == R.id.my_tickets_list ? activity.getColor(R.color.browse_text) : 0xff10283f);
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(dp(8), dp(8), dp(8), dp(8)); parent.addView(view); return view;
    }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(activity); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private void card(LinearLayout parent, JSONObject order, boolean saved) {
        ArenaTheme team = theme(order); LinearLayout card = column(); card.setPadding(dp(8), dp(8), dp(8), dp(8));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(16)); bg.setStroke(dp(2), team.primaryColor); card.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, dp(8), 0, dp(12)); parent.addView(card, params);
        text(card, activity.getString(R.string.booking_simulated), 13, true);
        if (saved) text(card, activity.getString(R.string.booking_order, order.optString("id")), 13, true);
        TextView title = text(card, order.optString("match"), 22, true); title.setTypeface(TeamAppearance.heading(order.optString("homeTeamId")));
        String time = MatchListState.display(order.optLong("startsAt"), "dd/MM/yyyy · HH:mm (z)", TimeZone.getDefault());
        text(card, time + "\n" + order.optString("venue"), 15, false);
        JSONArray tickets = order.optJSONArray("tickets"); String currency = order.optString("currency");
        for (int i = 0; i < tickets.length(); i++) {
            JSONObject ticket = tickets.optJSONObject(i);
            text(card, activity.getString(R.string.booking_seat, ticket.optString("label")) + "\n" + money(ticket.optLong("price"), currency), 16, false);
        }
        text(card, activity.getString(R.string.booking_total, money(order.optLong("total"), currency)), 22, true);
    }
    private void renderTickets() {
        LinearLayout list = activity.findViewById(R.id.my_tickets_list); list.removeAllViews();
        text(list, activity.getString(R.string.booking_my_tickets), 26, true);
        if (model.loadingOrders) text(list, activity.getString(R.string.booking_loading), 16, false);
        else if (model.error != null && !model.showing) text(list, activity.getString(R.string.booking_load_error), 16, false);
        else if (model.orders.isEmpty()) text(list, activity.getString(R.string.booking_empty), 17, false);
        for (JSONObject order : model.orders) card(list, order, true);
    }
    private void render() {
        selectionChanged(); renderTickets();
        if (!model.showing || model.draft == null || activity.isFinishing()) return;
        if (dialog == null) {
            dialog = new Dialog(activity, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar);
            LinearLayout root = column(); root.setPadding(dp(16), dp(12), dp(16), dp(12));
            ArenaTheme team = theme(model.draft); root.setBackground(TeamAppearance.background(team, true));
            back = new Button(activity); back.setText(R.string.booking_back); root.addView(back, new LinearLayout.LayoutParams(-1, dp(48)));
            ScrollView scroll = new ScrollView(activity); content = column(); scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            confirm = new Button(activity); confirm.setId(R.id.booking_confirm);
            confirm.setBackgroundTintList(ColorStateList.valueOf(team.primaryColor));
            confirm.setTextColor(ColorUtils.calculateLuminance(team.primaryColor) > .179 ? Color.BLACK : Color.WHITE);
            root.addView(confirm, new LinearLayout.LayoutParams(-1, dp(56)));
            dialog.setContentView(root); dialog.setOnCancelListener(d -> leave());
            back.setOnClickListener(v -> leave());
            confirm.setOnClickListener(v -> { if (model.result != null) { leave(); showTickets.run(); } else model.confirm(); });
            dialog.show(); dialog.getWindow().setLayout(-1, -1);
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(dp(16) + bars.left, dp(12) + bars.top, dp(16) + bars.right, dp(12) + bars.bottom); return insets;
            });
        }
        dialog.setCancelable(!model.busy); back.setEnabled(!model.busy);
        String key = model.result == null ? "draft" : model.result.optString("id");
        if (!key.equals(renderedOrder) || model.error != null) {
            content.removeAllViews(); card(content, model.result == null ? model.draft : model.result, model.result != null);
            if (model.result != null) text(content, activity.getString(R.string.booking_success), 19, true);
            if (model.error instanceof BookingStore.Conflict) text(content, activity.getString(R.string.booking_conflict, model.error.getMessage()), 17, true);
            else if (model.error != null) text(content, activity.getString(R.string.booking_error), 17, true);
            renderedOrder = key;
        }
        confirm.setText(model.result != null ? R.string.booking_view_tickets : model.busy ? R.string.booking_saving : R.string.booking_confirm);
        confirm.setEnabled(!model.busy && !(model.error instanceof BookingStore.Conflict));
        back.setText(model.error instanceof BookingStore.Conflict ? R.string.booking_reselect : R.string.booking_back);
    }
    private void leave() {
        if (model.busy) return;
        boolean refresh = model.result != null || model.error instanceof BookingStore.Conflict;
        model.showing = false; if (dialog != null) { dialog.dismiss(); dialog = null; } renderedOrder = null;
        if (refresh) refreshArena.run();
    }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    @Override public void close() { if (dialog != null) { dialog.dismiss(); dialog = null; } }
}
