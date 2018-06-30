package ca.keal.varianttap.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import ca.keal.varianttap.R;
import ca.keal.varianttap.gpgs.GPGSAction;
import ca.keal.varianttap.gpgs.GPGSCallback;
import ca.keal.varianttap.gpgs.GPGSHelperClient;
import ca.keal.varianttap.gpgs.GPGSHelperService;
import ca.keal.varianttap.gpgs.GPGSHelperServiceConnection;
import ca.keal.varianttap.util.Util;

public class PostGameActivity extends AppCompatActivity implements
    DifficultyButtonsFragment.OnFragmentInteractionListener, GPGSHelperClient, GPGSCallback {
  
  private static final String TAG = "PostGameActivity";
  
  // DO NOT CHANGE THESE CONSTANTS EVER - changes will break all local leaderboards
  
  private static final String SHARED_PREFS_LEADERBOARD_PREFIX = "local_leaderboard_";
  
  private static final String PREF_BEST_SCORE = "bestScore";
  private static final String PREF_TOTAL_SCORE = "totalScore";
  private static final String PREF_TIMES_PLAYED = "timesPlayed";
  
  public static final String EXTRA_SCORE = "SCORE";
  public static final String EXTRA_DIFFICULTY = "DIFFICULTY";
  public static final String EXTRA_NO_TAPS = "NO_TAPS";
  
  // Constants for saving the state
  // Difficulty and score aren't saved because they're gotten from the intent
  private static final String STATE_BEST_SCORE = "bestScore";
  private static final String STATE_AVERAGE_SCORE = "averageScore";
  private static final String STATE_IS_NEW_BEST_SCORE = "isNewBestScore";
  private static final String STATE_INCREMENT_ACHIEVEMENTS = "incrementXGamesAchievements";
  private static final String STATE_LOST_NO_TAPS = "lostWithNoTaps";
  
  private int difficulty;
  private int score;
  
  private int bestScore;
  private int averageScore;
  private boolean isNewBestScore;
  
  private TextView bestText;
  private TextView averageText;
  private TextView newBestScoreText;
  
  private GPGSHelperService gpgsHelper;
  private GPGSHelperServiceConnection connection;
  
  private boolean incrementXGamesAchievements;
  private boolean lostWithNoTaps;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_post_game);
    
    // Get score from intent, set it to the score TextView
    
    if (getIntent().getExtras() == null) {
      throw new IllegalArgumentException("PostGameActivity called without extras");
    }
    
    score = getIntent().getExtras().getInt(EXTRA_SCORE, -1);
    if (score == -1) { // score was not in the extras
      Log.e(TAG, "Intent did not have \"" + EXTRA_SCORE + "\" extra!");
    }
    
    TextView scoreText = findViewById(R.id.post_score_text);
    scoreText.setText(String.valueOf(score));
    
    // Get difficulty from intent, set it to the difficulty TextView
    
    difficulty = getIntent().getExtras().getInt(EXTRA_DIFFICULTY, -1);
    if (difficulty == -1) { // difficulty was not in the extras
      Log.e(TAG, "Intent did not have \"" + EXTRA_DIFFICULTY + "\" extra!");
    }
    
    lostWithNoTaps = getIntent().getExtras().getBoolean(EXTRA_NO_TAPS);
    
    String difficultyStr;
    switch (difficulty) {
      case 0:
        difficultyStr = getString(R.string.easy_mode);
        break;
      case 1:
        difficultyStr = getString(R.string.normal_mode);
        break;
      case 2:
        difficultyStr = getString(R.string.hard_mode);
        break;
      case -1: // difficulty wasn't passed; don't log a second error
        difficultyStr = "";
        break;
      default:
        Log.e(TAG, "Intent has nonsensical difficulty " + difficulty + "!");
        difficultyStr = "";
    }
    
    TextView difficultyText = findViewById(R.id.post_difficulty_text);
    difficultyText.setText(difficultyStr);
    
    bestText = findViewById(R.id.best_score_text);
    averageText = findViewById(R.id.average_score_text);
    newBestScoreText = findViewById(R.id.new_best_score_text);
    
    ImageView leaderboardButton = findViewById(R.id.leaderboard_button);
    ImageView achievementsButton = findViewById(R.id.achievements_button);
    if (leaderboardButton != null && achievementsButton != null) {
      fixTextViewButtonColoursAndSize(leaderboardButton, achievementsButton);
    }
  
    if (savedInstanceState == null) { // is this a fresh start?
      incrementXGamesAchievements = true;
      accessAndUpdateSharedPreferences();
    } else {
      restoreState(savedInstanceState);
    }
    
    connection = new GPGSHelperServiceConnection(this);
  }
  
  /** Make sure {@code imgs} have the proper colour/size. */
  private void fixTextViewButtonColoursAndSize(ImageView... imgs) {
    int textButtonColor = ContextCompat.getColor(this, R.color.postGameButtonColor);
    for (ImageView img : imgs) {
      img.getDrawable().setColorFilter(textButtonColor, PorterDuff.Mode.MULTIPLY);
    }
  }
  
  private void accessAndUpdateSharedPreferences() {
    // Get data from shared preferences for the difficulty
    // The user is allowed to mess with the shared preferences, Google Play Games doesn't use them
    
    SharedPreferences prefs = getSharedPreferences(
        SHARED_PREFS_LEADERBOARD_PREFIX + difficulty, MODE_PRIVATE);
    
    int oldBestScore = prefs.getInt(PREF_BEST_SCORE, 0);
    int totalScore = prefs.getInt(PREF_TOTAL_SCORE, 0);
    int timesPlayed = prefs.getInt(PREF_TIMES_PLAYED, 0);
    
    totalScore += score;
    timesPlayed++;
    
    isNewBestScore = score > oldBestScore;
    
    averageScore = totalScore / timesPlayed;
    bestScore = Math.max(oldBestScore, score);
    
    // Update shared preferences (and also update "New Best Score!" text's visibility/animation)
    
    SharedPreferences.Editor editor = prefs.edit();
    
    if (isNewBestScore) {
      editor.putInt(PREF_BEST_SCORE, score);
    }
    
    editor.putInt(PREF_TOTAL_SCORE, totalScore);
    editor.putInt(PREF_TIMES_PLAYED, timesPlayed);
    
    editor.apply();
    
    updateUi();
  }
  
  private void restoreState(Bundle savedInstanceState) {
    bestScore = savedInstanceState.getInt(STATE_BEST_SCORE);
    averageScore = savedInstanceState.getInt(STATE_AVERAGE_SCORE);
    isNewBestScore = savedInstanceState.getBoolean(STATE_IS_NEW_BEST_SCORE);
    incrementXGamesAchievements = savedInstanceState.getBoolean(STATE_INCREMENT_ACHIEVEMENTS);
    lostWithNoTaps = savedInstanceState.getBoolean(STATE_LOST_NO_TAPS);
    
    updateUi();
  }
  
  @Override
  protected void onStart() {
    super.onStart();
    
    // Bind to service
    Intent intent = new Intent(this, GPGSHelperService.class);
    bindService(intent, connection, BIND_AUTO_CREATE);
  }
  
  @Override
  protected void onStop() {
    super.onStop();
    unbindService(connection);
  }
  
  @Override
  public void receiveService(GPGSHelperService service) {
    gpgsHelper = service;
    gpgsHelper.addActionOnSignIn(this, GPGSAction.CallCallback);
    gpgsHelper.connectWithoutSignInFlow(this);
  }
  
  @Override
  public void gpgsCallback() {
    if (!incrementXGamesAchievements) return;
    
    // Increment all the "X games played" achievements here so that the unlocked popup shows here
    // and not on GameActivity
    gpgsHelper.incrementAchievement(R.string.achievement_id_5_games);
    gpgsHelper.incrementAchievement(R.string.achievement_id_30_games);
    gpgsHelper.incrementAchievement(R.string.achievement_id_100_games);
    
    unlockXScoreOnXDifficultyAchievements();
    
    if (lostWithNoTaps) {
      gpgsHelper.unlockAchievement(R.string.achievement_id_lose_no_taps);
    }
    
    incrementXGamesAchievements = false;
  }
  
  private void unlockXScoreOnXDifficultyAchievements() {
    switch (difficulty) {
      case 0:
        if (score >= 500) gpgsHelper.unlockAchievement(R.string.achievement_id_score_500_easy);
        if (score >= 5000) gpgsHelper.unlockAchievement(R.string.achievement_id_score_5000_easy);
        if (score >= 10000) gpgsHelper.unlockAchievement(R.string.achievement_id_score_10000_easy);
        break;
      case 1:
        if (score >= 500) gpgsHelper.unlockAchievement(R.string.achievement_id_score_500_normal);
        if (score >= 5000) gpgsHelper.unlockAchievement(R.string.achievement_id_score_5000_normal);
        if (score >= 10000) gpgsHelper.unlockAchievement(
            R.string.achievement_id_score_10000_normal);
        break;
      case 2:
        if (score >= 500) gpgsHelper.unlockAchievement(R.string.achievement_id_score_500_hard);
        if (score >= 5000) gpgsHelper.unlockAchievement(R.string.achievement_id_score_5000_hard);
        if (score >= 10000) gpgsHelper.unlockAchievement(R.string.achievement_id_score_10000_hard);
        break;
    }
  }
  
  private void updateUi() {
    bestText.setText(String.valueOf(bestScore));
    averageText.setText(String.valueOf(averageScore));
    
    if (isNewBestScore) {
      // Make the "New Best Score!" TextView visible + animate it
      Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse);
      newBestScoreText.setVisibility(View.VISIBLE);
      newBestScoreText.setAnimation(pulseAnim);
    }
  }
  
  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    
    outState.putInt(STATE_BEST_SCORE, bestScore);
    outState.putInt(STATE_AVERAGE_SCORE, averageScore);
    outState.putBoolean(STATE_IS_NEW_BEST_SCORE, isNewBestScore);
    outState.putBoolean(STATE_INCREMENT_ACHIEVEMENTS, incrementXGamesAchievements);
    outState.putBoolean(STATE_LOST_NO_TAPS, lostWithNoTaps);
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    
    if (isNewBestScore) {
      // Stop the new best score animation
      // It's a view animation so it's not pausable, and no one cares if we miss a bit of pulsing
      newBestScoreText.getAnimation().cancel();
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    if (isNewBestScore) {
      // Start/restart the pulse animation
      updateUi(); // needed to set up the pulse animation again
      newBestScoreText.getAnimation().start();
    }
  }
  
  @Override
  public void onBackPressed() {
    super.onBackPressed();
    Util.doTransition(this);
  }
  
  /**
   * Hook for after going to the game activity.
   * @param difficulty ignored
   */
  @Override
  public void afterToGameActivity(int difficulty) {
    incrementXGamesAchievements = true; // allow incrementing achievements next time
    finish(); // remove this activity from the stack
  }
  
  /** Hook from leaderboard button. */
  public void toLeaderboard(View v) {
    gpgsHelper.tryActionOrConnect(this, GPGSAction.ShowLeaderboard);
  }
  
  /** Hook from achievements button. */
  public void toAchievements(View v) {
    gpgsHelper.tryActionOrConnect(this, GPGSAction.ShowAchievements);
  }
  
}