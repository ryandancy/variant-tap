package ca.keal.varianttap.ui.circlebutton;

import android.content.Context;
import android.util.AttributeSet;

import ca.keal.varianttap.R;
import ca.keal.varianttap.gpgs.GPGSAction;

/**
 * A {@link GPGSCircleButton} which triggers {@link GPGSAction#ShowLeaderboard}. It consists of
 * the GPGS leaderboard icon (a crown).
 */
public class LeaderboardCircleButton extends GPGSCircleButton {
  
  public LeaderboardCircleButton(Context context) {
    this(context, null);
  }
  
  public LeaderboardCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.leaderboardCircleButtonStyle, GPGSAction.ShowLeaderboard);
  }
  
}