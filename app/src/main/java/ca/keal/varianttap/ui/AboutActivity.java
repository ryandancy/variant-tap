package ca.keal.varianttap.ui;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;

import ca.keal.varianttap.R;
import ca.keal.varianttap.gpgs.GPGSAction;
import ca.keal.varianttap.gpgs.GPGSCallback;
import ca.keal.varianttap.gpgs.GPGSHelperClient;
import ca.keal.varianttap.gpgs.GPGSHelperService;
import ca.keal.varianttap.gpgs.GPGSHelperServiceConnection;
import ca.keal.varianttap.ads.AdRemovalManager;
import ca.keal.varianttap.ads.EUConsentForm;
import ca.keal.varianttap.util.Util;

import static android.view.ViewTreeObserver.OnGlobalLayoutListener;

public class AboutActivity extends AppCompatActivity
    implements GPGSHelperClient, GPGSCallback, EUConsentForm.OnCloseListener {
  
  private static final String TAG = "AboutActivity";
  
  private GPGSHelperService gpgsHelper;
  private GPGSHelperServiceConnection connection;
  
  private TextView statusText;
  private TextView changeLink;
  private EUConsentForm euConsentForm = null;
  
  private Button signInOutButton;
  
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);
    
    connection = new GPGSHelperServiceConnection(this);
    
    statusText = findViewById(R.id.eu_consent_status);
    changeLink = findViewById(R.id.change_eu_consent);
    signInOutButton = findViewById(R.id.sign_in_out_button);
    
    signInOutButton.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      public void onGlobalLayout() {
        // Scale the controller icon and set it to signInOutButton's drawableStart
        // In OnGlobalLayoutListener because getHeight() calls must be made after layout
        
        float padding = getResources().getDimension(R.dimen.button_drawable_padding);
        float size = signInOutButton.getHeight() - padding;
        
        Drawable controllerIcon = ContextCompat.getDrawable(
            AboutActivity.this, R.drawable.controller);
  
        signInOutButton.setTextSize(Util.getLargestTextSize(
            signInOutButton,
            getString(R.string.gpgs_sign_out),
            signInOutButton.getWidth() - 2*padding - 1.25f*size,
            signInOutButton.getTextSize()
        ));
        
        // ContextCompat.getDrawable() is @Nullable even though it *should* never be null;
        // this is just to quiet the inspector.
        if (controllerIcon == null) {
          Log.wtf(TAG, "The controller icon was null??");
          throw new RuntimeException("The controller icon was null??");
        }
        
        controllerIcon.setColorFilter(ContextCompat.getColor(AboutActivity.this, R.color.textLight),
            PorterDuff.Mode.MULTIPLY);
        controllerIcon.setBounds(0, 0, (int) size, (int) size);
        signInOutButton.setCompoundDrawablesRelative(null, null, controllerIcon, null);
        centerButtonTextAndIcon();
        
        // Prevent this OnGlobalLayoutListener from being called multiple times
        signInOutButton.getViewTreeObserver().removeOnGlobalLayoutListener(this);
      }
    });
    
    setupLegalLinks();
    
    if (!AdRemovalManager.areAdsRemoved()) {
      ConsentInformation consentInfo = ConsentInformation.getInstance(this);
      if (consentInfo.isRequestLocationInEeaOrUnknown()) {
        setupConsentLink(consentInfo.getConsentStatus());
      }
    }
  }
  
  private void setupLegalLinks() {
    // Make privacy policy and terms of service links work
    TextView privacyPolicyLink = findViewById(R.id.privacy_policy_link);
    TextView termsLink = findViewById(R.id.terms_link);
    
    privacyPolicyLink.setText(Util.buildLink(
        getString(R.string.privacy_policy_url),
        getString(R.string.privacy_policy_link_text)));
    termsLink.setText(Util.buildLink(
        getString(R.string.terms_url),
        getString(R.string.terms_link_text)));
    
    privacyPolicyLink.setMovementMethod(LinkMovementMethod.getInstance());
    termsLink.setMovementMethod(LinkMovementMethod.getInstance());
  }
  
  private void setupConsentLink(ConsentStatus consentStatus) {
    selectStatusText(consentStatus);
    
    // Give the change link a hyperlink formatting, add its functionality
    changeLink.setText(Util.formatLink(getString(R.string.change_consent)));
    changeLink.setOnClickListener(v -> {
      // Spawn a dismissible EUConsentForm
      euConsentForm = new EUConsentForm(AboutActivity.this);
      euConsentForm.setOnCloseListener(AboutActivity.this);
      euConsentForm.show(true);
    });
    
    statusText.setVisibility(View.VISIBLE);
    changeLink.setVisibility(View.VISIBLE);
  }
  
  @Override
  public void onEUConsentFormClose() {
    if (AdRemovalManager.areAdsRemoved()) {
      statusText.setVisibility(View.GONE);
      changeLink.setVisibility(View.GONE);
    } else {
      selectStatusText(ConsentInformation.getInstance(this).getConsentStatus());
    }
  }
  
  private void selectStatusText(ConsentStatus consentStatus) {
    // Choose the status string based on the consent status
    @StringRes int statusStringId;
    switch (consentStatus) {
      case PERSONALIZED:
        statusStringId = R.string.consent_yes;
        break;
      case NON_PERSONALIZED:
        statusStringId = R.string.consent_no;
        break;
      case UNKNOWN:
      default: // compiler requires this case
        // how did this happen? this isn't supposed to happen.
        Log.e(TAG, "Consent status is unknown past consent dialog");
        statusStringId = R.string.consent_unknown;
    }
    statusText.setText(statusStringId);
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    if (gpgsHelper != null) {
      gpgsHelper.signInSilently(this);
    }
  }
  
  @Override
  protected void onStart() {
    super.onStart();
    
    // Bind to the GPGS service
    Intent intent = new Intent(this, GPGSHelperService.class);
    bindService(intent, connection, BIND_AUTO_CREATE);
  }
  
  @Override
  protected void onStop() {
    super.onStop();
    gpgsHelper.clearActionOnSignIn(this);
    unbindService(connection);
  }
  
  @Override
  protected void onDestroy() {
    if (euConsentForm != null) {
      euConsentForm.onDestroy();
    }
    
    super.onDestroy();
  }
  
  @Override
  public void receiveService(GPGSHelperService service) {
    gpgsHelper = service;
    gpgsHelper.setActionOnSignIn(this, GPGSAction.CallCallback);
    gpgsHelper.signInSilently(this);
  }
  
  public void signInOrOutOfGPGS(View v) {
    if (gpgsHelper.isSignedIn()) {
      gpgsHelper.signOut();
      updateButtonText();
    } else {
      gpgsHelper.setActionOnSignIn(this, GPGSAction.CallCallback);
      gpgsHelper.signIn(this);
    }
  }
  
  private void updateButtonText() {
    signInOutButton.setText(gpgsHelper.isSignedIn()
        ? R.string.gpgs_sign_out : R.string.gpgs_sign_in);
    centerButtonTextAndIcon();
  }
  
  /** Center the button's text and icon by manipulating the padding. */
  private void centerButtonTextAndIcon() {
    float iconPadding = getResources().getDimension(R.dimen.button_drawable_padding);
    
    float iconSize = signInOutButton.getHeight() - iconPadding;
    float iconSpacing = getResources().getDimension(R.dimen.button_drawable_spacing);
    float textWidth = signInOutButton.getPaint().measureText(signInOutButton.getText().toString());
    
    float totalSize = iconSize + iconSpacing + textWidth;
    float padding = (signInOutButton.getWidth() / 2f) - (totalSize / 2f);
    
    signInOutButton.setPaddingRelative(0, 0, (int) padding, 0);
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    gpgsHelper.onActivityResult(this, requestCode, resultCode, data);
  }
  
  @Override
  public void gpgsCallback() {
    updateButtonText();
    
    // There's an achievement for reading the about - unlock it
    // Note that at this point we're always signed in because that's when this method is called
    gpgsHelper.unlockAchievement(R.string.achievement_id_read_about);
  }
  
  @Override
  public void onBackPressed() {
    super.onBackPressed();
    Util.doTransition(this);
  }
  
}