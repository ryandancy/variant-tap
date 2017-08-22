package ca.keal.varianttap;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class PostGameActivity extends AppCompatActivity
    implements DifficultyButtonsFragment.OnFragmentInteractionListener {
  
  // DO NOT CHANGE THESE CONSTANTS EVER - changes will break all local leaderboards
  
  private static final String SHARED_PREFS_LEADERBOARD_PREFIX = "local_leaderboard_";
  
  private static final String PREF_BEST_SCORE = "bestScore";
  private static final String PREF_TOTAL_SCORE = "totalScore";
  private static final String PREF_TIMES_PLAYED = "timesPlayed";
  
  public static final String EXTRA_SCORE = "SCORE";
  public static final String EXTRA_DIFFICULTY = "DIFFICULTY";
  
  // Constants for saving the state
  // Difficulty and score aren't saved because they're gotten from the intent
  private static final String STATE_BEST_SCORE = "bestScore";
  private static final String STATE_AVERAGE_SCORE = "averageScore";
  private static final String STATE_IS_NEW_BEST_SCORE = "isNewBestScore";
  
  private int difficulty;
  private int score;
  
  private int bestScore;
  private int averageScore;
  private boolean isNewBestScore;
  
  private TextView bestText;
  private TextView averageText;
  private TextView newBestScoreText;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_post_game);
    
    // Get score from intent, set it to the score TextView
    
    score = getIntent().getExtras().getInt(EXTRA_SCORE, -1);
    if (score == -1) { // score was not in the extras
      Log.e(getClass().getName(), "Intent did not have \"" + EXTRA_SCORE + "\" extra!");
    }
    
    TextView scoreText = findViewById(R.id.post_score_text);
    scoreText.setText(String.valueOf(score));
    
    // Get difficulty from intent, set it to the difficulty TextView
    
    difficulty = getIntent().getExtras().getInt(EXTRA_DIFFICULTY, -1);
    if (difficulty == -1) { // difficulty was not in the extras
      Log.e(getClass().getName(), "Intent did not have \"" + EXTRA_DIFFICULTY + "\" extra!");
    }
    
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
        Log.e(getClass().getName(), "Intent has nonsensical difficulty " + difficulty + "!");
        difficultyStr = "";
    }
    
    TextView difficultyText = findViewById(R.id.post_difficulty_text);
    difficultyText.setText(difficultyStr);
    
    bestText = findViewById(R.id.best_score_text);
    averageText = findViewById(R.id.average_score_text);
    newBestScoreText = findViewById(R.id.new_best_score_text);
  
    if (savedInstanceState == null) { // is this a fresh start?
      accessAndUpdateSharedPreferences();
    } else {
      restoreState(savedInstanceState);
    }
  }
  
  private void accessAndUpdateSharedPreferences() {
    // Get data from shared preferences for the difficulty
    // The user is allowed to mess with the shared preferences, Google Play Games won't use them
    // TODO: Google Play Games integration
    
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
    
    updateUi();
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
  }
  
  @Override
  protected void onPause() {
    super.onPause();
    
    if (isNewBestScore) {
      // Stop the new best score animation and set it up to start again
      // It's a view animation so it's not pausable, and no one cares if we miss a bit of pulsing
      newBestScoreText.getAnimation().cancel();
      newBestScoreText.getAnimation().reset();
    }
  }
  
  @Override
  protected void onResume() {
    super.onResume();
    
    if (isNewBestScore) {
      // Start/restart the pulse animation
      newBestScoreText.getAnimation().start();
    }
  }
  
  @Override
  public void onBackPressed() {
    super.onBackPressed();
    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
  }
  
  /**
   * Hook for after going to the game activity.
   * @param difficulty ignored
   */
  @Override
  public void afterToGameActivity(int difficulty) {
    // Remove this activity from the stack
    finish();
  }
  
}