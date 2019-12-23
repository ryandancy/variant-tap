package ca.keal.varianttap.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A simple class which handles storing the user's agreement to the terms of service and privacy policy.
 */
public class LegalAgreementManager {
  
  private static final String TAG = "LegalAgreementManager";
  
  // DO NOT CHANGE THESE
  private static final String LEGAL_AGREEMENT_SHARED_PREFS_FILE = "legal_agreement";
  private static final String PREF_HAS_AGREED = "hasAgreed";
  
  private SharedPreferences legalPrefs;
  
  public LegalAgreementManager(Context context) {
    legalPrefs = context.getSharedPreferences(LEGAL_AGREEMENT_SHARED_PREFS_FILE, Context.MODE_PRIVATE);
  }
  
  public boolean hasUserAgreed() {
    return legalPrefs.getBoolean(PREF_HAS_AGREED, false);
  }
  
  public void agree() {
    Log.i(TAG, "User agreed to terms and privacy policy");
    
    SharedPreferences.Editor editor = legalPrefs.edit();
    editor.putBoolean(PREF_HAS_AGREED, true);
    editor.apply();
  }
  
}