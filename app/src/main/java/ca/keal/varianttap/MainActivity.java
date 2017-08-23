package ca.keal.varianttap;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.view.ViewTreeObserver.OnGlobalLayoutListener;

public class MainActivity extends AppCompatActivity {
  
  private Random random = null;
  
  private Runnable throwingRunnable;
  private Handler throwingHandler;
  private boolean throwFromLeft;
  private int msBetweenThrows;
  
  /** Contains all throw animations currently playing; used to pause and restart the animations. */
  private List<AnimatorSet> throwAnims;
  
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
    
    final ConstraintLayout layout = findViewById(R.id.main_layout);
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
  // TODO split up this huge method
  // TODO make images slide off to the side instead of disappearing at the edge of the screen
  private void throwImage(final boolean invert) {
    if (random == null) random = new Random();
    
    // Construct the ImageView to be thrown
    
    final ImageView image = new ImageView(this);
    final AnimatorSet throwAnim = new AnimatorSet(); // defined up here for anonymous inner classes
    
    int parent = R.id.main_layout;
    final ViewGroup parentLayout = findViewById(R.id.main_layout);
    
    int length = (int) getResources().getDimension(R.dimen.thrown_image_size);
    final ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(length, length);
    
    params.leftToLeft = parent;
    params.rightToRight = parent;
    params.topToTop = parent;
    params.bottomToBottom = parent;
    
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
    
    Log.d(getClass().getName(), "Throwing image with startBias = " + startBias + ", vertex = ("
        + vertexHorizBias + ", " + minBias + "), rotating " + rotateTimes + " times");
    Log.d(getClass().getName(),
        "Equation: y = " + factor + "(x - " + vertexHorizBias + ")^2 + " + minBias);
    
    // Construct the parabola animator, which moves the image
    
    ValueAnimator parabolaAnimator = ValueAnimator.ofFloat(0, 1); // TODO come from off left side
    parabolaAnimator.setInterpolator(new LinearInterpolator());
    parabolaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      public void onAnimationUpdate(ValueAnimator animation) {
        float xBias = (float) animation.getAnimatedValue();
        
        // Equation is y = a(x - h)^2 + k
        float yBias = factor * (float) Math.pow(xBias - vertexHorizBias, 2) + minBias;
        
        params.horizontalBias = invert ? 1f - xBias : xBias; // handle right-to-left
        params.verticalBias = yBias;
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
  
}