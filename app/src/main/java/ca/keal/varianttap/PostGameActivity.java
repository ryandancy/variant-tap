package ca.keal.varianttap;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class PostGameActivity extends AppCompatActivity {
  
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
    
    // Get data from shared preferences
    // The user is allowed to mess with the shared preferences, Google Play Games won't use them
    // TODO: Google Play Games integration
    
    SharedPreferences prefs = getPreferences(MODE_PRIVATE); // hopefully only one prefs file needed
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
   * Play again - i.e. go to GameActivity.
   * @param v - ignored
   */
  public void playAgain(View v) {
    Intent intent = new Intent(this, GameActivity.class);
    // TODO set a difficulty here
    startActivity(intent);
    finish(); // remove this activity from the stack so the user can't navigate back to it
  }
  
}