package ca.keal.varianttap.ui.circlebutton;

import android.content.Context;
import android.util.AttributeSet;

import ca.keal.varianttap.R;
import ca.keal.varianttap.gpgs.GPGSAction;

/**
 * A {@link GPGSCircleButton} which triggers {@link GPGSAction#ShowAchievements}. It consists of
 * the GPGS achievements icon (a ribbon).
 */
public class AchievementsCircleButton extends GPGSCircleButton {
  
  public AchievementsCircleButton(Context context) {
    this(context, null);
  }
  
  public AchievementsCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.achievementsCircleButtonStyle, GPGSAction.ShowAchievements);
  }
  
}
