package ca.keal.varianttap;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.widget.GridLayout;
import android.widget.ImageView;

public class GameActivity extends AppCompatActivity {
  
  /**
   * The array of ImageViews in imgsGrid.
   */
  private ImageView[] imgs;
  
  /**
   * The layout containing the ImageViews in the centre of the screen. The user taps on the
   * ImageView they think is different than the others ('variant').
   */
  private GridLayout imgsGrid;
  
  private ImageSupplier imgSupplier;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
  
    imgSupplier = new ImageSupplier(getAssets());
    
    // There are 3 difficulties: 4 (easy), 6 (normal) and 9 (hard). The numbers are the number of
    // ImageViews in imgs -- the more ImageViews the harder it is.
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
    
    imgs = new ImageView[difficulty];
    
    for (int i = 0; i < difficulty; i++) {
      imgs[i] = new ImageView(getBaseContext());
      imgsGrid.addView(imgs[i]);
    }
  }
  
  /**
   * Update the ImageViews with new images. One of the ImageViews has a variant image, the others
   * have the corresponding normal image.
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