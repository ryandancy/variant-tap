package ca.keal.varianttap;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.example.games.basegameutils.GameHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.view.ViewTreeObserver.OnGlobalLayoutListener;

// TODO move all circle buttons to fragments
public class MainActivity extends AppCompatActivity implements GameHelper.GameHelperListener {
  
  private static final String TAG = "MainActivity";
  
  // For circle buttons
  private boolean soundOn;
  
  // For image-throwing animation
  private Random random = null;
  
  private Runnable throwingRunnable;
  private Handler throwingHandler;
  private boolean throwFromLeft;
  private int msBetweenThrows;
  
  /** Contains all throw animations currently playing; used to pause and restart the animations. */
  private List<AnimatorSet> throwAnims;
  
  /** The interface to the Google Play game services API. */
  private GameHelper gameHelper;
  
  /** Actions that can be performed with Google Play games services. */
  private enum GPGSAction {
    ShowLeaderboard() {
      @Override
      public void performAction(Activity activity, GameHelper gameHelper) {
        super.performAction(activity, gameHelper);
        activity.startActivityForResult(
            Games.Leaderboards.getAllLeaderboardsIntent(gameHelper.getApiClient()),
            Util.REQUEST_LEADERBOARD);
      }
    },
    Nothing;
    
    private static final String TAG = "GPGSAction";
    
    public void performAction(Activity activity, GameHelper gameHelper) {
      Log.d(TAG, "GPGS action " + this + " performed in " + activity.getLocalClassName());
    }
  }
  
  private GPGSAction actionOnSignIn = GPGSAction.Nothing;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    // Make all the circle button icons circleButtonColor instead of white
    
    int circleButtonColor = ContextCompat.getColor(this, R.color.circleButtonColor);
    ViewGroup circleButtons = findViewById(R.id.circle_buttons);
    
    for (int i = 0; i < circleButtons.getChildCount(); i++) {
      View child = circleButtons.getChildAt(i);
      if (!(child instanceof ImageButton)) return;
      
      ImageButton circleButton = (ImageButton) child;
      circleButton.getDrawable().setColorFilter(circleButtonColor, PorterDuff.Mode.MULTIPLY);
    }
    
    // Update sound circle button
    
