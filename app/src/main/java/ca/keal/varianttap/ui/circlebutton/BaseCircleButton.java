package ca.keal.varianttap.ui.circlebutton;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.support.annotation.AttrRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.View;

import ca.keal.varianttap.R;

abstract class BaseCircleButton extends AppCompatImageButton implements View.OnClickListener {
  
  protected Context context;
  protected int baseColor;
  
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
  
    TypedArray a = context.getTheme().obtainStyledAttributes(
        attrs, R.styleable.BaseCircleButton, 0, 0);
    
    try {
      baseColor = a.getColor(R.styleable.BaseCircleButton_baseColor,
          ContextCompat.getColor(context, R.color.circleButtonColor));
    } finally {
      a.recycle();
    }
  
    getDrawable().setColorFilter(baseColor, PorterDuff.Mode.MULTIPLY);
    getBackground().setColorFilter(baseColor, PorterDuff.Mode.MULTIPLY);
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    // Horrible hack to fix a bug where base colours would carry over
    super.onDraw(canvas);
    getDrawable().setColorFilter(baseColor, PorterDuff.Mode.MULTIPLY);
  }
  
  @Override
  public void onClick(View v) {
    onClick(); // just a nicer signature
  }
  
  protected abstract void onClick();
  
}