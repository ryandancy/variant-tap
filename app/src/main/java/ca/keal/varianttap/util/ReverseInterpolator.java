package ca.keal.varianttap.util;

import android.view.animation.Interpolator;

/**
 * Reverses the interpolation of whatever interpolator it's passed.
 * From https://stackoverflow.com/a/28459276.
 */
public class ReverseInterpolator implements Interpolator {
  
  private final Interpolator delegate;
  
  public ReverseInterpolator(Interpolator delegate) {
    this.delegate = delegate;
  }
  
  @Override
  public float getInterpolation(float input) {
    return 1f - delegate.getInterpolation(input);
  }
  
  public Interpolator getDelegate() {
    return delegate;
  }
  
}