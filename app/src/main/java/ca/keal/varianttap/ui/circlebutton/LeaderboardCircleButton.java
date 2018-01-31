package ca.keal.varianttap.ui.circlebutton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.util.Log;

import ca.keal.varianttap.R;
import ca.keal.varianttap.gpgs.GPGSAction;
import ca.keal.varianttap.gpgs.GPGSHelperClient;
import ca.keal.varianttap.gpgs.GPGSHelperService;
import ca.keal.varianttap.gpgs.GPGSHelperServiceConnection;

public class LeaderboardCircleButton extends BaseCircleButton implements GPGSHelperClient {
  
  private static final String TAG = "LeaderboardCircleButton";
  
  private GPGSHelperService gpgsHelper;
  private GPGSHelperServiceConnection connection;
  
  public LeaderboardCircleButton(Context context) {
    this(context, null);
  }
  
  public LeaderboardCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.leaderboardCircleButtonStyle);
    
    if (!(context instanceof GPGSHelperClient)) {
      Log.e(TAG, "Leaderboard circle button used without context being a GPGSHelperClient!");
      return;
    }
    
    connection = new GPGSHelperServiceConnection(this);
  }
  
  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    
    // Bind to the GPGS service
    Intent intent = new Intent(context, GPGSHelperService.class);
    context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
  }
  
  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    context.unbindService(connection);
  }
  
  @Override
  public void receiveService(GPGSHelperService service) {
    gpgsHelper = service;
  }
  
  @Override
  protected void onClick() {
    if (!(context instanceof Activity)) {
      Log.e(TAG, "Why are you using LeaderboardCircleButton outside of an Activity? "
          + "Ignoring click!");
      return;
    }
    
    Activity activity = (Activity) context;
    
    if (gpgsHelper.isConnected()) {
      GPGSAction.ShowLeaderboard.performAction(activity, gpgsHelper.getApiClient());
    } else {
      gpgsHelper.setActionOnSignIn(activity, GPGSAction.ShowLeaderboard);
      gpgsHelper.connect(activity);
    }
  }
  
}