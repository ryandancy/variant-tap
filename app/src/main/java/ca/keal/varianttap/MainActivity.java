package ca.keal.varianttap;

import android.content.Intent;
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
  
  private Button playButton;
  private LinearLayout difficultyBtnsLayout;
  
  /** The list of buttons which when selected will start the game at a certain difficulty. */
  private List<Button> difficultyBtns = new ArrayList<>();
  
  /**
   * The list of the animations that make the difficulty buttons slide up from the bottom. This is
   * a list because the "fromYDelta" and "duration" attributes will vary depending on the button.
   */
  private List<Animation> slideUpAnims = new ArrayList<>();
  
  private boolean difficultyBtnsShowing = false;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  
    difficultyBtnsLayout = (LinearLayout) findViewById(R.id.difficulty_button_layout);
  
    playButton = (Button) findViewById(R.id.play_button);
  
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
          anim.setFillAfter(true);
          
          slideUpAnims.add(anim);
        }
      }
    });
  }
  
  /** @return Whether the sliding up animations are playing, using the first (longest) animation. */
  private boolean areSlidingUpAnimationsPlaying() {
    return !slideUpAnims.get(0).hasStarted() || slideUpAnims.get(0).hasEnded();
  }
  
  public void toggleDifficultyButtons(View v) {
    if (areSlidingUpAnimationsPlaying()) {
      if (difficultyBtnsShowing) {
        hideDifficultyButtons();
      } else {
        showDifficultyButtons();
      }
    }
  }
  
  private void showDifficultyButtons() {
    difficultyBtnsShowing = true;
    
    difficultyBtnsLayout.setVisibility(View.VISIBLE);
    
    for (int i = 0; i < difficultyBtns.size(); i++) {
      Button button = difficultyBtns.get(i);
      Animation anim = slideUpAnims.get(i);
      
      // Undo reversing from hideDifficultyButtons()
      if (anim.getInterpolator() instanceof ReverseInterpolator) {
        anim.setInterpolator(((ReverseInterpolator) anim.getInterpolator()).getDelegate());
      }
      
      button.startAnimation(anim);
    }
    
    playButton.setText(getString(R.string.close_difficulty_btns));
  }
  
  private void hideDifficultyButtons() {
    difficultyBtnsShowing = false;
    
    for (int i = 0; i < difficultyBtns.size(); i++) {
      Button button = difficultyBtns.get(i);
      Animation anim = slideUpAnims.get(i);
      
      // Reverse the animation
      anim.setInterpolator(new ReverseInterpolator(anim.getInterpolator()));
      
      button.startAnimation(anim);
    }
    
    // Make the difficulty buttons invisible when the animations finish
    // slideUpAnims[0] is the longest so we use it to detect the animations' finishing
    slideUpAnims.get(0).setAnimationListener(new Animation.AnimationListener() {
      public void onAnimationStart(Animation animation) {}
      public void onAnimationRepeat(Animation animation) {}
      
      public void onAnimationEnd(Animation animation) {
        difficultyBtnsLayout.setVisibility(View.INVISIBLE);
        slideUpAnims.get(0).setAnimationListener(null);
      }
    });
    
    playButton.setText(getString(R.string.play));
  }
  
  /** Go to {@link GameActivity}, setting the {@code "DIFFICULTY"} extra with the parameter. */
  private void toGameActivity(int difficulty) {
    Intent intent = new Intent(this, GameActivity.class);
    intent.putExtra("DIFFICULTY", difficulty);
    startActivity(intent);
    hideDifficultyButtons(); // don't show them when back button is pressed post-game
  }
  
  public void onEasyClick(View v) {
    toGameActivity(4);
  }
  
  public void onNormalClick(View v) {
    toGameActivity(6);
  }
  
  public void onHardClick(View v) {
    toGameActivity(9);
  }
  
}