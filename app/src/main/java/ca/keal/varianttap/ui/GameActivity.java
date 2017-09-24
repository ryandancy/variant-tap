package ca.keal.varianttap.ui;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.CycleInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.android.gms.games.Games;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.keal.varianttap.R;
import ca.keal.varianttap.gpgs.GPGSHelperService;
import ca.keal.varianttap.gpgs.GPGSHelperServiceClient;
import ca.keal.varianttap.gpgs.GPGSHelperServiceConnection;
import ca.keal.varianttap.gpgs.Score;
import ca.keal.varianttap.util.ImageSupplier;
import ca.keal.varianttap.util.MusicActivity;
import ca.keal.varianttap.util.ReverseInterpolator;
import ca.keal.varianttap.util.SFXManager;
import ca.keal.varianttap.util.Util;

public class GameActivity extends MusicActivity implements View.OnClickListener,
    GPGSHelperServiceClient {
  
  private static final String TAG = "GameActivity";
  
  // Constants for saving the state
  // Note: difficulty is not persisted because it's gotten from the intent
  private static final String STATE_ROUND = "round";
  private static final String STATE_SCORE = "score";
  private static final String STATE_SCORE_FOR_ROUND = "scoreForRound";
  private static final String STATE_VARIANT_ID = "variantId";
  private static final String STATE_ALLOW_IMG_TAPS = "allowImgTaps";
  private static final String STATE_HAS_LOST = "hasLost";
  private static final String STATE_IS_PAUSED = "isPaused";
  private static final String STATE_SWITCH_ON_RESUME = "switchOnResume";
  private static final String STATE_COUNTDOWN_CIRCLE_MAX = "countdownCircleMax";
  private static final String STATE_COUNTDOWN_CIRCLE_PROGRESS = "countdownCircleProgress";
  private static final String STATE_COUNTDOWN_CIRCLE_VALUE = "countdownCircleValue";
  private static final String STATE_COUNTDOWN_CIRCLE_RESETTING = "countdownCircleResetting";
  private static final String STATE_IMG_PAIR_ID = "imgPairId";
  
  /** The ID/index of the variant ImageSwitcher. */
  private int variantId;
  
  /** Which round we're on. */
  private int round;
  private int difficulty;
  
  /** The amount that would be added to the score if the user tapped the variant right now. */
  private int scoreForRound;
  private int score;
  
  /** Whether to process the user's taps on the images. */
  private boolean allowImgTaps;
  private boolean hasLost;
  private boolean isPaused;
  
  /** Go to PostGameActivity in onResume()? (used for exiting out of lose animation) */
  private boolean switchOnResume;
  
  private ImageButton pauseButton;
  private ConstraintLayout pauseOverlay;
  private TextView pausedText;
  private Button unpauseButton;
  private Button quitButton;
  private LinearLayout circleButtons;
  private TextView unpauseCountdownText;
  
  /** The array of images in imgsGrid. */
  private ImageSwitcher[] imgs;
  private Pair<Drawable, Drawable> currentImgPair;
  
  private TextView startingCountdownText;
  private TextView startingCountdownHint;
  
  /** The circle at the top of the screen that counts down time and also shows score. */
  private DonutProgress countdownCircle;
  private ValueAnimator countdownAnim;
  private ObjectAnimator resetCountdownAnim;
  
  private AnimatorSet loseAnim;
  
  private TextView scoreText;
  private TextView scoreLabel;
  
  private SFXManager sfx;
  
  private GPGSHelperService gpgsHelper;
  private GPGSHelperServiceConnection connection;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
    
    // Initialize everything
    
    round = 0;
    score = 0;
    allowImgTaps = false;
    isPaused = false;
    hasLost = false;
    switchOnResume = false;
    
    countdownCircle = findViewById(R.id.countdown_circle);
    scoreText = findViewById(R.id.score_text);
    scoreLabel = findViewById(R.id.score_label);
    
    startingCountdownText = findViewById(R.id.starting_countdown_text);
    startingCountdownHint = findViewById(R.id.starting_countdown_hint);
    
    pauseButton = findViewById(R.id.pause_button);
    pauseOverlay = findViewById(R.id.pause_overlay);
    pausedText = findViewById(R.id.paused_text);
    unpauseButton = findViewById(R.id.unpause_button);
    quitButton = findViewById(R.id.quit_button);
    circleButtons = findViewById(R.id.paused_circle_buttons);
    unpauseCountdownText = findViewById(R.id.unpause_countdown_text);
    
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
      Log.e(TAG, "Intent did not have \"DIFFICULTY\" extra!");
    }
    
    // Initialize the grid of images
    
    GridLayout imgsGrid = findViewById(R.id.imgs_grid);
    
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
        Log.e(TAG, "Passed unknown difficulty: " + difficulty);
        rows = 0;
        columns = 0;
    }
    
    Log.i(TAG, "Difficulty " + difficulty + ", setting grid to " + rows + "x" + columns);
    
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
      
      imgs[i].setSoundEffectsEnabled(false);
      
      imgs[i].setId(i); // for determining which is the variant; equal to the index
      imgs[i].setOnClickListener(this);
      
      imgsGrid.addView(imgs[i]);
    }
    
    if (savedInstanceState == null) { // fresh startup with no state to be restored
      startCountdownToGameStart();
    } else {
      restoreState(savedInstanceState);
    }
    
    connection = new GPGSHelperServiceConnection(this);
  }
  
  private void startCountdownToGameStart() {
    isPaused = true; // don't allow pausing with the back button
    allowImgTaps = false;
    
    // TODO vary pre-game hints (startingCountdownHint)
    countdownCircle.setText(String.valueOf(getMaxScoreForRound(round)));
    
    Util.getCountdownAnimator(
        getResources().getInteger(R.integer.start_delay_ms),
        startingCountdownText,
        new Util.CountdownEndListener() {
          public void onEnd() {
            isPaused = false;
            allowImgTaps = true;
            
            hideStartingCountdown();
            startRound();
          }
        }
    ).start();
  }
  
  private void hideStartingCountdown() {
    startingCountdownText.setVisibility(View.GONE);
    startingCountdownHint.setVisibility(View.GONE);
    pauseButton.setVisibility(View.VISIBLE);
  }
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    Log.d(TAG, "Saving instance state...");
    
    outState.putInt(STATE_ROUND, round);
    outState.putInt(STATE_SCORE, score);
    outState.putInt(STATE_SCORE_FOR_ROUND, scoreForRound);
    outState.putInt(STATE_VARIANT_ID, variantId);
    
    outState.putBoolean(STATE_ALLOW_IMG_TAPS, allowImgTaps);
    outState.putBoolean(STATE_HAS_LOST, hasLost);
    outState.putBoolean(STATE_IS_PAUSED, isPaused);
    
    outState.putBoolean(STATE_SWITCH_ON_RESUME, switchOnResume);
    
    outState.putInt(STATE_COUNTDOWN_CIRCLE_MAX, countdownCircle.getMax());
    outState.putFloat(STATE_COUNTDOWN_CIRCLE_PROGRESS, countdownCircle.getProgress());
    outState.putInt(STATE_COUNTDOWN_CIRCLE_VALUE, Integer.valueOf(countdownCircle.getText()));
    outState.putBoolean(STATE_COUNTDOWN_CIRCLE_RESETTING, resetCountdownAnim.isRunning());
    
    outState.putInt(STATE_IMG_PAIR_ID, ImageSupplier.getInstance(this).getPairId(currentImgPair));
  }
  
  private void restoreState(Bundle savedInstanceState) {
    Log.d(TAG, "Restoring state...");
    
    hideStartingCountdown();
    
    // Restore the simple state variables
    
    round = savedInstanceState.getInt(STATE_ROUND);
    score = savedInstanceState.getInt(STATE_SCORE);
    scoreForRound = savedInstanceState.getInt(STATE_SCORE_FOR_ROUND);
    variantId = savedInstanceState.getInt(STATE_VARIANT_ID);
    
    allowImgTaps = savedInstanceState.getBoolean(STATE_ALLOW_IMG_TAPS);
    hasLost = savedInstanceState.getBoolean(STATE_HAS_LOST);
    isPaused = savedInstanceState.getBoolean(STATE_IS_PAUSED);
    
    switchOnResume = savedInstanceState.getBoolean(STATE_SWITCH_ON_RESUME);
    
    // Restore view state
    
    countdownCircle.setMax(savedInstanceState.getInt(STATE_COUNTDOWN_CIRCLE_MAX));
    countdownCircle.setProgress(savedInstanceState.getFloat(STATE_COUNTDOWN_CIRCLE_PROGRESS));
    countdownCircle.setText(String.valueOf(
        savedInstanceState.getInt(STATE_COUNTDOWN_CIRCLE_VALUE)));
    
    scoreText.setText(String.valueOf(score));
    
    currentImgPair = ImageSupplier.getInstance(this).getPairById(
        savedInstanceState.getInt(STATE_IMG_PAIR_ID));
    
    for (int i = 0; i < imgs.length; i++) {
      imgs[i].setImageDrawable(i == variantId ? currentImgPair.second : currentImgPair.first);
    }
    
    // Restore animation state
    
    if (savedInstanceState.getBoolean(STATE_COUNTDOWN_CIRCLE_RESETTING)) {
      // Not restored exactly - interpolator is reset
      resetCountdownAnim.setDuration(getResetCountdownAnimationDuration(countdownCircle));
      resetCountdownAnim.start();
    } else {
      // "Continue" the countdown animation with another animator
      countdownAnim.setFloatValues(countdownCircle.getProgress(), 0);
      countdownAnim.setDuration((long) (getTimeForRoundMillis(round)
          * (countdownCircle.getProgress() / countdownCircle.getMax())));
      countdownAnim.start();
    }
    
    // Restore paused state
    // Note: since onPause() usually pauses the game, isPaused will almost always be true
    // This means that if the activity is destroyed and recreated, it'll come back paused
    
    if (isPaused) {
      Log.d(TAG, "Pausing from state");
      isPaused = false; // so that pause() actually pauses
      pause(null);
    }
  }
  
  @Override
  protected void onStart() {
    super.onStart();
    
    // Allocate audio resources
    sfx = new SFXManager(this);
    
    // Bind to service
    Intent intent = new Intent(this, GPGSHelperService.class);
    bindService(intent, connection, BIND_AUTO_CREATE);
  }
  
  @Override
  protected void onStop() {
    super.onStop();
    
    // Release audio resources
    sfx.release(this);
    sfx = null;
    
    // Unbind from service
    unbindService(connection);
  }
  
  @Override
  public void receiveService(GPGSHelperService service) {
    gpgsHelper = service;
    gpgsHelper.connectWithoutSignInFlow(this);
  }
  
  @Override
  public GPGSHelperService getService() {
    return gpgsHelper;
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    
    if (hasLost) {
      if (loseAnim != null) {
        loseAnim.cancel();
      }
      switchOnResume = true;
      return;
    }
    
    if (!isPaused) {
      pause(null); // go to the pause screen
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    if (switchOnResume) {
      toPostGameActivity(false);
    }
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
    currentImgPair = ImageSupplier.getInstance(this).getRandomPair();
    Drawable normal = currentImgPair.first, variant = currentImgPair.second;
    
    variantId = ImageSupplier.getInstance(this).random.nextInt(imgs.length);
    
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
      sfx.play(this, R.raw.success);
      nextRound();
    } else {
      onLose();
    }
  }
  
  /**
   * Handle losing the game: cancel the countdown animation, play an animation, then go to the
   * post-game activity.
   */
  private void onLose() {
    allowImgTaps = false;
    hasLost = true;
    
    if (countdownAnim.isRunning()) {
      countdownAnim.cancel();
    }
    
    sfx.play(this, R.raw.lose);
    music.stop();
    
    // Don't allow pausing during the losing animation
    pauseButton.setVisibility(View.INVISIBLE);
    isPaused = true; // don't allow pausing with the back button
    
    // Get the leaderboard ID
    int leaderboardRes = -1;
    switch (difficulty) {
      case 0:
        leaderboardRes = R.string.leaderboard_id_easy;
        break;
      case 1:
        leaderboardRes = R.string.leaderboard_id_normal;
        break;
      case 2:
        leaderboardRes = R.string.leaderboard_id_hard;
        break;
    }
    String leaderboardId = getString(leaderboardRes);
    
    // Submit or cache the score
    if (gpgsHelper.isConnected()) {
      Games.Leaderboards.submitScore(gpgsHelper.getApiClient(), leaderboardId, score);
    } else {
      gpgsHelper.getScoreCache().cache(this, new Score(leaderboardId, score));
    }
    
    // Play the lose animation, go to PostGameActivity afterwards
    loseAnim = getLoseAnimation();
    loseAnim.addListener(new Animator.AnimatorListener() {
      private boolean canceled = false;
      
      public void onAnimationCancel(Animator animator) {
        canceled = true;
      }
      
      public void onAnimationEnd(Animator animator) {
        if (canceled) return;
        toPostGameActivity(true);
      }
      
      public void onAnimationRepeat(Animator animator) {}
      public void onAnimationStart(Animator animator) {}
    });
    loseAnim.start();
  }
  
  /**
   * Makes and returns the lose animation: the normals fall to the bottom, while the variant goes to
   * the centre while growing, pulses a bit, then falls to the bottom like the normals.
   * @return The {@link AnimatorSet} representing the lose animation.
   */
  private AnimatorSet getLoseAnimation() {
    // Gee I wish I could define this in XML, but there's dynamic stuff so that can't be done!
    
    ViewGroup parent = (ViewGroup) imgs[0].getParent();
    ViewGroup root = (ViewGroup) imgs[0].getRootView();
    
    // Construct the variant animation
    
    ImageSwitcher variant = imgs[variantId];
    
    // Move into centre
    float transX = (parent.getWidth() / 2f) - (variant.getWidth() / 2f) - variant.getX();
    float transY = (parent.getHeight() / 2f) - (variant.getHeight() / 2f) - variant.getY();
    
    ObjectAnimator transXAnim = ObjectAnimator.ofFloat(variant, View.TRANSLATION_X, 0f, transX);
    ObjectAnimator transYAnim = ObjectAnimator.ofFloat(variant, View.TRANSLATION_Y, 0f, transY);
    
    // Get bigger
    float size = getResources().getDimension(R.dimen.lose_variant_anim_size);
    float scale = size / variant.getWidth();
    ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(variant, View.SCALE_X, 1f, scale);
    ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(variant, View.SCALE_Y, 1f, scale);
    
    AnimatorSet moveAnim = new AnimatorSet();
    moveAnim.playTogether(transXAnim, transYAnim, scaleXAnim, scaleYAnim);
    moveAnim.setDuration(getResources().getInteger(R.integer.lose_variant_move_time_ms));
    moveAnim.setInterpolator(new AccelerateDecelerateInterpolator());
    
    // Pulse a bit
    float pulseSize = getResources().getDimension(R.dimen.lose_variant_anim_pulse_size);
    float pulseScale = pulseSize / variant.getWidth();
    ObjectAnimator pulseXAnim = ObjectAnimator.ofFloat(variant, View.SCALE_X, scale, pulseScale);
    ObjectAnimator pulseYAnim = ObjectAnimator.ofFloat(variant, View.SCALE_Y, scale, pulseScale);
    
    AnimatorSet pulseAnim = new AnimatorSet();
    pulseAnim.playTogether(pulseXAnim, pulseYAnim);
    pulseAnim.setDuration(getResources().getInteger(R.integer.lose_variant_pulse_time_ms));
    pulseAnim.setInterpolator(new CycleInterpolator(getResources()
        .getInteger(R.integer.lose_variant_pulse_times)));
    
    // Fall
    ObjectAnimator fallAnim = ObjectAnimator.ofFloat(
        variant, View.TRANSLATION_Y, transY, root.getHeight() - variant.getY());
    fallAnim.setDuration(getResources().getInteger(R.integer.lose_variant_fall_time_ms));
    fallAnim.setInterpolator(new AccelerateInterpolator());
    
    AnimatorSet variantAnim = new AnimatorSet();
    variantAnim.playSequentially(moveAnim, pulseAnim, fallAnim);
    variantAnim.setStartDelay(getResources().getInteger(R.integer.lose_before_variant_time_ms));
    
    // Construct the normal animations
    
    AnimatorSet normalAnim = new AnimatorSet();
    List<Animator> normalAnims = new ArrayList<>();
    
    for (int i = 0; i < imgs.length; i++) {
      if (i == variantId) continue;
      ImageSwitcher normal = imgs[i];
      
      ObjectAnimator normalFallAnim = ObjectAnimator.ofFloat(
          normal, View.TRANSLATION_Y, 0f, root.getHeight() - normal.getY());
      normalFallAnim.setInterpolator(new AccelerateInterpolator());
      normalFallAnim.setDuration(getResources().getInteger(R.integer.lose_normal_fall_time_ms));
      normalAnims.add(normalFallAnim);
    }
    
    normalAnim.playTogether(normalAnims);
    
    // Play them together
    AnimatorSet loseAnim = new AnimatorSet();
    loseAnim.playTogether(normalAnim, variantAnim);
    
    return loseAnim;
  }
  
  private void toPostGameActivity(boolean transition) {
    Intent intent = new Intent(this, PostGameActivity.class);
    intent.putExtra(PostGameActivity.EXTRA_SCORE, score);
    intent.putExtra(PostGameActivity.EXTRA_DIFFICULTY, difficulty);
    if (transition) {
      startActivity(intent, Util.getActivityTransition(this));
    } else {
      startActivity(intent);
    }
    finish();
  }
  
  /**
   * Pause the game; i.e., pause the animations and bring up the pause overlay.
   */
  public void pause(View v) {
    if (isPaused) return; // can't pause if it's already paused
    
    isPaused = true;
    allowImgTaps = false; // no tapping on the images when it's paused
    
    if (countdownAnim.isRunning()) countdownAnim.pause();
    if (resetCountdownAnim.isRunning()) resetCountdownAnim.pause();
    // Note: not pausing ImageSwitcher animations - TODO is doing so possible? worth it?
    
    pauseOverlay.setVisibility(View.VISIBLE);
    
    // Change colours - TODO abstract the colours into themes for less repetition???
    countdownCircle.setTextColor(
        ResourcesCompat.getColor(getResources(), R.color.countdownCirclePausedText, null));
    countdownCircle.setFinishedStrokeColor(
        ResourcesCompat.getColor(getResources(), R.color.countdownCirclePausedFinished, null));
    countdownCircle.setUnfinishedStrokeColor(
        ResourcesCompat.getColor(getResources(), R.color.countdownCirclePausedUnfinished, null));
    
    int gameMainColor = ResourcesCompat.getColor(getResources(), R.color.gamePausedMain, null);
    scoreText.setTextColor(gameMainColor);
    scoreLabel.setTextColor(gameMainColor);
    
    // Bring countdownCircle, scoreText, scoreLabel on top of pause overlay
    countdownCircle.bringToFront();
    scoreText.bringToFront();
    scoreLabel.bringToFront();
  }
  
  @Override
  public void onBackPressed() {
    // Pause on back button press
    pause(null);
  }
  
  /**
   * Start the process of unpausing; i.e., start the 3..2..1 animation counting down to unpausing.
   */
  public void startUnpause(View v) {
    if (!isPaused) return;
    
    // Replace the paused text + buttons with the unpause countdown text
    pausedText.setVisibility(View.INVISIBLE);
    unpauseButton.setVisibility(View.INVISIBLE);
    quitButton.setVisibility(View.INVISIBLE);
    circleButtons.setVisibility(View.INVISIBLE);
    unpauseCountdownText.setVisibility(View.VISIBLE);
    
    // Make and play the unpause animation
    Util.getCountdownAnimator(
        getResources().getInteger(R.integer.unpause_delay_ms),
        unpauseCountdownText,
        new Util.CountdownEndListener() {
          public void onEnd() {
            unpause();
          }
        }
    ).start();
  }
  
  /**
   * Finish unpausing; i.e. hide the overlay, reset the layout, and unpause the animations.
   */
  public void unpause() {
    pauseOverlay.setVisibility(View.INVISIBLE);
    
    // Reset the overlay - undo the changes in startUnpause()
    unpauseCountdownText.setVisibility(View.INVISIBLE);
    pausedText.setVisibility(View.VISIBLE);
    unpauseButton.setVisibility(View.VISIBLE);
    quitButton.setVisibility(View.VISIBLE);
    circleButtons.setVisibility(View.VISIBLE);
    
    // Reset the colours - undo the changes in pause()
    
    countdownCircle.setTextColor(
        ResourcesCompat.getColor(getResources(), R.color.countdownCircleText, null));
    countdownCircle.setFinishedStrokeColor(
        ResourcesCompat.getColor(getResources(), R.color.countdownCircleFinished, null));
    countdownCircle.setUnfinishedStrokeColor(
        ResourcesCompat.getColor(getResources(), R.color.countdownCircleUnfinished, null));
    
    int gameMainColor = ResourcesCompat.getColor(getResources(), R.color.gameMain, null);
    scoreText.setTextColor(gameMainColor);
    scoreLabel.setTextColor(gameMainColor);
    
    // Unpause the animations, make it playable again
    
    if (countdownAnim.isPaused()) countdownAnim.resume();
    if (resetCountdownAnim.isPaused()) resetCountdownAnim.resume();
    
    isPaused = false;
    allowImgTaps = true;
  }
  
  /**
   * Quit the game and return to the main menu. This consists of sending a confirmation dialog, then
   * returning to MainActivity if an affirmative response is received.
   */
  public void quit(View v) {
    // Send a confirmation dialog
    new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme_Holo_Dialog))
    .setMessage(R.string.quit_message)
    .setPositiveButton(R.string.quit_action, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        // Wipe this activity off the stack, which will return the user to MainActivity
        hasLost = true;
        finish();
        Util.doTransition(GameActivity.this);
      }
    })
    .setNegativeButton(android.R.string.cancel, null)
    .show();
  }
  
  @Override
  protected AssetFileDescriptor getBackgroundMusicFD() throws IOException {
    return getAssets().openFd("music/game.mp3");
  }
  
}