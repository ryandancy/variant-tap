package ca.keal.varianttap.ui.circlebutton;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;

import ca.keal.varianttap.ui.AboutActivity;
import ca.keal.varianttap.R;
import ca.keal.varianttap.util.Util;

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