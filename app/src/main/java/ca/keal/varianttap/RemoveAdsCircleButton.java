package ca.keal.varianttap;

import android.content.Context;
import android.util.AttributeSet;

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