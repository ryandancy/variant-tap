package ca.keal.varianttap.gpgs;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

import ca.keal.varianttap.util.Util;

/**
 * An enumeration of actions that can be performed with Google Play games services once signed in.
 * Use {@link #performAction(Activity, GoogleApiClient)} to perform an action. It is the calling
 * code's responsibility to ensure that the {@link GoogleApiClient} is valid and connected before
 * calling {@link #performAction(Activity, GoogleApiClient)}.
 */
public enum GPGSAction {
  
  CallCallback() {
    @Override
    public void performAction(Activity activity, GoogleApiClient client) {
      super.performAction(activity, client);
      
      // Check that it's a gpgsCallback
      if (!(activity instanceof GPGSCallback)) {
        Log.w(TAG, "CallCallback: activity does not implement GPGSCallback");
        return;
      }
      
      // Call it
      GPGSCallback cb = (GPGSCallback) activity;
      cb.gpgsCallback();
    }
  },
  
  ShowLeaderboard() {
    @Override
    public void performAction(Activity activity, GoogleApiClient client) {
      super.performAction(activity, client);
      activity.startActivityForResult(
          Games.Leaderboards.getAllLeaderboardsIntent(client),
          Util.REQUEST_LEADERBOARD);
    }
  },
  
  ShowAchievements() {
    @Override
    public void performAction(Activity activity, GoogleApiClient client) {
      super.performAction(activity, client);
      activity.startActivityForResult(
          Games.Achievements.getAchievementsIntent(client),
          Util.REQUEST_ACHIEVEMENTS);
    }
  };
  
  private static final String TAG = "GPGSAction";
  
  public void performAction(Activity activity, GoogleApiClient client) {
    Log.d(TAG, "GPGS action " + this + " performed in " + activity.getLocalClassName());
  }
  
}