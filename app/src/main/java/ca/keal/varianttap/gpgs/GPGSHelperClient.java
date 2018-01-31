package ca.keal.varianttap.gpgs;

/**
 * A client of {@link GPGSHelperService}. Can receive and store instances of
 * {@link GPGSHelperService}.
 */
public interface GPGSHelperClient {
  
  /**
   * Receive {@code service} and store it to be used later for API calls, etc.
   */
  void receiveService(GPGSHelperService service);
  
  /**
   * Return the service previously passed to {@link #receiveService(GPGSHelperService)}. This method
   * is guaranteed to be called after {@link #receiveService(GPGSHelperService)}.
   */
  GPGSHelperService getService();
  
}