    SharedPreferences prefs = getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE);
    soundOn = prefs.getBoolean(Util.PREF_SOUND_ON, true); // sound on by default
    
    ImageButton soundBtn = findViewById(R.id.toggle_sound_btn);
    if (soundOn) {
      toggleSoundButtonOn(soundBtn);
    } else {
      toggleSoundButtonOff(soundBtn);
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
    
    // Setup the Google Play games services helper
    if (gameHelper == null) {
      gameHelper = new GameHelper(this, GameHelper.CLIENT_GAMES);
    }
    gameHelper.enableDebugLog(true);
    gameHelper.setMaxAutoSignInAttempts(1);
    gameHelper.setup(this);
  }
  
  @Override
  protected void onStart() {
    super.onStart();
    gameHelper.onStart(this);
  }
  
  @Override
  protected void onStop() {
    super.onStop();
    gameHelper.onStop();
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    // Resume all throw animations
    for (AnimatorSet anim : throwAnims) {
      if (anim.isPaused()) {
        anim.resume();
      }
    }
    
    // Start/restart adding more throwing animations
    throwingRunnable.run();
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
    
    Drawable drawable = ImageSupplier.getInstance(this).getRandomImage();
    image.setImageDrawable(drawable);
    image.setAdjustViewBounds(true);
    image.setScaleType(ImageView.ScaleType.FIT_XY);
    
    // Generate the starting and minimum vertical biases
    // Starting bias is the position at x=0, min bias is the min of the parabola
    
    float minVertBias = Util.getFloatResource(this, R.dimen.min_thrown_image_vertical_bias);
    float maxVertBias = Util.getFloatResource(this, R.dimen.max_thrown_image_vertical_bias);
    float minBiasDiff = Util.getFloatResource(this, R.dimen.min_start_min_thrown_image_bias_diff);
    
    float startBias = Util.randomFloatBetween(random, minVertBias + minBiasDiff, maxVertBias);
    final float minBias = Util.randomFloatBetween(random, minVertBias, startBias - minBiasDiff);
    
    // Generate the vertex horizontal bias - the horizontal bias at the max vertical bias (the AoS)
    
    float minHorizBias = Util.getFloatResource(this, R.dimen.min_thrown_image_horizontal_bias);
    float maxHorizBias = Util.getFloatResource(this, R.dimen.max_thrown_image_horizontal_bias);
    
    final float vertexHorizBias = Util.randomFloatBetween(random, minHorizBias, maxHorizBias);
    
    // Equation of the parabola is y = a(x - h)^2 + k where (h, k) is the vertex
    // Find a (factor) by subbing in (0, b) - the y-intercept
    // Rearranges to a = (b - k) / h^2
    final float factor = (startBias - minBias) / (float) Math.pow(vertexHorizBias, 2);
    
    // Generate the number of times the image will rotate
    
    float minRotateTimes = Util.getFloatResource(this, R.dimen.min_thrown_image_rotate_times);
    float maxRotateTimes = Util.getFloatResource(this, R.dimen.max_thrown_image_rotate_times);
    float rotateTimes = Util.randomFloatBetween(random, minRotateTimes, maxRotateTimes);
    
    float initialRotation = Util.randomFloatBetween(random, 0, 360);
    
    Log.v(TAG, "Throwing image with startBias = " + startBias + ", vertex = ("
        + vertexHorizBias + ", " + minBias + "), rotating " + rotateTimes + " times");
    
    // Construct the parabola animator, which moves the image
    
    ValueAnimator parabolaAnimator = ValueAnimator.ofFloat(-0.2f, 1.2f);
    parabolaAnimator.setInterpolator(new LinearInterpolator());
    parabolaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      public void onAnimationUpdate(ValueAnimator animation) {
        float xBias = (float) animation.getAnimatedValue();
        
        // Equation is y = a(x - h)^2 + k
        float yBias = factor * (float) Math.pow(xBias - vertexHorizBias, 2) + minBias;
        
        xBias = invert ? 1f - xBias : xBias; // handle right-to-left
        
        params.leftMargin = (int) (parentLayout.getWidth() * xBias);
        params.topMargin = (int) (parentLayout.getHeight() * yBias);
        image.setLayoutParams(params);
      }
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
    throwAnim.setDuration(getResources().getInteger(R.integer.image_throw_duration));
    throwAnims.add(throwAnim);
    
    parentLayout.addView(image, 0); // add at back, below all other elements
    throwAnim.start();
  }
  
  public void toggleSound(View view) {
    soundOn = !soundOn;
    
    ImageButton btn = (ImageButton) view;
    if (soundOn) {
      toggleSoundButtonOn(btn);
    } else {
      toggleSoundButtonOff(btn);
    }
    
    // Write the sound to the SharedPreferences
    SharedPreferences.Editor editPrefs = getSharedPreferences(Util.PREF_FILE, MODE_PRIVATE).edit();
    editPrefs.putBoolean(Util.PREF_SOUND_ON, soundOn);
    editPrefs.apply();
  }
  
  private void toggleSoundButtonOn(ImageButton btn) {
    int circleButtonColor = ContextCompat.getColor(this, R.color.circleButtonColor);
    btn.getDrawable().setColorFilter(circleButtonColor, PorterDuff.Mode.MULTIPLY);
    btn.setBackgroundResource(R.drawable.circle);
    btn.setContentDescription(getString(R.string.sound_on_desc));
  }
  
  private void toggleSoundButtonOff(ImageButton btn) {
    int disabledColor = ContextCompat.getColor(this, R.color.circleButtonDisabled);
    btn.getDrawable().setColorFilter(disabledColor, PorterDuff.Mode.MULTIPLY);
    btn.setBackgroundResource(R.drawable.circle_disabled);
    btn.setContentDescription(getString(R.string.sound_off_desc));
  }
  
  public void toAbout(View v) {
    Intent intent = new Intent(this, AboutActivity.class);
    startActivity(intent, Util.getToLeftTransition(this));
  }
  
  public void toLeaderboard(View v) {
    if (gameHelper.isSignedIn()) { // connected?
      GPGSAction.ShowLeaderboard.performAction(this, gameHelper);
    } else {
      actionOnSignIn = GPGSAction.ShowLeaderboard;
      gameHelper.beginUserInitiatedSignIn();
    }
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    gameHelper.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == Util.REQUEST_LEADERBOARD
        && resultCode == GamesActivityResultCodes.RESULT_RECONNECT_REQUIRED) {
      Log.d(TAG, "User signed out from within default UI: disconnecting");
      
      // Fix weird GameHelper bug with connection state and isSignedIn()
      gameHelper.disconnect();
      
      // Don't automatically sign the user in again
      gameHelper.setConnectOnStart(false);
    }
  }
  
  @Override
  public void onSignInFailed() {
    if (gameHelper.hasSignInError()) {
      gameHelper.showFailureDialog();
    }
  }
  
  @Override
  public void onSignInSucceeded() {
    actionOnSignIn.performAction(this, gameHelper);
    actionOnSignIn = GPGSAction.Nothing;
  }
  
}