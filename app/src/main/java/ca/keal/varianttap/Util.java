package ca.keal.varianttap;

import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.DimenRes;
import android.util.TypedValue;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import java.util.Random;

final class Util {
  
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