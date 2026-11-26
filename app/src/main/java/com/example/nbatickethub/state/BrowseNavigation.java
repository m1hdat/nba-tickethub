package com.example.nbatickethub.state;

/** Browse stack is separate from seat state: returning never rebinds a different match. */
public final class BrowseNavigation {
    public enum Page { HOME, SCHEDULE, TEAMS, DETAIL, ARENA }
    public Page page = Page.HOME;
    public Page detailOrigin = Page.HOME;
    public Page arenaOrigin = Page.HOME;
    public String detailMatchId;
    public int homeScroll, teamsScroll, detailScroll;

    public void tab(Page target) {
        if (target != Page.HOME && target != Page.SCHEDULE && target != Page.TEAMS) {
            throw new IllegalArgumentException("Not a tab");
        }
        page = target;
    }
    public void detail(String matchId) {
        if (page != Page.DETAIL) detailOrigin = page;
        if (!matchId.equals(detailMatchId)) detailScroll = 0;
        detailMatchId = matchId;
        page = Page.DETAIL;
    }
    public void arena() {
        if (page != Page.ARENA) arenaOrigin = page;
        page = Page.ARENA;
    }
    public boolean back() {
        if (page == Page.ARENA) page = arenaOrigin;
        else if (page == Page.DETAIL) page = detailOrigin;
        else if (page != Page.HOME) page = Page.HOME;
        else return false;
        return true;
    }
}
