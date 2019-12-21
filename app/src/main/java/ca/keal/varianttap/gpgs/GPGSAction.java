package ca.keal.varianttap.gpgs;

import android.app.Activity;
import android.content.Intent;
import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import ca.keal.varianttap.R;
import ca.keal.varianttap.util.Util;

/**
 * An enumeration of actions that can be performed with Google Play games services once signed in.
 * Use {@link #performAction(Activity, GoogleSignInAccount)} to perform an action. It is the calling
 * code's responsibility to ensure that the {@link GoogleApiClient} is valid and connected before
 * calling {@link #performAction(Activity, GoogleSignInAccount)}.
 */
public enum GPGSAction {
  
  CallCallback() {
    @Override
    public void performAction(Activity activity, GoogleSignInAccount account) {
      super.performAction(activity, account);
      
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
    public void performAction(final Activity activity, GoogleSignInAccount account) {
      super.performAction(activity, account);
      startIntentFromTask(activity,
          Games.getLeaderboardsClient(activity, account).getAllLeaderboardsIntent(),
          Util.REQUEST_LEADERBOARD);
    }
  },
  
  ShowAchievements() {
    @Override
    public void performAction(Activity activity, GoogleSignInAccount account) {
      super.performAction(activity, account);
      startIntentFromTask(activity,
          Games.getAchievementsClient(activity, account).getAchievementsIntent(),
          Util.REQUEST_LEADERBOARD);
    }
  };
  
  private static final String TAG = "GPGSAction";
  
  public void performAction(Activity activity, GoogleSignInAccount account) {
    Log.d(TAG, "GPGS action " + this + " performed in " + activity.getLocalClassName());
  }
  
  private static void startIntentFromTask(
      final Activity activity, Task<Intent> task, final int requestCode) {
    task.addOnCompleteListener(activity, new OnCompleteListener<Intent>() {
      @Override
      public void onComplete(@NonNull Task<Intent> task) {
        if (task.isSuccessful()) {
          activity.startActivityForResult(task.getResult(), requestCode);
        } else {
          // Somehow it failed to get the intent - show a toast??
          Log.e(TAG, "GPGS action " + this + " failed with " + task.getException());
          Toast.makeText(activity, R.string.gpgs_action_intent_failed, Toast.LENGTH_LONG).show();
        }
      }
    });
  }
  
}