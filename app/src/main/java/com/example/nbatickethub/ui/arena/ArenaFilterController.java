package com.example.nbatickethub.ui.arena;

import android.app.Activity;
import android.view.View;
import android.widget.*;
import com.example.nbatickethub.R;
import com.example.nbatickethub.model.*;
import com.example.nbatickethub.state.ArenaSelectionViewModel;
import java.util.*;

/** Small level/section controls. Filtering never changes the camera or selected IDs. */
public final class ArenaFilterController {
    private final Activity activity;
    private final ArenaSelectionViewModel model;
    private final Spinner levels, sections, types, budgets;
    private ArenaRenderData data;
    private String levelKey;
    private boolean binding;

    public ArenaFilterController(Activity activity, ArenaSelectionViewModel model) {
        this.activity = activity; this.model = model;
        types = activity.findViewById(R.id.type_filter); budgets = activity.findViewById(R.id.budget_filter);
        types.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                new String[]{activity.getString(R.string.all_types), activity.getString(R.string.seat_type_vip), activity.getString(R.string.seat_type_standard), activity.getString(R.string.seat_type_standing)}));
        budgets.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_spinner_dropdown_item,
                new String[]{activity.getString(R.string.all_budgets), activity.getString(R.string.budget_100), activity.getString(R.string.budget_250), activity.getString(R.string.budget_500)}));
        AdapterView.OnItemSelectedListener ticketListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (data == null || binding) return;
                String category = new String[]{null, "VIP", "STANDARD", "STANDING"}[types.getSelectedItemPosition()];
                long budget = new long[]{Long.MAX_VALUE, 10000, 25000, 50000}[budgets.getSelectedItemPosition()];
                if (Objects.equals(category, model.selection.category()) && budget == model.selection.budget()) return;
                model.selection.filterTickets(category, budget); model.changed();
            }
        };
        types.setOnItemSelectedListener(ticketListener); budgets.setOnItemSelectedListener(ticketListener);
        levels = activity.findViewById(R.id.level_filter);
        sections = activity.findViewById(R.id.section_filter);
        levels.setOnItemSelectedListener(listener(true));
        sections.setOnItemSelectedListener(listener(false));
        activity.findViewById(R.id.clear_filter).setOnClickListener(view -> {
            if (data != null) { model.selection.clearFilter(); model.changed(); }
        });
    }

    private AdapterView.OnItemSelectedListener listener(boolean level) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (binding || data == null || position != parent.getSelectedItemPosition()) { return; }
                Object item = parent.getSelectedItem();
                if (!(item instanceof Option)) { return; }
                String value = ((Option) item).id;
                if (Objects.equals(value, level ? model.selection.levelId() : model.selection.sectionId())) { return; }
                if (level) { model.selection.filterLevel(value); }
                else { model.selection.filterSection(value); }
                model.changed();
            }
        };
    }

    public void setData(ArenaRenderData value) {
        data = value; levelKey = null;
        levels.setEnabled(value != null); sections.setEnabled(value != null);
        if (value == null) { return; }
        binding = true;
        List<Option> options = new ArrayList<>();
        options.add(new Option(null, activity.getString(R.string.all_levels)));
        for (ArenaLevel level : value.dataset().levels.values()) { options.add(new Option(level.id, level.code)); }
        bind(levels, options);
        binding = false;
        refresh();
    }

    public void refresh() {
        if (data == null) { return; }
        binding = true;
        String key = data.arenaId() + ":" + model.selection.levelId();
        if (!key.equals(levelKey)) {
            List<Option> options = new ArrayList<>();
            options.add(new Option(null, activity.getString(R.string.all_sections)));
            for (Section section : data.dataset().sections.values()) {
                if (model.selection.levelId() == null || model.selection.levelId().equals(section.levelId)) {
                    options.add(new Option(section.id, section.code));
                }
            }
            bind(sections, options); levelKey = key;
        }
        String[] categories = {null, "VIP", "STANDARD", "STANDING"};
        for (int i = 0; i < categories.length; i++) { if (Objects.equals(categories[i], model.selection.category())) types.setSelection(i); }
        long[] limits = {Long.MAX_VALUE, 10000, 25000, 50000};
        for (int i = 0; i < limits.length; i++) { if (limits[i] == model.selection.budget()) budgets.setSelection(i); }
        select(levels, model.selection.levelId()); select(sections, model.selection.sectionId());
        binding = false;
    }

    private void bind(Spinner spinner, List<Option> options) {
        ArrayAdapter<Option> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
    private void select(Spinner spinner, String id) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (Objects.equals(((Option) spinner.getItemAtPosition(i)).id, id)) {
                if (spinner.getSelectedItemPosition() != i) { spinner.setSelection(i); }
                return;
            }
        }
    }
    private static final class Option {
        final String id, label;
        Option(String id, String label) { this.id = id; this.label = label; }
        @Override public String toString() { return label; }
    }
}
