package ca.keal.varianttap;

import android.app.Activity;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

/**
 * An enumeration of actions that can be performed with Google Play games services once signed in.
 * Use {@link #performAction(Activity, GoogleApiClient)} to perform an action. It is the calling
 * code's responsibility to ensure that the {@link GoogleApiClient} is valid and connected before
 * calling {@link #performAction(Activity, GoogleApiClient)}.
 */
enum GPGSAction {
  
  HideSignInButton() {
    @Override
    void performAction(Activity activity, GoogleApiClient client) {
      super.performAction(activity, client);
      
      // Check that it has a sign-in button
      if (!(activity instanceof HasSignInButton)) {
        Log.w(TAG, "HideSignInButton: activity does not have sign-in button");
        return;
      }
      
      // Hide it
      HasSignInButton hsib = (HasSignInButton) activity;
      hsib.hideSignInButton();
    }
  },
  
  ShowLeaderboard() {
    @Override
    void performAction(Activity activity, GoogleApiClient client) {
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