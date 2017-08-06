package ca.keal.varianttap;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.GridLayout;

public class GameActivity extends AppCompatActivity {
  
  /**
   * The layout containing the ImageViews in the centre of the screen. The user taps on the
   * ImageView they think is different than the others ('variant').
   */
  private GridLayout imgs;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_game);
    
    // There are 3 difficulties: 4 (easy), 6 (normal) and 9 (hard). The numbers are the number of
    // ImageViews in imgs -- the more ImageViews the harder it is.
    // TODO: get difficulty from intent
    int difficulty = 4; // TEMPORARY
    
    // Initialize imgs
    
    imgs = (GridLayout) findViewById(R.id.imgs);
    
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
    
    imgs.setRowCount(rows);
    imgs.setColumnCount(columns);
  }
  
}