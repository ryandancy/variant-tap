package ca.keal.varianttap.gpgs;

import android.app.Activity;
import android.os.Bundle;
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
  
  HideSignInButton() {
    @Override
    public void performAction(Activity activity, GoogleApiClient client, Bundle args) {
      super.performAction(activity, client, args);
      
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
    public void performAction(Activity activity, GoogleApiClient client, Bundle args) {
      super.performAction(activity, client, args);
      activity.startActivityForResult(
          Games.Leaderboards.getAllLeaderboardsIntent(client),
          Util.REQUEST_LEADERBOARD);
    }
  },
  
  ShowAchievements() {
    @Override
    public void performAction(Activity activity, GoogleApiClient client, Bundle args) {
      super.performAction(activity, client, args);
      activity.startActivityForResult(
          Games.Achievements.getAchievementsIntent(client),
          Util.REQUEST_ACHIEVEMENTS);
    }
  },
  
  Nothing;
  
  private static final String TAG = "GPGSAction";
  
  public void performAction(Activity activity, GoogleApiClient client, Bundle args) {
    Log.d(TAG, "GPGS action " + this + " performed in " + activity.getLocalClassName()
        + (args == null ? "" : " (arguments: " + args + ")"));
  }
  
  public void performAction(Activity activity, GoogleApiClient client) {
    performAction(activity, client, null);
  }
  
}