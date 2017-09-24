package ca.keal.varianttap;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

public class AboutCircleButton extends BaseCircleButton {
  
  public AboutCircleButton(Context context) {
    this(context, null);
  }
  
  public AboutCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.aboutCircleButtonStyle);
  }
  
  @Override
  protected void onClick() {
    Intent intent = new Intent(context, AboutActivity.class);
    context.startActivity(intent, Util.getActivityTransition(context));
  }
  
}