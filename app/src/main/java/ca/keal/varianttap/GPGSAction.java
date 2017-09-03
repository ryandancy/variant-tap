package ca.keal.varianttap;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

/**
 * An enumeration of actions that can be performed with Google Play games services.
 */
enum GPGSAction {
  
  ShowLeaderboard() {
    @Override
    public void performAction(Activity activity, GoogleApiClient client) {
      super.performAction(activity, client);
      activity.startActivityForResult(
          Games.Leaderboards.getAllLeaderboardsIntent(client),
          Util.REQUEST_LEADERBOARD);
    }
  },
  
  Nothing;
  
  private static final String TAG = "GPGSAction";
  
  void performAction(Activity activity, GoogleApiClient client) {
    Log.d(TAG, "GPGS action " + this + " performed in " + activity.getLocalClassName());
  }
  
}