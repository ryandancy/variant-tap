package ca.keal.varianttap;

import android.content.Context;

final class Util {
  
  private Util() {}
  
  public static float pxToDp(Context context, float px) {
    return px / context.getResources().getDisplayMetrics().density;
  }
  
  public static float dpToPx(Context context, float dp) {
    return dp * context.getResources().getDisplayMetrics().density;
  }
  
}