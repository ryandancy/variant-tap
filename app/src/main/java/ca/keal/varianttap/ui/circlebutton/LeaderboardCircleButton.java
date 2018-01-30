package ca.keal.varianttap.ui.circlebutton;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import ca.keal.varianttap.gpgs.GPGSAction;
import ca.keal.varianttap.gpgs.GPGSHelperService;
import ca.keal.varianttap.gpgs.GPGSHelperClient;
import ca.keal.varianttap.R;

public class LeaderboardCircleButton extends BaseCircleButton {
  
  private static final String TAG = "LeaderboardCircleButton";
  
  private GPGSHelperClient gpgsHelperClient;
  
  public LeaderboardCircleButton(Context context) {
    this(context, null);
  }
  
  public LeaderboardCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.leaderboardCircleButtonStyle);
    
    if (!(context instanceof GPGSHelperClient)) {
      Log.e(TAG, "Leaderboard circle button used without context being a GPGSHelperClient!");
      return;
    }
    
    gpgsHelperClient = (GPGSHelperClient) context;
  }
  
  @Override
  protected void onClick() {
    if (gpgsHelperClient == null) {
      Log.e(TAG, "Leaderboard circle button not in a GPGSHelperClient: ignoring click!");
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