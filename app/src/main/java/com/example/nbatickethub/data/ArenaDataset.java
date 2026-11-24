package com.example.nbatickethub.data;

import com.example.nbatickethub.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** One immutable, validated version of arena geometry and per-game inventory. */
public final class ArenaDataset {
    public final Arena arena;
    public final Map<String, Team> teams;
    public final Map<String, Game> games;
    public final Map<String, ArenaLevel> levels;
    public final Map<String, Section> sections;
    public final Map<String, SeatRow> rows;
    public final Map<String, Seat> seats;
    public final List<Seat> seatOrder;
    public final List<SeatGeometry> geometry;
    public final Map<String, Map<String, GameSeat>> inventory;

    public ArenaDataset(Arena arena, List<Team> teams, List<Game> games,
                        List<ArenaLevel> levels, List<Section> sections, List<SeatRow> rows,
                        List<Seat> seats, List<SeatGeometry> geometry, List<GameSeat> offers) {
        this.arena = Objects.requireNonNull(arena, "arena");
        this.teams = index(teams, value -> value.id);
        this.games = index(games, value -> value.id);
        this.levels = index(levels, value -> value.id);
        this.sections = index(sections, value -> value.id);
        this.rows = index(rows, value -> value.id);
        this.seats = index(seats, value -> value.id);
        this.seatOrder = Collections.unmodifiableList(new ArrayList<>(seats));
        this.geometry = Collections.unmodifiableList(new ArrayList<>(geometry));
        Map<String, Map<String, GameSeat>> grouped = new LinkedHashMap<>();
        for (GameSeat offer : offers) {
            Objects.requireNonNull(offer, "offer");
            Map<String, GameSeat> bySeat = grouped.computeIfAbsent(offer.gameId,
                    key -> new LinkedHashMap<>());
            if (bySeat.put(offer.seatId, offer) != null) {
                throw new IllegalArgumentException("Duplicate game/seat offer: "
                        + offer.gameId + "/" + offer.seatId);
            }
        }
        Map<String, Map<String, GameSeat>> frozen = new LinkedHashMap<>();
        grouped.forEach((game, bySeat) -> frozen.put(game,
                Collections.unmodifiableMap(bySeat)));
        this.inventory = Collections.unmodifiableMap(frozen);
        ArenaDataValidator.validate(this);
    }

    public int capacity() { return seats.size(); }

    public long seatedCapacity() {
        return seatOrder.stream().filter(seat -> seat.kind == PositionKind.SEATED).count();
    }

    public long availableCount(String gameId) {
        Map<String, GameSeat> offers = inventory.get(gameId);
        if (offers == null) { throw new IllegalArgumentException("Unknown game: " + gameId); }
        return offers.values().stream().filter(offer -> offer.baseStatus == SeatStatus.AVAILABLE)
                .count();
    }

    private static <T> Map<String, T> index(List<T> values, Function<T, String> id) {
        Map<String, T> result = new LinkedHashMap<>();
        for (T value : values) {
            Objects.requireNonNull(value, "dataset item");
            String key = id.apply(value);
            if (result.put(key, value) != null) {
                throw new IllegalArgumentException("Duplicate ID: " + key);
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
