package ca.keal.varianttap;

import android.content.Context;
import android.support.annotation.DimenRes;
import android.util.TypedValue;

import java.util.Random;

final class Util {
  
  private Util() {}
  
  static float pxToDp(Context context, float px) {
    return px / context.getResources().getDisplayMetrics().density;
  }
  
  static float dpToPx(Context context, float dp) {
    return dp * context.getResources().getDisplayMetrics().density;
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
  
}