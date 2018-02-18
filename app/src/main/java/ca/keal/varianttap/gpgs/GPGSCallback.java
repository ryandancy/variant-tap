package ca.keal.varianttap.gpgs;

/**
 * Used by {@link GPGSAction#CallCallback} to trigger a gpgsCallback on any activity.
 */
public interface GPGSCallback {
  
  /**
   * Called when {@link GPGSAction#CallCallback} is triggered.
   */
  void gpgsCallback();
  
}