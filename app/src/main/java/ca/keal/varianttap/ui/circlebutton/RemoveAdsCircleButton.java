package ca.keal.varianttap.ui.circlebutton;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import ca.keal.varianttap.R;
import ca.keal.varianttap.ads.AdRemovalManager;
import ca.keal.varianttap.ads.HasRemovableAds;

public class RemoveAdsCircleButton extends BaseCircleButton {
  
  public RemoveAdsCircleButton(Context context) {
    this(context, null);
  }
  
  public RemoveAdsCircleButton(Context context, AttributeSet attrs) {
    super(context, attrs, R.attr.removeAdsCircleButtonStyle);
  }
  
  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    checkAndMaybeRemove(); // automatically at startup
  }
  
  @Override
  protected void onClick() {
    // TODO make this an in-app purchase
    
    if (context instanceof HasRemovableAds) {
      // Remove this activity's ads
      ((HasRemovableAds) context).removeAds();
    }
    
    AdRemovalManager.setAdsRemoved();
    removeSelf(); // no need for a remove ads button anymore
  }
  
  /**
   * Check whether ads are removed. If so, remove this circle button.
   * This should be called in onResume() when ads could be removed in an activity deeper in the
   * activity stack.
   */
  public void checkAndMaybeRemove() {
    if (!AdRemovalManager.ENABLE_AD_REMOVAL || AdRemovalManager.areAdsRemoved()) {
      removeSelf();
    }
  }
  
  /** Remove this circle button from its layout. */
  private void removeSelf() {
    setVisibility(GONE);
    
    // Sometimes (i.e. in the circle button bar layout), there's a whole container to go along with
    // the circle button; remove that too
    View container = getRootView().findViewById(R.id.remove_ads_circle_button_container);
    if (container != null) {
      container.setVisibility(GONE);
    }
  }
  
}