package ca.keal.varianttap.gpgs;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.example.games.basegameutils.BaseGameUtils;

import java.util.HashMap;
import java.util.Map;

import ca.keal.varianttap.R;
import ca.keal.varianttap.util.Util;

/**
 * A bound service that provides access to one instance of {@link GoogleApiClient} across multiple
 * activities and handles its lifecycle. Activities binding to this service must call
 * {@link #onActivityResult(Activity, int, int, Intent)} from their
 * {@link Activity#onActivityResult(int, int, Intent)} methods. Binding activities should also
 * (but are not required to) call either {@link #trySignIn(Activity)} or
 * {@link #signInSilently(Activity)} as soon as they receive an instance of this service
 * (usually in an implementation of
 * {@link GPGSHelperClient#receiveService(GPGSHelperService)}); they should also call
 * {@link #signInSilently(Activity)} in {@link Activity#onResume()}.
 */
public class GPGSHelperService extends Service {
  
  private static final String TAG = "GPGSHelperService";
  
  private static final int REQUEST_SIGN_IN = 9001;
  
  private final GPGSHelperBinder binder = new GPGSHelperBinder();
  
  private GoogleSignInClient client;
  private GoogleSignInAccount account;
  
  private Activity currentActivity;
  
  private Map<Activity, GPGSAction[]> activityToActionOnSignIn = new HashMap<>();
  
  private ScoreCache scoreCache = new ScoreCache();
  private boolean trySubmitCacheOnSignIn = true; // try to submit the cache when signed in?
  
  /** Null only if we aren't signed in, in which case why are you accessing the account anyways? */
  private GoogleSignInAccount getAccount() {
    if (account == null && isSignedIn()) {
      account = GoogleSignIn.getLastSignedInAccount(this);
    }
    return account;
  }
  
  public boolean isSignedIn() {
    return account != null && GoogleSignIn.getLastSignedInAccount(this) != null;
  }
  
