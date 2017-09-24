package ca.keal.varianttap.gpgs;

/**
 * Used by {@link GPGSAction#HideSignInButton} to hide the sign-in button of any activity.
 * Implementors must be activities with sign-in buttons.
 */
public interface HasSignInButton {
  
  /**
   * Hide the sign-in button. If there is a sign-out button, show it.
   */
  void hideSignInButton();
  
}