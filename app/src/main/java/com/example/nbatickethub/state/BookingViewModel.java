package com.example.nbatickethub.state;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import com.example.nbatickethub.data.*;
import com.example.nbatickethub.model.Game;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.*;

/** Retains an in-flight transaction and its request ID across Activity recreation. */
public final class BookingViewModel extends AndroidViewModel {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    public final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    public boolean showing, busy, loadingOrders;
    public JSONObject draft, result;
    public Exception error;
    public List<JSONObject> orders = Collections.emptyList();
    private ArenaDataset dataset;
    private Game game;
    private List<String> ids;
    private String requestId;
    public BookingViewModel(Application app) { super(app); }
    private void changed() { changes.setValue(changes.getValue() + 1); }
    public void begin(ArenaDataset data, Game match, Collection<String> seats) {
        if (busy || seats.isEmpty()) return;
        try {
            dataset = data; game = match; ids = new ArrayList<>(seats); requestId = "SIM-" + UUID.randomUUID();
            draft = BookingStore.snapshot(data, match, ids, requestId); result = null; error = null; showing = true; changed();
        } catch (Exception failure) { error = failure; changed(); }
    }
    public void confirm() {
        if (busy || result != null || !showing || draft == null || error instanceof BookingStore.Conflict) return;
        busy = true; error = null; changed();
        worker.execute(() -> {
            JSONObject saved = null; Exception failure = null;
            try (BookingStore store = new BookingStore(getApplication())) { saved = store.book(dataset, game, ids, requestId); }
            catch (Exception e) { failure = e; }
            JSONObject value = saved; Exception problem = failure;
            main.post(() -> { busy = false; result = value; error = problem; changed(); });
        });
    }
    public void loadOrders() {
        if (loadingOrders) return;
        loadingOrders = true;
        worker.execute(() -> {
            List<JSONObject> values = Collections.emptyList(); Exception failure = null;
            try (BookingStore store = new BookingStore(getApplication())) { values = store.orders(); }
            catch (Exception e) { failure = e; }
            List<JSONObject> loaded = values; Exception problem = failure;
            main.post(() -> { orders = loaded; loadingOrders = false; error = problem; changed(); });
        });
    }
    @Override protected void onCleared() { worker.shutdown(); }
}
