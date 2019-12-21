package ca.keal.varianttap.ads;

import android.annotation.SuppressLint;
import android.app.Activity;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ads.consent.AdProvider;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;

import ca.keal.varianttap.R;
import ca.keal.varianttap.util.Util;

/**
 * A consent form dialog. Mostly adapted from lenooh's StackOverflow answer at
 * https://stackoverflow.com/a/50556255
 */
public class EUConsentForm {
  
  private static final String TAG = "EUConsentForm";
  
  private final Activity activity;
  
  private OnCloseListener onCloseListener = null;
  
  private AlertDialog mainDialog = null;
  private AlertDialog moreInfoDialog = null;
  
  public EUConsentForm(Activity activity) {
    this.activity = activity;
  }
  
  /**
   * Show the dialog.
   * @param cancelable Whether the dialog is cancelable or not. Set to true if the user has already
   *                   indicated consent (i.e. in AboutActivity) or false if the user must indicate
   *                   consent to proceed (i.e. in MainActivity).
   */
  @SuppressLint("InflateParams")
  public void show(boolean cancelable) {
    // Inflating the layout here to modify it later
    View consentLayout = activity.getLayoutInflater().inflate(R.layout.eu_consent_dialog, null);
    
    // Build the dialog itself
    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
      .setView(consentLayout)
      .setCancelable(cancelable);
    
    if (cancelable) {
      dialogBuilder.setPositiveButton(R.string.close, null);
    }
    
    mainDialog = dialogBuilder.create();
    
    // Add click listeners
    Button buttonYes = consentLayout.findViewById(R.id.eu_consent_yes_button);
    Button buttonNo = consentLayout.findViewById(R.id.eu_consent_no_button);
    Button buttonRemoveAds = consentLayout.findViewById(R.id.eu_consent_remove_button);
    
    buttonYes.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.i(TAG, "User gave consent for personalized ads");
        close(ConsentStatus.PERSONALIZED, R.string.eu_consent_selected_yes);
      }
    });
    
    buttonNo.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Log.i(TAG, "User declined consent for personalized ads");
        close(ConsentStatus.NON_PERSONALIZED, R.string.eu_consent_selected_no);
      }
    });
    
    buttonRemoveAds.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        AdRemovalManager.setAdsRemoved();
        mainDialog.cancel(); // TODO cancel dialog only once they've actually bought the IAP
        if (onCloseListener != null) onCloseListener.onEUConsentFormClose();
      }
    });
    
    // Setup the "learn more" text as a link
    TextView learnMoreLink = consentLayout.findViewById(R.id.eu_consent_learn_more);
    learnMoreLink.setText(Util.formatLink(activity.getString(R.string.eu_consent_learn_more)));
    learnMoreLink.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showMoreInfo();
      }
    });
    
    mainDialog.show();
  }
  
  private void close(ConsentStatus newConsentStatus, @StringRes int toastText) {
    mainDialog.cancel();
    Toast.makeText(activity, toastText, Toast.LENGTH_LONG).show();
    ConsentInformation.getInstance(activity).setConsentStatus(newConsentStatus);
    if (onCloseListener != null) onCloseListener.onEUConsentFormClose();
  }
  
  public void setOnCloseListener(OnCloseListener listener) {
    onCloseListener = listener;
  }
  
  @SuppressWarnings("InflateParams")
  private void showMoreInfo() {
    View moreInfoRootLayout = activity.getLayoutInflater()
        .inflate(R.layout.eu_consent_more_info_dialog, null);
    
    ViewGroup moreInfoLayout = moreInfoRootLayout.findViewById(R.id.more_info_layout);
    TextView privacyPolicyLink = moreInfoLayout.findViewById(R.id.privacy_policy_link);
    
    // Make the privacy policy link a hyperlink
    privacyPolicyLink.setText(Html.fromHtml(buildLink(
        activity.getString(R.string.privacy_policy_url),
        activity.getString(R.string.privacy_policy_link_text))));
    privacyPolicyLink.setMovementMethod(LinkMovementMethod.getInstance());
    
    // Add a list of ad providers with whom AdMob shares data
    for (AdProvider adProvider : ConsentInformation.getInstance(activity).getAdProviders()) {
      TextView adProviderText = new TextView(activity);
      adProviderText.setTextSize(Util.pxToSp(activity, activity.getResources()
          .getDimensionPixelSize(R.dimen.text_body_size)));
      adProviderText.setText(Html.fromHtml("- " + buildLink(
          adProvider.getPrivacyPolicyUrlString(), adProvider.getName())));
      adProviderText.setMovementMethod(LinkMovementMethod.getInstance());
      moreInfoLayout.addView(adProviderText);
    }
    
    // Display it all in a dialog
    moreInfoDialog = new AlertDialog.Builder(activity)
      .setView(moreInfoRootLayout)
      .setPositiveButton(R.string.close, null)
      .create();
    moreInfoDialog.show();
  }
  
  private String buildLink(String url, String text) {
    return "<a href=\"" + url + "\">" + text + "</a>";
  }
  
  /**
   * Call in onDestroy() to prevent errors when rotating the screen.
   */
  public void onDestroy() {
    if (mainDialog != null && mainDialog.isShowing()) {
      mainDialog.cancel();
    }
    
    if (moreInfoDialog != null && moreInfoDialog.isShowing()) {
      moreInfoDialog.cancel();
    }
  }
  
  public interface OnCloseListener {
    void onEUConsentFormClose();
  }
  
}