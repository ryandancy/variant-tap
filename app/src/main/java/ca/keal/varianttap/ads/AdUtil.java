package ca.keal.varianttap.ads;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;

import ca.keal.varianttap.R;

public final class AdUtil {
  
  private static final String TAG = "AdUtil";
  
  private AdUtil() {}
  
  public static AdRequest getAdRequest(Context context) {
    AdRequest.Builder builder = new AdRequest.Builder();
    
    // Add the test devices - TODO remove this on release
    for (String testDeviceId : context.getResources().getStringArray(R.array.test_devices)) {
      builder.addTestDevice(testDeviceId);
    }
    
    // Serve non-personalized ads if the user's in the EEA and hasn't opted-in
    ConsentInformation consentInfo = ConsentInformation.getInstance(context);
    if (consentInfo.isRequestLocationInEeaOrUnknown()
        && (consentInfo.getConsentStatus() == ConsentStatus.NON_PERSONALIZED
            || consentInfo.getConsentStatus() == ConsentStatus.UNKNOWN)) {
      Log.d(TAG, "Requesting non-personalized ad");
      Bundle extras = new Bundle();
      extras.putString("npa", "1");
      builder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
    }
    
    return builder.build();
  }
  
}