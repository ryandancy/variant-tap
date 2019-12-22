package ca.keal.varianttap.ads;

import android.util.Log;

/**
 * A simple utility class to keep track of whether ads are or are not removed.
 * There is no way to set ads as not removed after they have been set removed because there is no
 * need to do so. This class must be reset with the correct value on each startup.
 */
public final class AdRemovalManager {
  
  private static final String TAG = "AdRemovalManager";
  
  // TODO change this when we eventually enable ad removal
  public static final boolean ENABLE_AD_REMOVAL = false;
  
  private static boolean adsRemoved = false;
  
  private AdRemovalManager() {}
  
  public static boolean areAdsRemoved() {
    return ENABLE_AD_REMOVAL && adsRemoved;
  }
  
  public static void setAdsRemoved() {
    if (!ENABLE_AD_REMOVAL) {
      Log.w(TAG, "Tried to remove ads, but ad removal is disabled!");
      return;
    }
    Log.i(TAG, "Ads set as removed for this run");
    adsRemoved = true;
  }
  
}