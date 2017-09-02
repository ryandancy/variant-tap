package ca.keal.varianttap;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.GameHelper;

/**
 * An enumeration of actions that can be performed with Google Play games services.
 */
enum GPGSAction {
  
  ShowLeaderboard() {
    @Override
    public void performAction(Activity activity, GameHelper gameHelper) {
      super.performAction(activity, gameHelper);
      activity.startActivityForResult(
          Games.Leaderboards.getAllLeaderboardsIntent(gameHelper.getApiClient()),
          Util.REQUEST_LEADERBOARD);
    }
  },
  
  Nothing;
  
  private static final String TAG = "GPGSAction";
  
  void performAction(Activity activity, GameHelper gameHelper) {
    Log.d(TAG, "GPGS action " + this + " performed in " + activity.getLocalClassName());
  }
  
}