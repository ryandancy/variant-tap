package ca.keal.varianttap;

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

public class GameActivity extends AppCompatActivity implements View.OnClickListener {
  
  /**
   * The array of images in imgsGrid.
   */
  private ImageSwitcher[] imgs;
  
  /**
   * The ID/index of the variant ImageSwitcher
   */
  private int variantId;
  
  /**
   * The layout containing the images in the centre of the screen. The user taps on the image they
   * think is different than the others ('variant').
   */
  private GridLayout imgsGrid;
  
  private ImageSupplier imgSupplier;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
  
    imgSupplier = new ImageSupplier(getAssets());
    
    // There are 3 difficulties: 4 (easy), 6 (normal) and 9 (hard). The numbers are the number of
    // images in imgs -- the more images the harder it is.
    // TODO: get difficulty from intent
    int difficulty = 4; // TEMPORARY
    
    // Initialize imgs
    
    imgsGrid = (GridLayout) findViewById(R.id.imgs_grid);
    
    // Calculate imgs' rows/columns
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
      params.setMargins(20, 20, 20, 20);
      
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
    
    updateImages();
  }
  
  /**
   * Handle clicking an ImageSwitcher. Delegates to either {@link #onVariantClick()} or
   * {@link #onNormalClick()}.
   */
  @Override
  public void onClick(View view) {
    if (!(view instanceof ImageSwitcher)) return; // only dealing with ImageSwitchers
    ImageSwitcher img = (ImageSwitcher) view;
    
    // Is img the variant?
    if (img.getId() == variantId) {
      onVariantClick();
    } else {
      onNormalClick();
    }
  }
  
  private void onVariantClick() {
    // TODO: add score, reset timer, etc.
    updateImages();
  }
  
  private void onNormalClick() {
    // TODO: stop clock, then delegate to an eventual onLose() method
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
  
}