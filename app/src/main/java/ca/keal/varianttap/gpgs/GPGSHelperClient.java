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
  
}