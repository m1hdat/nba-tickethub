package com.example.nbatickethub.state;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.example.nbatickethub.state.BrowseNavigation.Page.*;

public class BrowseNavigationTest {
    @Test public void calendarDetailArenaBackRetainsCalendarContext() {
        BrowseNavigation n = new BrowseNavigation(); n.tab(SCHEDULE); n.detail("away-game");
        n.detailScroll = 120; n.arena(); n.arena();
        assertTrue(n.back()); assertEquals(DETAIL, n.page); assertEquals("away-game", n.detailMatchId);
        assertEquals(120, n.detailScroll); assertTrue(n.back()); assertEquals(SCHEDULE, n.page);
    }
    @Test public void directHomeSeatCtaReturnsHomeAndNewDetailResetsOnlyDetailScroll() {
        BrowseNavigation n = new BrowseNavigation(); n.homeScroll = 64; n.arena(); n.back();
        assertEquals(HOME, n.page); assertEquals(64, n.homeScroll);
        n.detail("a"); n.detailScroll = 99; n.detail("b"); assertEquals(0, n.detailScroll);
        n.back(); assertEquals(HOME, n.page);
    }
    @Test public void topLevelBackAndExplicitCalendarSwitchHavePredictableDestinations() {
        BrowseNavigation n = new BrowseNavigation(); assertFalse(n.back()); n.tab(TEAMS); n.back();
        assertEquals(HOME, n.page); n.detail("a"); n.arena(); n.tab(SCHEDULE);
        assertEquals(SCHEDULE, n.page); n.back(); assertEquals(HOME, n.page);
    }
}
