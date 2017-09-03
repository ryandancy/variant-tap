package ca.keal.varianttap;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.example.games.basegameutils.BaseGameUtils;

/**
 * A bound service that provies access to one instance of {@link GoogleApiClient} across multiple
 * activities and handles its lifecycle. Activities binding to this service must call
 * {@link GPGSHelperBinder#onActivityResult(Activity, int, int)} from their
 * {@link Activity#onActivityResult(int, int, Intent)} methods.
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
  
  public GoogleApiClient getApiClient() {
    return client;
  }
  
  public boolean isConnected() {
    return client != null && client.isConnected();
  }
  
  public void tryAutoConnect(Activity activity) {
    // Auto-connect if the shared preferences say we should
    SharedPreferences prefs = getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE);
    if (prefs.getBoolean(Util.PREF_AUTO_SIGN_IN, true)) {
      connect(activity);
    }
  }
  
  public void connect(Activity activity) {
    trySignIn = true;
    currentActivity = activity;
    client.connect();
  }
  
  public void onActivityResult(Activity activity, int requestCode, int resultCode) {
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
      getApiClient().disconnect();
    }
  }
  
  public void setActionOnSignIn(Activity activity, GPGSAction action) {
    actionOnSignIn = action;
    currentActivity = activity;
  }
  
  private void setAutoSignIn(boolean autoSignIn) {
    SharedPreferences.Editor editor = getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE).edit();
    editor.putBoolean(Util.PREF_AUTO_SIGN_IN, autoSignIn);
    editor.apply();
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
      Log.d(TAG, "(Error message: " + result.getErrorMessage() + ")");
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
    actionOnSignIn.performAction(currentActivity, client);
    setAutoSignIn(true);
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