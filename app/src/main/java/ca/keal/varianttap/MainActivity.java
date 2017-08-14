package ca.keal.varianttap;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.view.ViewTreeObserver.OnGlobalLayoutListener;

public class MainActivity extends AppCompatActivity {
  
  private LinearLayout difficultyBtnsLayout;
  
  /** The list of buttons which when selected will start the game at a certain difficulty. */
  private List<Button> difficultyBtns = new ArrayList<>();
  
  /**
   * The list of the animations that make the difficulty buttons slide up from the bottom. This is
   * a list because the "fromYDelta" and "duration" attributes will vary depending on the button.
   */
  private List<Animation> slideUpAnims = new ArrayList<>();
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  
    difficultyBtnsLayout = (LinearLayout) findViewById(R.id.difficulty_button_layout);
  
    final Button playButton = (Button) findViewById(R.id.play_button);
  
    // Initialize difficultyBtns
    Collections.addAll(difficultyBtns,
        (Button) findViewById(R.id.easy_button),
        (Button) findViewById(R.id.normal_button),
        (Button) findViewById(R.id.hard_button)
    );
  
    RelativeLayout layout = (RelativeLayout) findViewById(R.id.main_layout);
    layout.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
      public void onGlobalLayout() {
        // Initialize slideUpAnims with properly initialized animations.
        // This is in an OnGlobalLayoutListener so that the calls to getY() happen after the layout
        // is actually laid out (if this was in the main method, getY() would return 0).
        
        int slideUpSpeed = getResources().getInteger(R.integer.slide_up_speed);
        
        for (int i = 0; i < difficultyBtns.size(); i++) {
          Button button = difficultyBtns.get(i);
          
          float fromYDelta = Math.abs(button.getY() - (playButton.getY() - playButton.getHeight()));
          float duration = Util.pxToDp(MainActivity.this, fromYDelta) / 100 * slideUpSpeed;
          
          TranslateAnimation anim = new TranslateAnimation(0, 0, fromYDelta, 0);
          anim.setInterpolator(new DecelerateInterpolator());
          anim.setDuration((long) duration);
          
          slideUpAnims.add(anim);
        }
      }
    });
  }
    
  
  public void showDifficultyButtons(View v) {
    difficultyBtnsLayout.setVisibility(View.VISIBLE);
    
    for (int i = 0; i < difficultyBtns.size(); i++) {
      Button button = difficultyBtns.get(i);
      Animation anim = slideUpAnims.get(i);
      button.startAnimation(anim);
    }
  }
  
}