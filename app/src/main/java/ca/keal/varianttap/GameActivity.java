package ca.keal.varianttap;

import android.animation.AnimatorInflater;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import android.widget.ViewSwitcher;

import com.github.lzyzsd.circleprogress.DonutProgress;

import static ca.keal.varianttap.R.animator.countdown;

public class GameActivity extends AppCompatActivity implements View.OnClickListener {
  
  /**  The ID/index of the variant ImageSwitcher. */
  private int variantId;
  
  /** Which round we're on. */
  private int round;
  
  /** The array of images in imgsGrid. */
  private ImageSwitcher[] imgs;
  
  /** The circle at the top of the screen that counts down time and also shows score. */
  private DonutProgress countdownCircle;
  private ValueAnimator countdownAnim;
  
  private ImageSupplier imgSupplier;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
    
    // Initialize everything
  
    imgSupplier = new ImageSupplier(getAssets());
    round = 0;
    
    countdownCircle = (DonutProgress) findViewById(R.id.countdown_circle);
    
    countdownAnim = (ValueAnimator) AnimatorInflater.loadAnimator(this, countdown);
    countdownAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      public void onAnimationUpdate(ValueAnimator animation) {
        countdownCircle.setProgress((Float) animation.getAnimatedValue());
        countdownCircle.setText(String.valueOf(((Float) animation.getAnimatedValue()).intValue()));
      }
    });
    
    // There are 3 difficulties: 4 (easy), 6 (normal) and 9 (hard). The numbers are the number of
    // images in imgs -- the more images the harder it is.
    // TODO: get difficulty from intent
    int difficulty = 4; // TEMPORARY
    
    // Initialize the grid of images
    
    GridLayout imgsGrid = (GridLayout) findViewById(R.id.imgs_grid);
    
    // Calculate rows/columns
    int rows, columns;
    switch (difficulty) {
      case 4:
        rows = 2;
        columns = 2;
        break;
      case 6:
        rows = 2;
        columns = 3;
        break;
      case 9:
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
    
    imgs = new ImageSwitcher[difficulty];
    
    // Create the animations outside the loop so as not to load them multiple times
    // The out animation is just the in animation reversed, so we use ReverseInterpolator.
    Animation in = AnimationUtils.loadAnimation(this, R.anim.grow_in);
    Animation out = AnimationUtils.loadAnimation(this, R.anim.grow_in);
    out.setInterpolator(new ReverseInterpolator(out.getInterpolator()));
    
    for (int i = 0; i < difficulty; i++) {
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
    
    nextRound();
  }
  
  /**
   * Go to the next round; this involves updating the images, resetting animations, and so on.
   */
  private void nextRound() {
    int maxScore = getMaxScoreForRound(round);
    countdownCircle.setMax(maxScore);
    countdownCircle.setProgress(maxScore);
    
    countdownAnim.setFloatValues(maxScore, 0);
    countdownAnim.setDuration(getTimeForRoundMillis(round));
    countdownAnim.start();
    
    round++;
    
    updateImages();
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
   * Handle clicking an ImageSwitcher. Delegates to either {@link #nextRound()} or
   * {@link #onLose()}.
   */
  @Override
  public void onClick(View view) {
    if (!(view instanceof ImageSwitcher)) return; // only dealing with ImageSwitchers
    ImageSwitcher img = (ImageSwitcher) view;
    
    // Is img the variant?
    if (img.getId() == variantId) {
      nextRound();
    } else {
      onLose();
    }
  }
  
  private void onLose() {
    // TODO: stop clock, go to post-game activity, etc.
  }
  
}