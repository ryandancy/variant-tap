package ca.keal.varianttap.ui.circlebutton;

import android.content.Context;
import android.util.AttributeSet;

import ca.keal.varianttap.R;

public class RemoveAdsCircleButton extends BaseCircleButton {
  
  public RemoveAdsCircleButton(Context context) {
    this(context, null);
  }
  
  public RemoveAdsCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.removeAdsCircleButtonStyle);
  }
  
  @Override
  protected void onClick() {
    // TODO: remove ads here, also (elsewhere) remove button if ads already removed
  }
  
}