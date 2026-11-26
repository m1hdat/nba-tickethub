package com.example.nbatickethub.state;

import com.example.nbatickethub.model.Game;
import java.text.SimpleDateFormat;
import java.util.*;

/** Device-zone calendar days, never fixed 24-hour durations across DST boundaries. */
public final class MatchListState {
    public enum Range { NEXT_SEVEN, TODAY, DAY, ALL_UPCOMING }
    public String teamId, selectedDay;
    public Range range = Range.NEXT_SEVEN;
    public boolean homeOnly;

    public void selectTeam(String id) {
        teamId = id; homeOnly = false; selectedDay = null;
        range = id == null ? Range.NEXT_SEVEN : Range.ALL_UPCOMING;
    }

    public void selectDay(String day) { selectedDay = day; range = Range.DAY; }
    public void today() { range = Range.TODAY; selectedDay = null; }
    public void sevenDays() { range = Range.NEXT_SEVEN; selectedDay = null; }
    public void clear() { selectTeam(null); }

    public List<Game> filter(List<Game> games, long now, TimeZone zone) {
        String today = dayKey(now, zone);
        Calendar end = Calendar.getInstance(zone);
        end.setTimeInMillis(now); end.add(Calendar.DAY_OF_YEAR, 6);
        String last = dayKey(end.getTimeInMillis(), zone);
        List<Game> result = new ArrayList<>();
        for (Game game : games) {
            if (game.startsAtMillis <= now) { continue; }
            if (teamId != null && !(teamId.equals(game.homeTeamId)
                    || (!homeOnly && teamId.equals(game.awayTeamId)))) { continue; }
            String day = dayKey(game.startsAtMillis, zone);
            if (range == Range.TODAY && !day.equals(today)) { continue; }
            if (range == Range.DAY && !day.equals(selectedDay)) { continue; }
            if (range == Range.NEXT_SEVEN && (day.compareTo(today) < 0 || day.compareTo(last) > 0)) { continue; }
            result.add(game);
        }
        result.sort(Comparator.comparingLong((Game game) -> game.startsAtMillis).thenComparing(game -> game.id));
        return result;
    }

    public static String dayKey(long time, TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
        format.setTimeZone(zone); return format.format(new Date(time));
    }

    public static String display(long time, String pattern, TimeZone zone) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, new Locale("vi", "VN"));
        format.setTimeZone(zone); return format.format(new Date(time));
    }
}
