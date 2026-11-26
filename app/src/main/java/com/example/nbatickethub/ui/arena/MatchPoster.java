package com.example.nbatickethub.ui.arena;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.*;
import androidx.core.graphics.ColorUtils;
import com.example.nbatickethub.R;
import com.example.nbatickethub.data.ArenaCatalog;
import com.example.nbatickethub.model.*;
import com.example.nbatickethub.state.MatchListState;
import java.text.NumberFormat;
import java.util.*;

/** One poster binder for home, schedule and detail; price is the loader's inventory minimum. */
public final class MatchPoster {
    private MatchPoster() { }
    public static ArenaTheme team(ArenaCatalog catalog, String id) {
        for (ArenaTheme theme : catalog.themes) if (theme.teamId.equals(id)) return theme;
        throw new IllegalArgumentException("Unknown team " + id);
    }
    public static String price(Activity activity, ArenaPreviewLoader loader, Game game) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        format.setCurrency(Currency.getInstance("USD"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return activity.getString(R.string.browse_price, format.format(loader.startingPrices.get(game.id) / 100d));
    }
    public static TeamBadgeDrawable badge(Activity activity, ArenaPreviewLoader loader, ArenaTheme team, int dp) {
        return new TeamBadgeDrawable(loader.teamLogo(team), team.teamId.toUpperCase(Locale.ROOT),
                team.primaryColor, Math.round(dp * activity.getResources().getDisplayMetrics().density));
    }
    public static void bind(Activity a, View card, Game game, ArenaCatalog catalog,
                            ArenaPreviewLoader loader, String action, Runnable open) {
        ArenaTheme home = team(catalog, game.homeTeamId), away = team(catalog, game.awayTeamId);
        card.setTag(game.id);
        card.setBackground(new PosterBackground(away.primaryColor, home.primaryColor));
        ((ImageView) card.findViewById(R.id.match_away_logo)).setImageDrawable(badge(a, loader, away, 80));
        ((ImageView) card.findViewById(R.id.match_home_logo)).setImageDrawable(badge(a, loader, home, 80));
        card.findViewById(R.id.match_away_logo).setContentDescription(away.name);
        card.findViewById(R.id.match_home_logo).setContentDescription(home.name);
        ((TextView) card.findViewById(R.id.match_away_name)).setText(away.name);
        ((TextView) card.findViewById(R.id.match_home_name)).setText(home.name);
        for (int id : new int[]{R.id.match_home_name, R.id.match_away_name}) {
            ((TextView) card.findViewById(id)).setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        }
        ((TextView) card.findViewById(R.id.match_start_time)).setText(MatchListState.display(game.startsAtMillis,
                "EEE, dd/MM · HH:mm (z)", TimeZone.getDefault()));
        ((TextView) card.findViewById(R.id.match_venue)).setText(catalog.byId.get(game.arenaId).venueName);
        ((TextView) card.findViewById(R.id.match_from_price)).setText(price(a, loader, game));
        Button button = card.findViewById(R.id.open_match);
        button.setVisibility(action == null ? View.GONE : View.VISIBLE);
        button.setText(action);
        button.setTextColor(0xff101923);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xffffc95b));
        button.setOnClickListener(v -> open.run());
    }
    private static final class PosterBackground extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int away, home;
        PosterBackground(int away, int home) {
            this.away = ColorUtils.blendARGB(away, Color.BLACK, .36f);
            this.home = ColorUtils.blendARGB(home, Color.BLACK, .36f);
        }
        @Override public void draw(Canvas canvas) {
            Rect b = getBounds(); float w = b.width(), h = b.height();
            paint.setStyle(Paint.Style.FILL); paint.setColor(away); canvas.drawRect(b.left, b.top, b.left + w / 2, b.bottom, paint);
            paint.setColor(home); canvas.drawRect(b.left + w / 2, b.top, b.right, b.bottom, paint);
            paint.setColor(0x16ffffff); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2);
            canvas.drawRect(w * .07f, h * .10f, w * .93f, h * .90f, paint);
            canvas.drawLine(w / 2, h * .10f, w / 2, h * .90f, paint);
            canvas.drawCircle(w / 2, h / 2, w * .19f, paint);
        }
        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }
}
