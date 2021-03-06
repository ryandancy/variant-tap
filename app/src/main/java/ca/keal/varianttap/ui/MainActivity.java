package ca.keal.varianttap.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.consent.DebugGeography;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import ca.keal.varianttap.R;
import ca.keal.varianttap.ads.AdRemovalManager;
import ca.keal.varianttap.ads.EUConsentForm;
import ca.keal.varianttap.gpgs.GPGSHelperClient;
import ca.keal.varianttap.gpgs.GPGSHelperService;
import ca.keal.varianttap.gpgs.GPGSHelperServiceConnection;
import ca.keal.varianttap.ui.circlebutton.RemoveAdsCircleButton;
import ca.keal.varianttap.util.ImageSupplier;
import ca.keal.varianttap.util.LegalAgreementManager;
import ca.keal.varianttap.util.Util;

import static android.view.ViewTreeObserver.OnGlobalLayoutListener;

// Note: there is no app link logic here because nothing special needs to happen
public class MainActivity extends AppCompatActivity
    implements GPGSHelperClient, EUConsentForm.OnCloseListener {
  
  private static final String TAG = "MainActivity";
  
  // For image-throwing animation
  private Random random = null;
  
  private Runnable throwingRunnable;
  private Handler throwingHandler;
  private boolean throwFromLeft;
  private long msBetweenThrows;
  
  // For the achievement triggered after a certain amount of time staring at this activity
  private Timer timer;
  
  /** Contains all throw animations currently playing; used to pause and restart the animations. */
  private List<AnimatorSet> throwAnims;
  
  private GPGSHelperService gpgsHelper;
  private GPGSHelperServiceConnection connection;
  
  // It might need to be removed
  private RemoveAdsCircleButton removeAdsCircleButton;
  private EUConsentForm euConsentForm = null;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    // Start preloading the images
    ImageSupplier.getInstance(this).preload(getResources().getInteger(R.integer.images_to_preload));
    
    if (!AdRemovalManager.areAdsRemoved()) {
      MobileAds.initialize(this, getString(R.string.ad_banner_id));
    }
    
    removeAdsCircleButton = findViewById(R.id.remove_ads_circle_button);
    
    // Lock portrait orientation if we're not on a tablet
    if (getResources().getBoolean(R.bool.portrait_only)) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    
    // Set up periodic throwing animations
    
    throwFromLeft = false;
    msBetweenThrows = getResources().getInteger(R.integer.ms_between_throws);
    throwAnims = new ArrayList<>();
    
    throwingHandler = new Handler();
    throwingRunnable = new Runnable() {
      public void run() {
        throwImage(throwFromLeft);
        throwFromLeft = !throwFromLeft;
        throwingHandler.postDelayed(this, msBetweenThrows);
      }
    };
    
    // Set the title TextView's font size to exactly line up with the parent's border
    
    final ViewGroup layout = findViewById(R.id.main_layout);
    layout.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      public void onGlobalLayout() {
        // In OnGlobalLayoutListener so layout.getWidth() works properly
        TextView title = findViewById(R.id.main_title);
        title.setTextSize(Util.getLargestTextSize(
            title,
            title.getText().toString(),
            layout.getWidth() - 2 * getResources().getDimension(
                R.dimen.main_menu_title_side_padding),
            Util.pxToSp(MainActivity.this, getResources().getDimension(R.dimen.max_main_title_size))
        ));
        
        // Don't let this listener be called multiple times
        layout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
      }
    });
    
    connection = new GPGSHelperServiceConnection(this);
    
    if (!AdRemovalManager.areAdsRemoved()) {
      updateConsent();
    }
    
    getLegalAgreementIfNecessary();
  }
  
  private void updateConsent() {
    final ConsentInformation consentInfo = ConsentInformation.getInstance(this);
    String[] pubIds = getResources().getStringArray(R.array.ad_pub_ids);
    
    // Add test devices
    for (String testDeviceId : getResources().getStringArray(R.array.test_devices)) {
      consentInfo.addTestDevice(testDeviceId);
    }
    consentInfo.setDebugGeography(DebugGeography.DEBUG_GEOGRAPHY_EEA);
    
    consentInfo.requestConsentInfoUpdate(pubIds, new ConsentInfoUpdateListener() {
      @Override
      public void onConsentInfoUpdated(ConsentStatus consentStatus) {
        if (consentInfo.isRequestLocationInEeaOrUnknown()
            && consentStatus == ConsentStatus.UNKNOWN) {
          // Spawn a non-dismissible EUConsentForm
          euConsentForm = new EUConsentForm(MainActivity.this);
          euConsentForm.setOnCloseListener(MainActivity.this);
          euConsentForm.show(false);
        }
      }
      
      @Override
      public void onFailedToUpdateConsentInfo(String reason) {
        Log.e(TAG, "Consent info failed to update: " + reason);
      }
    });
  }
  
  @Override
  public void onEUConsentFormClose() {
    removeAdsCircleButton.checkAndMaybeRemove();
  }
  
  private void getLegalAgreementIfNecessary() {
    // Has the user already agreed to the legal stuff?
    LegalAgreementManager legalManager = new LegalAgreementManager(this);
    
    if (legalManager.hasUserAgreed()) {
      Log.i(TAG, "User already agreed to terms of service and privacy policy; skipping agreement dialog");
    } else {
      Log.i(TAG, "Launching dialog to get user agreement to terms of service and privacy policy");
      
      // Show a dialog to get them to agree
      AlertDialog dialog = new AlertDialog.Builder(this)
          .setTitle(R.string.legal_agreement_title)
          .setMessage(R.string.legal_agreement_message)
          .setPositiveButton(R.string.legal_agree, (dialog1, which) -> legalManager.agree())
          .setCancelable(false)
          .create();
      dialog.show();
      
      // Make the links clickable
      TextView dialogMsg = dialog.findViewById(android.R.id.message);
      dialogMsg.setMovementMethod(LinkMovementMethod.getInstance());
    }
  }
  
  @Override
  protected void onStart() {
    super.onStart();
    
    // Bind to GPGSHelperService
    Intent intent = new Intent(this, GPGSHelperService.class);
    bindService(intent, connection, BIND_AUTO_CREATE);
  }
  
  @Override
  protected void onStop() {
    super.onStop();
    unbindService(connection);
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    // Respond to out-of-app changes in sign-in status
    if (gpgsHelper != null) {
      gpgsHelper.signInSilently(this);
    }
    
    // Remove the 'remove ads' circle button if ads were removed later
    removeAdsCircleButton.checkAndMaybeRemove();
    
    // Resume all throw animations
    for (AnimatorSet anim : throwAnims) {
      if (anim.isPaused()) {
        anim.resume();
      }
    }
    
    // Start/restart adding more throwing animations
    throwingRunnable.run();
    
    // Start the achievement-granting timer
    Log.d(TAG, "Starting 'stare at animation' achievement timer");
    timer = new Timer("MainActivity achievement timer");
    timer.schedule(new GrantAchievementTask(),
        getResources().getInteger(R.integer.achievement_stare_animation_time_ms));
  }
  
  private class GrantAchievementTask extends TimerTask {
    @Override
    public void run() {
      gpgsHelper.unlockAchievement(R.string.achievement_id_stare_animation);
    }
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    
    // Don't add any more throw animations
    throwingHandler.removeCallbacks(throwingRunnable);
    
    // Pause all throw animations
    for (AnimatorSet anim : throwAnims) {
      if (anim.isRunning()) {
        anim.pause();
      }
    }
    
    // Cancel the achievement-granting timer because the user is no longer staring at the animation
    Log.d(TAG, "Stopping 'stare at animation' achievement timer");
    timer.cancel();
    timer = null; // so we don't have to do it in onDestroy()
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
    gpgsHelper.trySignIn(this);
  }
  
  /**
   * "Throw" (animate) a random image from the game across the screen. The image follows a parabolic
   * path across the screen and rotates. This is for a cool animation for the MainActivity.
   * @param invert if true, the image will be thrown right-to-left instead of left-to-right.
   */
  @SuppressWarnings("RtlHardcoded")
  private void throwImage(final boolean invert) {
    if (random == null) random = new Random();
    
    // Construct the ImageView to be thrown
    
    final ImageView image = new ImageView(this);
    final AnimatorSet throwAnim = new AnimatorSet(); // defined up here for anonymous inner classes
    
    int parent = R.id.throwing_layout;
    final ViewGroup parentLayout = findViewById(parent);
    
    int length = (int) getResources().getDimension(R.dimen.thrown_image_size);
    final FrameLayout.LayoutParams params
        = new FrameLayout.LayoutParams(length, length, Gravity.TOP | Gravity.LEFT);
    image.setLayoutParams(params);
    
    ImageSupplier supplier = ImageSupplier.getInstance(this);
    Pair<String, Drawable> nameAndDrawable = supplier.getRandomImage();
    image.setImageDrawable(nameAndDrawable.second);
    image.setAdjustViewBounds(true);
    image.setScaleType(ImageView.ScaleType.FIT_XY);
    
    // Start loading an image to replace this one
    supplier.preload(getResources().getInteger(R.integer.images_to_preload), nameAndDrawable.first);
    
    final float width = Util.getWidthDp(getResources());
    final float height = Util.getHeightDp(getResources());
    
    // Make tablets in landscape mode have values like they're in portrait mode
    float adjustmentWidth, adjustmentHeight;
    if (width > height) {
      // noinspection SuspiciousNameCombination
      adjustmentWidth = height;
      // noinspection SuspiciousNameCombination
      adjustmentHeight = width;
    } else {
      adjustmentWidth = width;
      adjustmentHeight = height;
    }
    
    // Multiplication/division by width/height is to convert bias "units" <-> dp
    
    final float gravity = Util.getFloatResource(this, R.dimen.thrown_image_gravity)
        * (adjustmentHeight / 1280); // adjustment factor to make it look good on all devices
    
    // Generate the difference between the starting and ending vertical positions (diffY)
    float minDiffY = Util.getFloatResource(this, R.dimen.min_thrown_image_diff_y) * height;
    float maxDiffY = Util.getFloatResource(this, R.dimen.max_thrown_image_diff_y) * height;
    float diffY = Util.randomFloatBetween(random, minDiffY, maxDiffY);
    
    // Generate the horizontal speed
    float minSpeedX = Util.getFloatResource(this, R.dimen.min_thrown_image_speed_x);
    float maxSpeedX = Util.getFloatResource(this, R.dimen.max_thrown_image_speed_x);
    final float speedX = Util.randomFloatBetween(random, minSpeedX, maxSpeedX)
        * (adjustmentWidth / 800); // adjustment factor to make it look good on all devices
    
    // Calculate the vertical speed + maximum height + total time
    // Derived via kinematics
    final float totalTime = width / speedX;
    final float initialSpeedY = diffY / totalTime - (gravity * totalTime) / 2;
    float maxY = (-(initialSpeedY * initialSpeedY) / (2 * gravity)) / height;
    
    // Get the maximum height possible
    float absoluteMaxY = Util.getFloatResource(this, R.dimen.max_thrown_image_vertical_bias);
    
    // Generate the distance from the maximum height possible to the maximum height of the image
    float minDiffMaxY = Util.getFloatResource(this, R.dimen.min_thrown_image_diff_max_y);
    float maxDiffMaxY = Util.getFloatResource(this, R.dimen.max_thrown_image_diff_max_y);
    float diffMaxY = Util.randomFloatBetween(random, minDiffMaxY, maxDiffMaxY);
    
    // Calculate the starting bias
    final float startBias = absoluteMaxY - diffMaxY - maxY;
    
    // Generate the number of times the image will rotate
    float minRotateTimes = Util.getFloatResource(this, R.dimen.min_thrown_image_rotate_times);
    float maxRotateTimes = Util.getFloatResource(this, R.dimen.max_thrown_image_rotate_times);
    float rotateTimes = Util.randomFloatBetween(random, minRotateTimes, maxRotateTimes)
        * (random.nextBoolean() ? 1 : -1);
    
    float initialRotation = Util.randomFloatBetween(random, 0, 360);
    
    Log.v(TAG, "Throwing image with startBias = " + startBias + " bias, x speed = " + speedX
        + " dp/s, initial y speed = " + initialSpeedY + " dp/s, max height = "
        + (absoluteMaxY - diffMaxY) + " bias, vertical difference = " + diffY
        + " dp, total time = " + totalTime + " s, rotating " + rotateTimes + " times");
    
    // Construct the parabola animator, which moves the image
    
    ValueAnimator parabolaAnimator = ValueAnimator.ofFloat(-0.2f, 1.2f);
    parabolaAnimator.setInterpolator(new LinearInterpolator());
    parabolaAnimator.addUpdateListener(animation -> {
      float value = (float) animation.getAnimatedValue();
      float time = totalTime * value;
      
      // d_x = v_x*t
      float x = speedX * time;
      
      // d_y = v_iy*t + 1/2 gt^2
      float dy = (initialSpeedY * time) + (gravity * time * time) / 2;
      
      float xBias = x / width;
      float yBias = startBias + (dy / height);
      
      xBias = invert ? 1f - xBias : xBias; // handle right-to-left
      
      params.leftMargin = (int) (parentLayout.getWidth() * xBias);
      params.topMargin = (int) (parentLayout.getHeight() * (1f - yBias));
      image.setLayoutParams(params);
    });
    parabolaAnimator.addListener(new Animator.AnimatorListener() {
      public void onAnimationStart(Animator animation) {}
      public void onAnimationRepeat(Animator animation) {}
      public void onAnimationCancel(Animator animation) {}
      public void onAnimationEnd(Animator animation) {
        parentLayout.removeView(image); // hopefully image is gone now...
        throwAnims.remove(throwAnim);
      }
    });
    
    // Construct the rotate animator, which rotates the image
    ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(
        image, ImageView.ROTATION, initialRotation, rotateTimes * 360 + initialRotation);
    // noinspection RedundantCast - it was casting to BaseInterpolator, only available with API 22
    rotateAnimator.setInterpolator(random.nextBoolean()
        ? (TimeInterpolator) new AccelerateInterpolator(0.5f)
        : (TimeInterpolator) new DecelerateInterpolator(0.5f));
    
    // Put them together and play
    throwAnim.play(parabolaAnimator).with(rotateAnimator);
    throwAnim.setDuration((long) (totalTime * 1.4 * 1000)); // *1.4 to account for -0.2 to 1.2
    throwAnims.add(throwAnim);
    
    parentLayout.addView(image, 0); // add at back, below all other elements
    throwAnim.start();
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    gpgsHelper.onActivityResult(this, requestCode, resultCode, data);
  }
  
}