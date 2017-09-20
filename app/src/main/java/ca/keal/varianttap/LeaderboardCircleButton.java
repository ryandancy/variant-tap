package ca.keal.varianttap;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

public class LeaderboardCircleButton extends BaseCircleButton {
  
  private static final String TAG = "LeaderboardCircleButton";
  
  private GPGSHelperServiceClient gpgsHelperClient;
  
  public LeaderboardCircleButton(Context context) {
    this(context, null);
  }
  
  public LeaderboardCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.leaderboardCircleButtonStyle);
    
    if (!(context instanceof GPGSHelperServiceClient)) {
      Log.e(TAG, "Leaderboard circle button used without context being a GPGSHelperServiceClient!");
      return;
    }
    
    gpgsHelperClient = (GPGSHelperServiceClient) context;
  }
  
  @Override
  protected void onClick() {
    if (gpgsHelperClient == null) {
      Log.e(TAG, "Leaderboard circle button not in a GPGSHelperServiceClient: ignoring click!");
      return;
    }
    
    if (!(context instanceof Activity)) {
      Log.e(TAG, "Why are you using LeaderboardCircleButton outside of an Activity? "
          + "Ignoring click!");
      return;
    }
    
    GPGSHelperService gpgsHelper = gpgsHelperClient.getService();
    Activity activity = (Activity) context;
    
    if (gpgsHelper.isConnected()) {
      GPGSAction.ShowLeaderboard.performAction(activity, gpgsHelper.getApiClient());
    } else {
      gpgsHelper.setActionOnSignIn(activity, GPGSAction.ShowLeaderboard);
      gpgsHelper.connect(activity);
    }
  }
  
}