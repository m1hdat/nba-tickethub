package com.example.nbatickethub.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.nbatickethub.model.*;
import org.json.*;
import java.util.*;

/** Local simulated orders. The unique inventory key and whole-order transaction are authoritative. */
public final class BookingStore extends SQLiteOpenHelper {
    public BookingStore(Context context) { this(context, "simulated-bookings.db"); }
    public BookingStore(Context context, String name) { super(context.getApplicationContext(), name, null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE orders (id TEXT PRIMARY KEY, snapshot TEXT NOT NULL)");
        db.execSQL("CREATE TABLE tickets (match_id TEXT NOT NULL, seat_id TEXT NOT NULL, order_id TEXT NOT NULL REFERENCES orders(id), PRIMARY KEY(match_id,seat_id))");
    }
    @Override public void onConfigure(SQLiteDatabase db) { db.setForeignKeyConstraintsEnabled(true); }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { throw new IllegalStateException("Unsupported booking schema"); }

    public static final class Conflict extends Exception {
        public final List<String> seats;
        Conflict(List<String> seats) { super(String.join(", ", seats)); this.seats = Collections.unmodifiableList(seats); }
    }
    public static String seatLabel(ArenaDataset data, String id) {
        Seat seat = data.seats.get(id); SeatRow row = data.rows.get(seat.rowId);
        return data.sections.get(row.sectionId).code + " / " + row.rowLabel + " / " + seat.positionNumber;
    }
    public static JSONObject snapshot(ArenaDataset data, Game game, Collection<String> ids, String requestId) throws JSONException {
        if (ids.isEmpty() || new HashSet<>(ids).size() != ids.size()) throw new IllegalArgumentException("Empty or duplicate seats");
        JSONObject order = new JSONObject(); JSONArray tickets = new JSONArray(); long total = 0; String currency = null;
        for (String id : ids) {
            GameSeat offer = data.inventory.get(game.id).get(id);
            if (offer == null || !offer.gameId.equals(game.id)) throw new IllegalArgumentException("Unknown match/seat");
            if (currency != null && !currency.equals(offer.price.currencyCode)) throw new IllegalArgumentException("Mixed currency");
            currency = offer.price.currencyCode; total = Math.addExact(total, offer.price.amountMinor);
            tickets.put(new JSONObject().put("seatId", id).put("label", seatLabel(data, id)).put("price", offer.price.amountMinor));
        }
        return order.put("id", requestId).put("matchId", game.id).put("homeTeamId", game.homeTeamId)
                .put("match", data.teams.get(game.awayTeamId).displayName + " @ " + data.teams.get(game.homeTeamId).displayName)
                .put("venue", data.arena.name).put("startsAt", game.startsAtMillis).put("time", game.displayTime)
                .put("tickets", tickets).put("currency", currency).put("total", total);
    }
    public JSONObject book(ArenaDataset data, Game game, Collection<String> ids, String requestId) throws Exception {
        JSONObject order = snapshot(data, game, ids, requestId);
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            try (Cursor existing = db.rawQuery("SELECT snapshot FROM orders WHERE id=?", new String[]{requestId})) {
                if (existing.moveToFirst()) {
                    JSONObject saved = new JSONObject(existing.getString(0));
                    if (!saved.getString("matchId").equals(game.id) || !seatIds(saved).equals(new HashSet<>(ids))) {
                        throw new IllegalArgumentException("Request ID reused with different seats");
                    }
                    db.setTransactionSuccessful(); return saved;
                }
            }
            Set<String> booked = bookedIds(db, game.id); List<String> conflicts = new ArrayList<>();
            for (String id : ids) {
                if (data.inventory.get(game.id).get(id).baseStatus != SeatStatus.AVAILABLE || booked.contains(id)) conflicts.add(seatLabel(data, id));
            }
            if (!conflicts.isEmpty()) throw new Conflict(conflicts);
            ContentValues values = new ContentValues(); values.put("id", requestId); values.put("snapshot", order.toString()); db.insertOrThrow("orders", null, values);
            for (String id : ids) {
                values.clear(); values.put("match_id", game.id); values.put("seat_id", id); values.put("order_id", requestId); db.insertOrThrow("tickets", null, values);
            }
            db.setTransactionSuccessful(); return order;
        } finally { db.endTransaction(); }
    }
    private static Set<String> seatIds(JSONObject order) throws JSONException {
        Set<String> ids = new HashSet<>(); JSONArray tickets = order.getJSONArray("tickets");
        for (int i = 0; i < tickets.length(); i++) ids.add(tickets.getJSONObject(i).getString("seatId"));
        return ids;
    }
    private Set<String> bookedIds(SQLiteDatabase db, String matchId) {
        Set<String> ids = new HashSet<>();
        try (Cursor rows = db.rawQuery("SELECT seat_id FROM tickets WHERE match_id=?", new String[]{matchId})) {
            while (rows.moveToNext()) ids.add(rows.getString(0));
        }
        return ids;
    }
    public List<JSONObject> orders() throws JSONException {
        List<JSONObject> orders = new ArrayList<>();
        try (Cursor rows = getReadableDatabase().rawQuery("SELECT snapshot FROM orders ORDER BY rowid DESC", null)) {
            while (rows.moveToNext()) orders.add(new JSONObject(rows.getString(0)));
        }
        return orders;
    }
    public ArenaDataset overlay(ArenaDataset data, Game game) {
        Set<String> ids = bookedIds(getReadableDatabase(), game.id);
        if (ids.isEmpty()) return data;
        List<GameSeat> offers = new ArrayList<>();
        for (Map<String, GameSeat> inventory : data.inventory.values()) for (GameSeat offer : inventory.values()) {
            offers.add(game.id.equals(offer.gameId) && ids.contains(offer.seatId)
                    ? new GameSeat(offer.gameId, offer.seatId, SeatStatus.BOOKED, offer.price, offer.provenance, offer.ticketCategory) : offer);
        }
        return new ArenaDataset(data.arena, new ArrayList<>(data.teams.values()), new ArrayList<>(data.games.values()),
                new ArrayList<>(data.levels.values()), new ArrayList<>(data.sections.values()), new ArrayList<>(data.rows.values()), data.seatOrder, data.geometry, offers);
    }
}
