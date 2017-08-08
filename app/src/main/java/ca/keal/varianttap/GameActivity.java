package ca.keal.varianttap;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

public class GameActivity extends AppCompatActivity {
  
  /**
   * The array of images in imgsGrid.
   */
  private ImageSwitcher[] imgs;
  
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
    
    for (int i = 0; i < difficulty; i++) {
      // Format the ImageSwitcher nicely
      
      GridLayout.Spec rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
      GridLayout.Spec columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
      
      GridLayout.LayoutParams params = new GridLayout.LayoutParams(rowSpec, columnSpec);
      params.setGravity(Gravity.FILL);
      params.setMargins(20, 20, 20, 20);
      
      imgs[i] = new ImageSwitcher(this);
      imgs[i].setLayoutParams(params);
      
      imgs[i].setFactory(new ViewSwitcher.ViewFactory() { // no lambda expressions *sigh*
        public View makeView() {
          ImageView imgView = new ImageView(getApplicationContext());
          imgView.setAdjustViewBounds(true);
          imgView.setScaleType(ImageView.ScaleType.FIT_XY);
          return imgView;
        }
      });
      
      imgsGrid.addView(imgs[i]);
    }
    
    updateImages();
  }
  
  /**
   * Update the ImagesSwitchers with new images. One of the ImageSwitchers has a variant image, the
   * others have the corresponding normal image.
   */
  private void updateImages() {
    Pair<Drawable, Drawable> normalAndVariant = imgSupplier.getRandomPair();
    Drawable normal = normalAndVariant.first, variant = normalAndVariant.second;
    
    int variantIdx = imgSupplier.random.nextInt(imgs.length);
    
    for (int i = 0; i < imgs.length; i++) {
      imgs[i].setImageDrawable(i == variantIdx ? variant : normal);
    }
  }
  
}