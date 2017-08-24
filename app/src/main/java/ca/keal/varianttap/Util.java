package ca.keal.varianttap;

import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DimenRes;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import java.util.Random;

@SuppressWarnings({"unused", "WeakerAccess"})
final class Util {
  
  private static final String TAG = "Util";
  
  private Util() {}
  
  static float pxToDp(Context context, float px) {
    return pxToDp(context.getResources(), px);
  }
  
  static float pxToDp(Resources resources, float px) {
    return px / resources.getDisplayMetrics().density;
  }
  
  static float dpToPx(Context context, float dp) {
    return dpToPx(context.getResources(), dp);
  }
  
  static float dpToPx(Resources resources, float dp) {
    return dp * resources.getDisplayMetrics().density;
  }
  
  static float spToPx(Context context, float sp) {
    return spToPx(context.getResources(), sp);
  }
  
  static float spToPx(Resources resources, float sp) {
    return sp * resources.getDisplayMetrics().scaledDensity;
  }
  
  static float pxToSp(Context context, float px) {
    return pxToSp(context.getResources(), px);
  }
  
  static float pxToSp(Resources resources, float px) {
    return px / resources.getDisplayMetrics().scaledDensity;
  }
  
  static float getFloatResource(Context context, @DimenRes int floatRes) {
    TypedValue floatValue = new TypedValue();
    context.getResources().getValue(floatRes, floatValue, true);
    return floatValue.getFloat();
  }
  
  /**
   * @return A random float {@code n}, where {@code min <= n <= max}, determined by {@code random}.
   */
  static float randomFloatBetween(Random random, float min, float max) {
    return (max - min) * random.nextFloat() + min;
  }
  
  /**
   * @return The largest text size that will make {@code text} in {@code textView} take up less than
   * or equal to {@code maxPxWidth px}, using a binary search with maximum {@code max}, in sp.
   */
  static float getLargestTextSize(TextView textView, String text, float maxPxWidth, float max) {
    float low = 0;
    float high = max;
    float size;
    
    while (low < high) {
      size = (int) ((low + high) / 2);
      float width = measureText(textView, text, size);
      float widthPlus1 = measureText(textView, text, size + 1);
      
      if (width <= maxPxWidth && widthPlus1 > maxPxWidth) {
        // optimal width
        Log.d(TAG, "getLargestTextSize() setting \"" + text + "\" size to " + size + "sp");
        return size;
      }
      
      if (width < maxPxWidth && widthPlus1 <= maxPxWidth) {
        // too low
        low = size + 1;
      }
      
      if (width > maxPxWidth) {
        // too high
        high = size - 1;
      }
    }
    
    Log.w(TAG, "getLargestTextSize() binary search could not complete: check arguments");
    return max; // maybe we're on a tablet or something?
  }
  
  private static float measureText(TextView textView, String text, float size) {
    TextPaint paint = textView.getPaint();
    paint.setTextSize(spToPx(textView.getResources(), size));
    return paint.measureText(text);
  }
  
  static Bundle getToLeftTransition(Context context) {
    return ActivityOptions.makeCustomAnimation(
        context, R.anim.slide_in_left, R.anim.slide_out_right).toBundle();
  }
  
  static Bundle getToRightTransition(Context context) {
    return ActivityOptions.makeCustomAnimation(
        context, R.anim.slide_in_right, R.anim.slide_out_left).toBundle();
  }
  
  static ValueAnimator getCountdownAnimator(
      long duration, final TextView text, final CountdownEndListener endListener) {
    ValueAnimator animator = ValueAnimator.ofFloat(3f, 0f);
    animator.setInterpolator(new LinearInterpolator());
    animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      public void onAnimationUpdate(ValueAnimator anim) {
        int countdownNum = (int) Math.ceil((float) anim.getAnimatedValue());
        
        if (countdownNum == 0) {
          // The animation's finished
          endListener.onEnd();
        } else {
          text.setText(String.valueOf(countdownNum));
        }
      }
    });
    animator.setDuration(duration);
    return animator;
  }
  
  interface CountdownEndListener {
    void onEnd();
  }
  
}