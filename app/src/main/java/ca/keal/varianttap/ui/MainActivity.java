package ca.keal.varianttap.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ca.keal.varianttap.R;
import ca.keal.varianttap.gpgs.GPGSHelperService;
import ca.keal.varianttap.gpgs.GPGSHelperServiceClient;
import ca.keal.varianttap.gpgs.GPGSHelperServiceConnection;
import ca.keal.varianttap.util.ImageSupplier;
import ca.keal.varianttap.util.Util;

import static android.view.ViewTreeObserver.OnGlobalLayoutListener;

public class MainActivity extends AppCompatActivity implements GPGSHelperServiceClient {
  
  private static final String TAG = "MainActivity";
  
  // For image-throwing animation
  private Random random = null;
  
  private Runnable throwingRunnable;
  private Handler throwingHandler;
  private boolean throwFromLeft;
  private int msBetweenThrows;
  
  /** Contains all throw animations currently playing; used to pause and restart the animations. */
  private List<AnimatorSet> throwAnims;
  
  private GPGSHelperService gpgsHelper;
  private GPGSHelperServiceConnection connection;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
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
  
  @Override
  public void receiveService(GPGSHelperService service) {
    gpgsHelper = service;
    gpgsHelper.tryAutoConnect(this);
  }
  
  @Override
  public GPGSHelperService getService() {
    return gpgsHelper;
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
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    gpgsHelper.onActivityResult(this, requestCode, resultCode);
  }
  
}