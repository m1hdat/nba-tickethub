package com.example.nbatickethub.state;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/** Retains IDs/filter/semantic camera across rotation; never retains Activity or View. */
public final class ArenaSelectionViewModel extends ViewModel {
    public final BrowseNavigation navigation = new BrowseNavigation();
    public final MatchListState matchList = new MatchListState();
    public int matchListScroll;
    public java.util.List<com.example.nbatickethub.model.Game> sampleGames;
    public final ArenaSelection selection = new ArenaSelection();
    public boolean finderOpen;
    public final java.util.Map<String, String> finderDraft = new java.util.HashMap<>();
    private final MutableLiveData<Integer> revision = new MutableLiveData<>(0);
    public String arenaId, themeId, cameraArenaId;
    public float[] camera;
    public boolean sheetExpanded;
    public String matchId;
    public boolean choosingGame = true;
    public final java.util.Map<String, int[]> ticketScroll = new java.util.HashMap<>();
    public LiveData<Integer> changes() { return revision; }
    public void changed() { revision.setValue(revision.getValue() + 1); }
}
