package ca.keal.varianttap.ui.circlebutton;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.View;

import ca.keal.varianttap.R;

abstract class BaseCircleButton extends AppCompatImageButton implements View.OnClickListener {
  
  protected Context context;
  
  public BaseCircleButton(Context context) {
    this(context, null);
  }
  
  public BaseCircleButton(Context context, AttributeSet attrs) {
    this(context, attrs, R.attr.circleButtonStyle);
  }
  
  public BaseCircleButton(Context context, AttributeSet attrs, @AttrRes int style) {
    super(context, attrs, style);
    this.context = context;
    setOnClickListener(this);
  }
  
  @Override
  public void onClick(View v) {
    onClick(); // just a nicer signature
  }
  
  protected abstract void onClick();
  
}