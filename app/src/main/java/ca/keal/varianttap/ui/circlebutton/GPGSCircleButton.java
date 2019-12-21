package ca.keal.varianttap.ui.circlebutton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.AttrRes;
import android.util.AttributeSet;
import android.util.Log;

import ca.keal.varianttap.gpgs.GPGSAction;
import ca.keal.varianttap.gpgs.GPGSHelperClient;
import ca.keal.varianttap.gpgs.GPGSHelperService;
import ca.keal.varianttap.gpgs.GPGSHelperServiceConnection;

/**
 * The abstract superclass of the circle buttons which are just shortcuts to launch a
 * {@link GPGSAction} ({@link LeaderboardCircleButton} and {@link AchievementsCircleButton}.)
 */
abstract class GPGSCircleButton extends BaseCircleButton implements GPGSHelperClient {
  
  private static final String TAG = "GPGSCircleButton";
  
  private GPGSHelperService gpgsHelper;
  private GPGSHelperServiceConnection connection;
  
  /** The {@link GPGSAction} performed upon tapping the {@link GPGSCircleButton}. */
  private final GPGSAction gpgsAction;
  
  public GPGSCircleButton(Context context, AttributeSet attrs, @AttrRes int style,
                          GPGSAction action) {
    super(context, attrs, style);
    connection = new GPGSHelperServiceConnection(this);
    gpgsAction = action;
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
      Log.e(TAG, "A GPGSCircleButton can only be used inside an activity. Ignoring click!");
      return;
    }
    gpgsHelper.tryActionOrConnect((Activity) context, gpgsAction);
  }
  
}