  /**
   * Attempt to sign in to GPGS silently, but if that cannot be done, sign in interactively, unless
   * the user has previously declined to sign in.
   */
  public void trySignIn(final Activity activity) {
    // Connect interactively if the shared preferences say we should
    SharedPreferences prefs = getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE);
    boolean tryInteractively = prefs.getBoolean(Util.PREF_AUTO_SIGN_IN, true);
    signIn(activity, tryInteractively);
  }
  
  /**
   * Attempt to sign in to GPGS silently, but if that cannot be done, sign in interactively.
   */
  public void signIn(Activity activity) {
    signIn(activity, true);
  }
  
  /**
   * Attempt to sign in to GPGS silently, but if that cannot be done, do not attempt to sign in
   * interactively. Call this method in onResume().
   */
  public void signInSilently(Activity activity) {
    signIn(activity, false);
  }
  
  private void signIn(final Activity activity, final boolean tryInteractively) {
    currentActivity = activity;
    
    if (isSignedIn()) {
      // Treat it as if we signed in and succeeded
      performActionsOnSignIn(activity);
      return;
    }
    
    Log.d(TAG, "Attempting silent sign-in");
    
    client.silentSignIn().addOnCompleteListener(new OnCompleteListener<GoogleSignInAccount>() {
      @Override
      public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
        if (task.isSuccessful()) {
          account = task.getResult();
          onSignInSuccessful();
        } else if (tryInteractively) {
          Log.d(TAG, "Silent sign-in failed, attempting interactive sign-in");
          activity.startActivityForResult(client.getSignInIntent(), REQUEST_SIGN_IN);
        } else {
          Log.d(TAG, "Silent sign-in failed");
        }
      }
    });
  }
  
  public void signOut() {
    Log.d(TAG, "Signing out");
    trySubmitCacheOnSignIn = true;
    setAutoSignIn(false);
    client.signOut();
    client.revokeAccess();
    account = null;
  }
  
  /**
   * Call this in onActivityResult().
   */
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "Received activity result in " + activity.getLocalClassName()
        + " (request = " + requestCode + ", result = " + resultCode + ")");
    
    if (requestCode == REQUEST_SIGN_IN) {
      GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
      
      if (result.isSuccess()) {
        Log.d(TAG, "User successfully signed in interactively");
        account = result.getSignInAccount();
        onSignInSuccessful();
      } else if (resultCode == Activity.RESULT_CANCELED) {
        // The user canceled the interactive sign-in
        Log.d(TAG, "User canceled interactive sign-in");
        setAutoSignIn(false);
      } else {
        // Can't sign in
        Log.d(TAG, "Connection resolution failed");
        BaseGameUtils.showActivityResultError(activity, requestCode, resultCode,
            R.string.sign_in_failed);
      }
    } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
      // The user signed out from within one of the default GPGS UIs (i.e. the leaderboard UI)
      Log.d(TAG, "User signed out from within default GPGS UI");
      signOut();
    }
  }
  
  public void onSignInSuccessful() {
    Log.d(TAG, "Successfully signed in");
    
    setCurrentActivityAsPopupView(); // for the "welcome back" notification
    performActionsOnSignIn(currentActivity);
    setAutoSignIn(true);
    
    if (trySubmitCacheOnSignIn) {
      trySubmitCacheOnSignIn = false;
      scoreCache.submitCache(currentActivity, getAccount());
    }
  }
  
  /*
   * There are two actions on sign in for each activity: a primary one and a secondary one.
   * The primary action on sign in is set by activities; the secondary action is set internally as
   * part of tryActionOrConnect(). The two GPGSActions are stored as a length 2 array for
   * simplicity, with the 0th being the primary action and the 1st being the secondary action. This
   * array is then mapped to each activity with the HashMap activityToActionsOnSignIn.
   */
  
  public void setActionOnSignIn(Activity activity, GPGSAction action) {
    setActionOnSignIn(activity, action, 0);
  }
  
  private void setSecondaryActionOnSignIn(Activity activity, GPGSAction action) {
    setActionOnSignIn(activity, action, 1);
  }
  
  private void setActionOnSignIn(Activity activity, GPGSAction action, int which) {
    if (!activityToActionOnSignIn.containsKey(activity)) {
      activityToActionOnSignIn.put(activity, new GPGSAction[2]);
    }
    
    activityToActionOnSignIn.get(activity)[which] = action;
  }
  
  /** If the activity uses actions on sign in, please call this in onStop() */
  public void clearActionOnSignIn(Activity activity) {
    activityToActionOnSignIn.remove(activity);
  }
  
  private void performActionsOnSignIn(Activity activity) {
    if (!activityToActionOnSignIn.containsKey(activity)) return;
    
    for (GPGSAction action : activityToActionOnSignIn.get(activity)) {
      if (action != null) {
        action.performAction(activity, getAccount());
      }
    }
    
    // Remove the actions to prevent them being called multiple times
    activityToActionOnSignIn.remove(activity);
  }
  
  /**
   * Try to perform {@code action}. If we aren't connected to GPGS, sign in and perform
   * {@code action} after connecting.
   */
  public void tryActionOrConnect(Activity activity, GPGSAction action) {
    if (isSignedIn()) {
      action.performAction(activity, getAccount());
    } else {
      setSecondaryActionOnSignIn(activity, action);
      signIn(activity);
    }
  }
  
  private void setAutoSignIn(boolean autoSignIn) {
    SharedPreferences.Editor editor = getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE).edit();
    editor.putBoolean(Util.PREF_AUTO_SIGN_IN, autoSignIn);
    editor.apply();
  }
  
  public void submitScore(Score score) {
    if (isSignedIn()) {
      score.submit(this, getAccount());
    } else {
      scoreCache.cache(this, score);
    }
  }
  
  /** Unlock an achievement. Make sure you're signed in before calling this. */
  public void unlockAchievement(@StringRes int id) {
    if (!isSignedIn()) return;
    
    setCurrentActivityAsPopupView();
    String strId = getString(id);
    Games.getAchievementsClient(this, getAccount()).unlock(strId);
    Log.d(TAG, "Unlocking achievement with id " + strId);
  }
  
  /** Increment an achievement by numSteps. Make sure you're signed in before calling this. */
  public void incrementAchievement(@StringRes int id, int numSteps) {
    if (!isSignedIn()) return;
    
    setCurrentActivityAsPopupView();
    String strId = getString(id);
    Games.getAchievementsClient(this, getAccount()).increment(strId, numSteps);
    Log.d(TAG, "Incrementing achievement with id " + strId);
  }
  
  /** Increment an achievement by 1. Make sure you're signed in before calling this. */
  public void incrementAchievement(@StringRes int id) {
    incrementAchievement(id, 1);
  }
  
  private void setCurrentActivityAsPopupView() {
    Games.getGamesClient(this, getAccount()).setViewForPopups(
        currentActivity.getWindow().getDecorView().findViewById(android.R.id.content));
  }
  
  @Override
  public void onCreate() {
    // Create the client
    Log.d(TAG, "Creating sign-in client");
    client = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);
  }
  
  @Override
  public void onDestroy() {
    // There are no activities left - disconnect from the API
    Log.d(TAG, "Destroying service: signing out from API");
    client.signOut();
    client = null;
    account = null;
  }
  
  @Override
  public GPGSHelperBinder onBind(Intent intent) {
    Log.d(TAG, "Bound to activity");
    return binder;
  }
  
  class GPGSHelperBinder extends Binder {
    
    GPGSHelperService getService() {
      return GPGSHelperService.this;
    }
    
  }
  
}