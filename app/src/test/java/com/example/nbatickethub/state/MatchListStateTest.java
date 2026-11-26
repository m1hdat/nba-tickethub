package com.example.nbatickethub.state;

import com.example.nbatickethub.model.*;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.data.mock.MockMatchSchedule;
import org.junit.Test;
import java.util.*;
import java.nio.file.*;
import static org.junit.Assert.*;

public class MatchListStateTest {
    private final TimeZone zone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
    private final DataProvenance provenance = new DataProvenance(DataProvenance.OriginType.MOCK,
            "test",1,"Test schedule",null,null);
    private long at(TimeZone tz,int year,int month,int day,int hour,int minute) {
        Calendar c=Calendar.getInstance(tz);c.clear();c.set(year,month-1,day,hour,minute);return c.getTimeInMillis();
    }
    private Game game(String id,String home,String away,long time) {
        return new Game(id,home,away,"arena","Sample",provenance,time);
    }
    @Test public void todayExcludesStartedIncludingExactKickoffAndSortsByTime() {
        long now=at(zone,2026,11,26,12,0);MatchListState state=new MatchListState();state.today();
        List<Game> games=Arrays.asList(game("late","nyk","bos",now+60000),game("started","nyk","bos",now-1),
                game("exact","nyk","bos",now),game("soon","nyk","mia",now+1000),game("tomorrow","nyk","mia",at(zone,2026,11,27,0,0)));
        List<Game> visible=state.filter(games,now,zone);assertEquals(2,visible.size());assertEquals("soon",visible.get(0).id);assertEquals("late",visible.get(1).id);
    }
    @Test public void sevenDaysIncludeTodayAndEntireSixthFollowingDayAcrossYearBoundary() {
        long now=at(zone,2026,12,31,12,0);MatchListState state=new MatchListState();
        List<Game> games=Arrays.asList(game("today","nyk","bos",now+1),game("last","nyk","bos",at(zone,2027,1,6,23,59)),game("outside","nyk","bos",at(zone,2027,1,7,0,0)));
        List<Game> visible=state.filter(games,now,zone);assertEquals(2,visible.size());assertEquals("last",visible.get(1).id);
    }
    @Test public void teamDefaultsToAllUpcomingAndHomeOnlyAndExplicitDayCompose() {
        long now=at(zone,2026,11,26,12,0);MatchListState state=new MatchListState();state.selectTeam("nyk");
        assertEquals(MatchListState.Range.ALL_UPCOMING,state.range);
        List<Game> games=Arrays.asList(game("home","nyk","bos",at(zone,2026,12,6,19,0)),game("away","bos","nyk",at(zone,2026,12,8,19,0)),game("other","bos","gsw",now+1000));
        assertEquals(2,state.filter(games,now,zone).size());state.homeOnly=true;
        assertEquals("home",state.filter(games,now,zone).get(0).id);
        state.selectDay("2026-12-08");assertTrue(state.filter(games,now,zone).isEmpty());state.homeOnly=false;
        assertEquals("away",state.filter(games,now,zone).get(0).id);
        state.clear();assertNull(state.teamId);assertFalse(state.homeOnly);assertEquals(MatchListState.Range.NEXT_SEVEN,state.range);
    }
    @Test public void explicitDateUsesDeviceZoneInsteadOfUtcSubstring() {
        long now=at(TimeZone.getTimeZone("UTC"),2026,11,26,20,0);
        Game fixture=game("cross-zone","nyk","bos",at(TimeZone.getTimeZone("UTC"),2026,11,27,0,30));
        MatchListState state=new MatchListState();state.selectDay("2026-11-27");
        assertEquals(1,state.filter(Collections.singletonList(fixture),now,zone).size());
        assertTrue(state.filter(Collections.singletonList(fixture),now,TimeZone.getTimeZone("America/Los_Angeles")).isEmpty());
        assertEquals("2026-11-26",MatchListState.dayKey(fixture.startsAtMillis,TimeZone.getTimeZone("America/Los_Angeles")));
    }
    @Test public void daylightSavingUsesCalendarDayWindow() {
        TimeZone ny=TimeZone.getTimeZone("America/New_York");long now=at(ny,2026,3,7,23,0);
        MatchListState state=new MatchListState();List<Game> games=Arrays.asList(game("day-six","nyk","bos",at(ny,2026,3,13,23,59)),game("day-seven","nyk","bos",at(ny,2026,3,14,0,0)));
        List<Game> visible=state.filter(games,now,ny);assertEquals(1,visible.size());assertEquals("day-six",visible.get(0).id);
    }
    @Test public void relativeDemoHasUpcomingTodayPastHomeAwayAndOutsideSevenDays() throws Exception {
        Path root=Files.isDirectory(Path.of("src/main/assets"))?Path.of("src/main/assets"):Path.of("app/src/main/assets");
        ArenaCatalog catalog=new ArenaCatalogReader().load(name->Files.newInputStream(root.resolve(name)));
        long now=at(zone,2031,1,15,23,55);List<Game> games=MockMatchSchedule.games(catalog,now,zone);
        assertEquals(33,games.size());assertEquals(games.get(0).arenaId,games.get(1).arenaId);
        assertNotEquals(games.get(0).awayTeamId,games.get(1).awayTeamId);
        MatchListState state=new MatchListState();state.today();List<Game> today=state.filter(games,now,zone);
        assertEquals(2,today.size());assertTrue(today.stream().allMatch(g->MatchListState.dayKey(g.startsAtMillis,zone).equals("2031-01-15")));
        assertTrue(games.stream().anyMatch(g->g.startsAtMillis<now));state.selectTeam("nyk");
        List<Game> team=state.filter(games,now,zone);assertTrue(team.stream().anyMatch(g->g.awayTeamId.equals("nyk")));
        assertTrue(team.stream().anyMatch(g->g.id.equals("sample-nyk-lal-future")));
        state.sevenDays();assertFalse(state.filter(games,now,zone).stream().anyMatch(g->g.id.equals("sample-nyk-lal-future")));
    }
}
