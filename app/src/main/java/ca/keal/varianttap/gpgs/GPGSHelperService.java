package ca.keal.varianttap.gpgs;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.example.games.basegameutils.BaseGameUtils;

import ca.keal.varianttap.R;
import ca.keal.varianttap.util.Util;

/**
 * A bound service that provies access to one instance of {@link GoogleApiClient} across multiple
 * activities and handles its lifecycle. Activities binding to this service must call
 * {@link #onActivityResult(Activity, int, int)} from their
 * {@link Activity#onActivityResult(int, int, Intent)} methods. Binding activities should also
 * (but are not required to) call either {@link #tryAutoConnect(Activity)} or
 * {@link #connectWithoutSignInFlow(Activity)} as soon as they receive an instance of this service
 * (usually in an implementation of
 * {@link GPGSHelperClient#receiveService(GPGSHelperService)}).
 */
public class GPGSHelperService extends Service
    implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {
  
  private static final String TAG = "GPGSHelperService";
  
  private static final int REQUEST_SIGN_IN = 9001;
  
  private final GPGSHelperBinder binder = new GPGSHelperBinder();
  
  private GoogleApiClient client;
  
  private Activity currentActivity;
  private boolean resolvingConnectionFailure = false;
  private boolean trySignIn = false; // try to sign in when connection fails?
  
  private GPGSAction actionOnSignIn = GPGSAction.Nothing;
  private Bundle actionOnSignInArgs = null;
  
  private ScoreCache scoreCache = new ScoreCache();
  private boolean trySubmitCacheOnSignIn = true; // try to submit the cache when signed in?
  
  public GoogleApiClient getApiClient() {
    return client;
  }
  
  public ScoreCache getScoreCache() {
    return scoreCache;
  }
  
  public boolean isConnected() {
    return client != null && client.isConnected();
  }
  
  public void tryAutoConnect(Activity activity) {
    if (isConnected()) return;
    
    // Auto-connect if the shared preferences say we should
    SharedPreferences prefs = getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE);
    if (prefs.getBoolean(Util.PREF_AUTO_SIGN_IN, true)) {
      connect(activity);
    }
  }
  
  public void connect(Activity activity) {
    connect(activity, true);
  }
  
  /**
   * Attempt to connect, but if the attempt fails, don't try to resolve it (i.e. don't start the
   * sign-in flow).
   */
  public void connectWithoutSignInFlow(Activity activity) {
    connect(activity, false);
  }
  
  private void connect(Activity activity, boolean trySignIn) {
    if (resolvingConnectionFailure) {
      Log.w(TAG, "Attempted to connect while resolving connection failure");
      return;
    }
    
    if (isConnected()) {
      // Treat it as if we connected and succeeded
      actionOnSignIn.performAction(activity, client);
      return;
    }
    
    Log.d(TAG, "Attempting to connect...");
    this.trySignIn = trySignIn;
    currentActivity = activity;
    client.connect();
  }
  
  public void signOut() {
    if (!isConnected()) {
      Log.w(TAG, "Attempted to sign out when already disconnected");
      return;
    }
    
    Log.d(TAG, "Signing out");
    setAutoSignIn(false);
    Games.signOut(client);
    client.disconnect();
  }
  
  public void onActivityResult(Activity activity, int requestCode, int resultCode) {
    Log.d(TAG, "Received activity result in " + activity.getLocalClassName()
        + " (request = " + requestCode + ", result = " + resultCode + ")");
    
    if (requestCode == REQUEST_SIGN_IN) {
      trySignIn = false;
      resolvingConnectionFailure = false;
  
      if (resultCode == Activity.RESULT_OK) {
        Log.d(TAG, "Resolved connection failure, connecting...");
        client.connect();
      } else if (resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
        Log.d(TAG, "Reconnect required, connecting...");
        client.connect();
      } else if (resultCode == Activity.RESULT_CANCELED) {
        // The user canceled the sign-in flow
        Log.d(TAG, "User canceled sign-in");
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
      setAutoSignIn(false);
      client.disconnect();
    }
  }
  
  public void setActionOnSignIn(Activity activity, GPGSAction action, Bundle args) {
    currentActivity = activity;
    actionOnSignIn = action;
    actionOnSignInArgs = args;
  }
  
  public void setActionOnSignIn(Activity activity, GPGSAction action) {
    setActionOnSignIn(activity, action, null);
  }
  
  /**
   * Try to perform {@code action}. If we aren't connected to GPGS, connect and perform
   * {@code action} after connecting.
   */
  public void tryActionOrConnect(Activity activity, GPGSAction action) {
    if (isConnected()) {
      action.performAction(activity, getApiClient());
    } else {
      setActionOnSignIn(activity, action);
      connect(activity);
    }
  }
  
  private void setAutoSignIn(boolean autoSignIn) {
    SharedPreferences.Editor editor = getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE).edit();
    editor.putBoolean(Util.PREF_AUTO_SIGN_IN, autoSignIn);
    editor.apply();
  }
  
  /** Unlock an achievement. Make sure you're signed in before calling this. */
  public void unlockAchievement(@StringRes int id) {
    setCurrentActivityAsPopupView();
    String strId = getString(id);
    Games.Achievements.unlock(client, strId);
    Log.d(TAG, "Unlocking achievement with id " + strId);
  }
  
  /** Increment an achievement by numSteps. Make sure you're signed in before calling this. */
  public void incrementAchievement(@StringRes int id, int numSteps) {
    setCurrentActivityAsPopupView();
    String strId = getString(id);
    Games.Achievements.increment(client, strId, numSteps);
    Log.d(TAG, "Incrementing achievement with id " + strId);
  }
  
  /** Increment an achievement by 1. Make sure you're signed in before calling this. */
  public void incrementAchievement(@StringRes int id) {
    incrementAchievement(id, 1);
  }
  
  private void setCurrentActivityAsPopupView() {
    Games.setViewForPopups(client, currentActivity.getWindow().getDecorView()
        .findViewById(android.R.id.content));
  }
  
  @Override
  public void onCreate() {
    // Create the client
    Log.d(TAG, "Creating API client");
    client = new GoogleApiClient.Builder(this)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .addApi(Games.API).addScope(Games.SCOPE_GAMES)
        .build();
  }
  
  @Override
  public void onDestroy() {
    // There are no activities left - disconnect from the API
    Log.d(TAG, "Destroying service: disconnecting from API");
    client.unregisterConnectionCallbacks(this);
    client.unregisterConnectionFailedListener(this);
    client.disconnect();
    client = null;
  }
  
  @Override
  public GPGSHelperBinder onBind(Intent intent) {
    Log.d(TAG, "Bound to activity");
    return binder;
  }
  
  @Override
  public void onConnectionFailed(@NonNull ConnectionResult result) {
    if (resolvingConnectionFailure) return; // already resolving
    
    if (trySignIn) {
      // Attempt to resolve the connection failure, usually resulting in the sign-in flow
      Log.d(TAG, "Attempt to connect failed, attempting to resolve...");
      Log.d(TAG, "(Error: " + result + ")");
      trySignIn = false;
      resolvingConnectionFailure = true;
      
      //noinspection RedundantIfStatement
      if (!BaseGameUtils.resolveConnectionFailure(currentActivity, client, result, REQUEST_SIGN_IN,
          R.string.sign_in_other_error)) {
        Log.d(TAG, "Could not resolve connection failure");
        resolvingConnectionFailure = false;
      }
    }
  }
  
  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.d(TAG, "Connected");
    
    actionOnSignIn.performAction(currentActivity, client, actionOnSignInArgs);
    setAutoSignIn(true);
    
    if (trySubmitCacheOnSignIn) {
      trySubmitCacheOnSignIn = false;
      scoreCache.submitCache(currentActivity, client);
    }
  }
  
  @Override
  public void onConnectionSuspended(int i) {
    Log.d(TAG, "Connection suspended, cause = " + i);
  }
  
  class GPGSHelperBinder extends Binder {
    
    GPGSHelperService getService() {
      return GPGSHelperService.this;
    }
    
  }
  
}