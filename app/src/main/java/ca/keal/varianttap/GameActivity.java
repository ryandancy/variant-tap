package ca.keal.varianttap;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.github.lzyzsd.circleprogress.DonutProgress;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {
  
  /**  The ID/index of the variant ImageSwitcher. */
  private int variantId;
  
  /** Which round we're on. */
  private int round;
  private int difficulty;
  
  /** The amount that would be added to the score if the user tapped the variant right now. */
  private int scoreForRound;
  private int score;
  
  /** Whether to process the user's taps on the images. */
  private boolean allowImgTaps;
  
  /** The array of images in imgsGrid. */
  private ImageSwitcher[] imgs;
  
  /** The circle at the top of the screen that counts down time and also shows score. */
  private DonutProgress countdownCircle;
  private ValueAnimator countdownAnim;
  private ObjectAnimator resetCountdownAnim;
  
  private TextView scoreText;
  
  private ImageSupplier imgSupplier;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
    
    // Initialize everything
  
    imgSupplier = new ImageSupplier(getAssets());
    round = 0;
    score = 0;
    allowImgTaps = false;
    
    countdownCircle = (DonutProgress) findViewById(R.id.countdown_circle);
    scoreText = (TextView) findViewById(R.id.score_text);
    
    countdownAnim = (ValueAnimator) AnimatorInflater.loadAnimator(this, R.animator.countdown);
    countdownAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      public void onAnimationUpdate(ValueAnimator animation) {
        float progress = (float) animation.getAnimatedValue();
        countdownCircle.setProgress(progress);
        
        // round to nearest step
        int step = getResources().getInteger(R.integer.score_step);
        scoreForRound = Math.round(progress / step) * step;
        countdownCircle.setText(String.valueOf(scoreForRound));
      }
    });
    countdownAnim.addListener(new Animator.AnimatorListener() {
      // The whole thing with canceled is to not lose on a canceled animation, which happens every
      // time the user taps the variant.
      private boolean canceled = false;
      
      public void onAnimationCancel(Animator animation) {
        canceled = true;
      }
      
      public void onAnimationEnd(Animator animation) {
        if (canceled) {
          canceled = false;
        } else {
          onLose();
        }
      }
      
      public void onAnimationStart(Animator animation) {}
      public void onAnimationRepeat(Animator animation) {}
    });
    
    resetCountdownAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(this,
        R.animator.reset_countdown);
    resetCountdownAnim.setTarget(countdownCircle);
    
    resetCountdownAnim.addListener(new Animator.AnimatorListener() {
      public void onAnimationCancel(Animator animation) {}
      public void onAnimationRepeat(Animator animation) {}
      public void onAnimationStart(Animator animation) {}
      
      public void onAnimationEnd(Animator animation) {
        onResetAnimationEnd();
      }
    });
    
    // There are 3 difficulties: 0 (easy), 1 (normal) and 2 (hard). Each successive difficulty has
    // a higher number of images: easy has 4 images, normal has 6, and hard has 9.
    
    // Get the difficulty from the Intent
    difficulty = getIntent().getIntExtra("DIFFICULTY", -1);
    if (difficulty == -1) { // no difficulty extra in the intent
      Log.e(getClass().getName(), "Intent did not have \"DIFFICULTY\" extra!");
    }
    
    // Initialize the grid of images
    
    GridLayout imgsGrid = (GridLayout) findViewById(R.id.imgs_grid);
    
    // Calculate rows/columns
    int rows, columns;
    switch (difficulty) {
      case 0: // easy
        rows = 2;
        columns = 2;
        break;
      case 1: // normal
        rows = 2;
        columns = 3;
        break;
      case 2: // hard
        rows = 3;
        columns = 3;
        break;
      default:
        Log.e(getClass().getName(), "Passed unknown difficulty: " + difficulty);
        rows = 0;
        columns = 0;
    }
    
    Log.i(getClass().getName(), "Difficulty " + difficulty + ", setting grid to "
        + rows + "x" + columns);
    
    imgsGrid.setRowCount(rows);
    imgsGrid.setColumnCount(columns);
    
    // Add ImageSwitchers to imgsGrid
    
    int numImgs = rows * columns;
    imgs = new ImageSwitcher[numImgs];
    
    // Create the animations outside the loop so as not to load them multiple times
    // The out animation is just the in animation reversed, so we use ReverseInterpolator.
    Animation in = AnimationUtils.loadAnimation(this, R.anim.grow_in);
    Animation out = AnimationUtils.loadAnimation(this, R.anim.grow_in);
    out.setInterpolator(new ReverseInterpolator(out.getInterpolator()));
    
    for (int i = 0; i < numImgs; i++) {
      // Construct the ImageSwitcher and add it to the grid
      
      GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
      GridLayout.Spec columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
      
      GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
      params.setGravity(Gravity.FILL);
      
      int margin = (int) getResources().getDimension(R.dimen.grid_spacing);
      params.setMargins(margin, margin, margin, margin);
      
      imgs[i] = new ImageSwitcher(this);
      imgs[i].setLayoutParams(params);
      
      // The factory is to provide correctly formatted ImageViews for the ImageSwitcher
      imgs[i].setFactory(new ViewSwitcher.ViewFactory() { // no lambda expressions *sigh*
        public View makeView() {
          ImageView imgView = new ImageView(GameActivity.this);
          imgView.setAdjustViewBounds(true);
          imgView.setScaleType(ImageView.ScaleType.FIT_XY);
          return imgView;
        }
      });
      
      imgs[i].setInAnimation(in);
      imgs[i].setOutAnimation(out);
      
      imgs[i].setId(i); // for determining which is the variant; equal to the index
      imgs[i].setOnClickListener(this);
      
      imgsGrid.addView(imgs[i]);
    }
    
    startRound();
  }
  
  /**
   * Go to the next round; this involves starting the reset countdown animation. This
   * is BEFORE the reset animation ends.
   */
  private void nextRound() {
    allowImgTaps = false;
    addToScore(scoreForRound);
    
    countdownAnim.cancel();
    countdownCircle.setText(String.valueOf(scoreForRound));
    
    resetCountdownAnim.setFloatValues(countdownCircle.getProgress(), countdownCircle.getMax());
    resetCountdownAnim.setDuration(getResetCountdownAnimationDuration(countdownCircle));
    resetCountdownAnim.start();
  }
  
  /**
   * Calculate the duration of the reset countdown animation with a constant speed.
   * countdownCircle is a parameter so that it's a pure function.
   */
  private long getResetCountdownAnimationDuration(DonutProgress countdownCircle) {
    int minDuration = getResources().getInteger(R.integer.reset_countdown_min_ms);
    int maxDuration = getResources().getInteger(R.integer.reset_countdown_max_ms);
    
    float progressPct = countdownCircle.getProgress() / countdownCircle.getMax();
    float pctToGo = 1 - progressPct;
    
    float duration = pctToGo * maxDuration;
    
    return (long) Math.max(duration, minDuration);
  }
  
  /**
   * Wait for a certain amount of time, then call startRound().
   */
  private void onResetAnimationEnd() {
    countdownCircle.setText(String.valueOf(getMaxScoreForRound(round)));
    startRound();
  }
  
  /**
   * Begin the next round; this involves updating the images, starting animations, and so on.
   */
  private void startRound() {
    updateImages();
    
    int maxScore = getMaxScoreForRound(round);
    countdownCircle.setMax(maxScore);
    countdownCircle.setProgress(maxScore);
    
    countdownAnim.setFloatValues(maxScore, 0);
    countdownAnim.setDuration(getTimeForRoundMillis(round));
    countdownAnim.start();
    
    allowImgTaps = true;
    
    round++;
  }
  
  private int getMaxScoreForRound(int round) {
    // Linear - TODO ~5-round "steps" with accompanying fade-out animated text like "+50"
    int minScore = getResources().getInteger(R.integer.min_score);
    int scorePerRound = getResources().getInteger(R.integer.score_per_round);
    
    return round * scorePerRound + minScore;
  }
  
  private int getTimeForRoundMillis(int round) {
    // Start out slowly decreasing, speed up, then slow down
    int initial = getResources().getInteger(R.integer.initial_time_ms);
    int min = getResources().getInteger(R.integer.min_time_ms);
    int roundsAtMin = getResources().getInteger(R.integer.min_time_rounds);
    
    if (round >= roundsAtMin) {
      return min;
    }
    
    double a = (double) (initial - min) / 2.0;
    double b = (double) min;
    double c = 1.0 / (double) roundsAtMin;

    // See https://www.desmos.com/calculator/93igcfor0b
    double time = a * Math.cos(c * round * Math.PI) + a + b;
    return (int) time;
  }
  
  /**
   * Update the ImagesSwitchers with new images. One of the ImageSwitchers has a variant image, the
   * others have the corresponding normal image.
   */
  private void updateImages() {
    Pair<Drawable, Drawable> normalAndVariant = imgSupplier.getRandomPair();
    Drawable normal = normalAndVariant.first, variant = normalAndVariant.second;
    
    variantId = imgSupplier.random.nextInt(imgs.length);
    
    for (int i = 0; i < imgs.length; i++) {
      imgs[i].setImageDrawable(i == variantId ? variant : normal);
    }
  }
  
  /**
   * Add {@code scoreToAdd} to {@link #score} and update {@link #scoreText}. Also maybe animations
   * eventually?
   * @param scoreToAdd - the amount to add to the score.
   */
  private void addToScore(int scoreToAdd) {
    score += scoreToAdd;
    scoreText.setText(String.valueOf(score));
  }
  
  /**
   * Handle clicking an ImageSwitcher. Delegates to either {@link #nextRound()} or
   * {@link #onLose()}.
   */
  @Override
  public void onClick(View view) {
    if (!(view instanceof ImageSwitcher)) return; // only dealing with ImageSwitchers
    if (!allowImgTaps) return;
    
    ImageSwitcher img = (ImageSwitcher) view;
    
    // Is img the variant?
    if (img.getId() == variantId) {
      nextRound();
    } else {
      onLose();
    }
  }
  
  /**
   * Handle losing the game: cancel the countdown animation, wait a bit, then go to the post-game
   * activity.
   */
  private void onLose() {
    // TODO a losing animation!!!
    allowImgTaps = false;
    
    if (countdownAnim.isRunning()) {
      countdownAnim.cancel();
    }
    
    new Handler().postDelayed(new Runnable() { // no lambdas *cry*
      public void run() {
        toPostGameActivity();
      }
    }, getResources().getInteger(R.integer.after_lose_wait_time_ms));
  }
  
  private void toPostGameActivity() {
    Intent intent = new Intent(this, PostGameActivity.class);
    intent.putExtra(PostGameActivity.EXTRA_SCORE, score);
    intent.putExtra(PostGameActivity.EXTRA_DIFFICULTY, difficulty);
    startActivity(intent);
  }
  
}