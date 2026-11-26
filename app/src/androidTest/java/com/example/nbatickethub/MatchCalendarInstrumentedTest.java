package com.example.nbatickethub;

import android.view.View;
import android.widget.*;
import android.os.SystemClock;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.*;
import com.example.nbatickethub.model.*;
import com.example.nbatickethub.state.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.hamcrest.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;

@RunWith(AndroidJUnit4.class)
public class MatchCalendarInstrumentedTest {
    private ArenaSelectionViewModel model(MainActivity a) { return new ViewModelProvider(a).get(ArenaSelectionViewModel.class); }
    private void ready(ActivityScenario<MainActivity> scenario) {
        AtomicBoolean loaded=new AtomicBoolean();long end=SystemClock.uptimeMillis()+40000;
        while(!loaded.get()&&SystemClock.uptimeMillis()<end){scenario.onActivity(a->{if(model(a).sampleGames!=null&&model(a).navigation.page!=BrowseNavigation.Page.SCHEDULE)a.findViewById(R.id.nav_schedule).performClick();loaded.set(model(a).sampleGames!=null&&((LinearLayout)a.findViewById(R.id.game_cards)).getChildCount()>0);});if(!loaded.get())SystemClock.sleep(50);}
        assertTrue("Match picker not ready",loaded.get());
    }
    private Set<String> ids(MainActivity a) {
        LinearLayout cards=a.findViewById(R.id.game_cards);Set<String> ids=new LinkedHashSet<>();
        for(int i=0;i<cards.getChildCount();i++)if(cards.getChildAt(i).getTag() instanceof String)ids.add((String)cards.getChildAt(i).getTag());return ids;
    }
    @Test public void dateTeamHomeOnlyEmptyAndRecreationUseActualControls() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
            ready(scenario);
            scenario.onActivity(a->{assertEquals(MatchListState.Range.NEXT_SEVEN,model(a).matchList.range);assertFalse(ids(a).contains("sample-started"));assertTrue(ids(a).contains("sample-nyk-bos"));a.findViewById(R.id.match_today).performClick();assertEquals(new HashSet<>(Arrays.asList("sample-nyk-bos","sample-gsw-lal")),ids(a));a.findViewById(R.id.match_team).performClick();});
            onData(new BaseMatcher<Object>() {
                @Override public boolean matches(Object value){return value instanceof ArenaTheme&&((ArenaTheme)value).teamId.equals("nyk");}
                @Override public void describeTo(Description description){description.appendText("NYK team");}
            }).inRoot(isDialog()).perform(click());
            scenario.onActivity(a->{assertEquals("nyk",model(a).matchList.teamId);assertEquals(MatchListState.Range.ALL_UPCOMING,model(a).matchList.range);assertTrue(ids(a).contains("sample-nyk-lal-future"));assertTrue(ids(a).contains("sample-bos"));assertEquals(View.VISIBLE,a.findViewById(R.id.match_home_only).getVisibility());a.findViewById(R.id.match_home_only).performClick();assertFalse(ids(a).contains("sample-bos"));assertTrue(ids(a).contains("sample-nyk-lal-future"));a.findViewById(R.id.match_date).performClick();});
            Calendar tomorrow=Calendar.getInstance();tomorrow.add(Calendar.DAY_OF_YEAR,1);
            setDate(tomorrow.get(Calendar.YEAR),tomorrow.get(Calendar.MONTH),tomorrow.get(Calendar.DAY_OF_MONTH));
            scenario.onActivity(a->{assertEquals(Collections.singleton("sample-nyk-mia"),ids(a));assertTrue(model(a).matchList.homeOnly);assertEquals(MatchListState.Range.DAY,model(a).matchList.range);});
            scenario.recreate();ready(scenario);
            scenario.onActivity(a->{assertEquals(Collections.singleton("sample-nyk-mia"),ids(a));assertEquals("nyk",model(a).matchList.teamId);assertTrue(model(a).matchList.homeOnly);a.findViewById(R.id.match_date).performClick();});
            setDate(2000,0,1);
            scenario.onActivity(a->{assertTrue(ids(a).isEmpty());assertEquals(View.VISIBLE,a.findViewById(R.id.match_empty).getVisibility());a.findViewById(R.id.match_clear).performClick();assertNull(model(a).matchList.teamId);assertEquals(MatchListState.Range.NEXT_SEVEN,model(a).matchList.range);assertEquals(View.GONE,a.findViewById(R.id.match_home_only).getVisibility());assertFalse(ids(a).isEmpty());});
        }
    }
    private void setDate(int year,int month,int day) {
        onView(withClassName(org.hamcrest.Matchers.equalTo(DatePicker.class.getName()))).inRoot(isDialog()).perform(new ViewAction(){
            @Override public Matcher<View> getConstraints(){return isAssignableFrom(DatePicker.class);}
            @Override public String getDescription(){return "Choose one local calendar date";}
            @Override public void perform(UiController controller,View view){((DatePicker)view).updateDate(year,month,day);controller.loopMainThreadUntilIdle();}
        });
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
    }
}
