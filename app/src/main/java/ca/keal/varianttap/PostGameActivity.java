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
  
  // DO NOT CHANGE EVER - a change will break all local leaderboards
  private static final String SHARED_PREFS_LEADERBOARD_PREFIX = "local_leaderboard_";
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_post_game);
    
    // Get score from intent, set it to the score TextView
    
    int score = getIntent().getExtras().getInt("SCORE", -1);
    if (score == -1) { // score was not in the extras
      Log.e(getClass().getName(), "Intent did not have \"SCORE\" extra!");
    }
    
    TextView scoreText = (TextView) findViewById(R.id.post_score_text);
    scoreText.setText(String.valueOf(score));
    
    // Get difficulty from intent, set it to the difficulty TextView
    
    int difficulty = getIntent().getExtras().getInt("DIFFICULTY", -1);
    if (difficulty == -1) { // difficulty was not in the extras
      Log.e(getClass().getName(), "Intent did not have \"DIFFICULTY\" extra!");
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
        Log.e(getClass().getName(), "Intent has nonsensical \"DIFFICULTY\" " + difficulty + "!");
        difficultyStr = "";
    }
    
    TextView difficultyText = (TextView) findViewById(R.id.post_difficulty_text);
    difficultyText.setText(difficultyStr);
  
    // Get data from shared preferences for the difficulty
    // The user is allowed to mess with the shared preferences, Google Play Games won't use them
    // TODO: Google Play Games integration
    
    SharedPreferences prefs = getSharedPreferences(
        SHARED_PREFS_LEADERBOARD_PREFIX + difficulty, MODE_PRIVATE);
    
    int bestScore = prefs.getInt("bestScore", 0); // TODO extract into constants? "SCORE" too?
    int totalScore = prefs.getInt("totalScore", 0);
    int timesPlayed = prefs.getInt("timesPlayed", 0);
    
    totalScore += score;
    timesPlayed++;
    
    // Update UI with shared preferences data
    
    int average = totalScore / timesPlayed;
    TextView averageText = (TextView) findViewById(R.id.average_score_text);
    averageText.setText(String.valueOf(average));
    
    int newBest = Math.max(bestScore, score);
    TextView bestText = (TextView) findViewById(R.id.best_score_text);
    bestText.setText(String.valueOf(newBest));
  
    // Update shared preferences (and also update "New Best Score!" text's visibility)
  
    SharedPreferences.Editor editor = prefs.edit();
    
    if (score > bestScore) {
      // Make the "New Best Score!" TextView visible + animate it
      TextView newBestScoreText = (TextView) findViewById(R.id.new_best_score_text);
      Animation pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse);
      newBestScoreText.setVisibility(View.VISIBLE);
      newBestScoreText.startAnimation(pulseAnim);
      
      editor.putInt("bestScore", score);
    }
    
    editor.putInt("totalScore", totalScore);
    editor.putInt("timesPlayed", timesPlayed);
    
    editor.apply();
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