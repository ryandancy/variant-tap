package ca.keal.varianttap;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
  }
  
}